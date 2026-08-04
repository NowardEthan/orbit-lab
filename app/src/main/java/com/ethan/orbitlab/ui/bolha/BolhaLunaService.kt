package com.ethan.orbitlab.ui.bolha

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.PathInterpolator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ethan.orbitlab.MainActivity
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.billing.UsageRepository
import com.ethan.orbitlab.data.crash.CrashReporting
import com.ethan.orbitlab.data.voice.VoiceClip
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Bolha da Luna — FAB no overlay; painel em [BolhaPainelActivity].
 * B1 gesto · B2 handoff · B3 sinais/peek · B5 quick reply.
 */
class BolhaLunaService :
    Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var view: ComposeView? = null
    private var snapAnimator: ValueAnimator? = null
    private var peekJob: Job? = null

    private var appEmPrimeiroPlano = false
    private var jaAvisouZonaDismiss = false
    private var peekAtivo = false
    private var xAntesDoPeek = 0
    /** Arraste ativo — move o WRAP via params, sem virar tela cheia no meio do gesto. */
    private var arrastando = false

    private val ui = MutableStateFlow(
        BolhaUiState(
            offsetX = 0,
            offsetY = PrefsRepository.bolhaY,
            telaCheia = false,
            sobreDismiss = false,
            quickAberto = false,
            enterNonce = 0,
        ),
    )

    private val appLifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                appEmPrimeiroPlano = true
                cancelarPeek()
                fecharQuick()
                atualizarVisibilidadeFab()
            }
            Lifecycle.Event.ON_STOP -> {
                appEmPrimeiroPlano = false
                ui.update { it.copy(enterNonce = it.enterNonce + 1) }
                atualizarVisibilidadeFab()
                agendarPeek()
            }
            else -> Unit
        }
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        tipoOverlay(),
        FLAGS_BOLHA,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        instancia = this
        restaurarPosicaoInicial()
        appEmPrimeiroPlano = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        montarBolha()
        observarSinais()
        _rodando.value = true
        atualizarVisibilidadeFab()
        if (!appEmPrimeiroPlano) agendarPeek()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        irParaPrimeiroPlano()
        return START_STICKY
    }

    private fun observarSinais() {
        val conversaId = ChatRepository.conversaPrincipal()
        scope.launch {
            combine(
                ChatRepository.observarConversa(conversaId),
                ChatRepository.streamDaConversa(conversaId),
                UsageRepository.bloqueado,
                UsageRepository.usage,
            ) { conv, stream, cotaBloq, usage ->
                Triple(conv, stream, cotaBloq to usage)
            }.collect { (conv, stream, cotaPair) ->
                val (cotaBloq, usage) = cotaPair
                val pensando = stream !is LunaStreamEstado.Idle ||
                    ChatRepository.turnoEmAndamento(conversaId)
                BolhaSinal.setPensando(pensando)

                val semSaldo = !usage.loading && !usage.ilimitado && !usage.temSaldoParaChat
                BolhaSinal.setAlertaCota(cotaBloq || semSaldo)

                val lastLuna = conv?.mensagens?.lastOrNull { it.isLuna }
                if (lastLuna != null &&
                    lastLuna.id != PrefsRepository.bolhaLastLunaMsgId &&
                    !_painelAberto.value &&
                    !appEmPrimeiroPlano
                ) {
                    BolhaSinal.setBadge(true)
                }
            }
        }
    }

    private fun restaurarPosicaoInicial() {
        val y = clampY(PrefsRepository.bolhaY, estimativaFabPx())
        val x = if (PrefsRepository.bolhaLadoEsquerdo) {
            0
        } else {
            (recursos().widthPixels - estimativaFabPx()).coerceAtLeast(0)
        }
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.x = x
        params.y = y
        ui.value = BolhaUiState(
            offsetX = x,
            offsetY = y,
            telaCheia = false,
            sobreDismiss = false,
            quickAberto = false,
            enterNonce = 0,
        )
    }

    private fun montarBolha() {
        val compose = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BolhaLunaService)
            setViewTreeViewModelStoreOwner(this@BolhaLunaService)
            setViewTreeSavedStateRegistryOwner(this@BolhaLunaService)
            setContent {
                val state by ui.collectAsState()
                val vibracao by PrefsRepository.vibracao.collectAsState()
                val badge by BolhaSinal.badge.collectAsState()
                val pensando by BolhaSinal.pensando.collectAsState()
                val alertaCota by BolhaSinal.alertaCota.collectAsState()
                BolhaOverlay(
                    telaCheia = state.telaCheia,
                    offsetX = state.offsetX,
                    offsetY = state.offsetY,
                    sobreDismiss = state.sobreDismiss,
                    quickAberto = state.quickAberto,
                    badge = badge,
                    pensando = pensando,
                    alertaCota = alertaCota,
                    enterNonce = state.enterNonce,
                    hapticsLigados = vibracao,
                    onDragStart = { iniciarArraste() },
                    onArrastar = { dx, dy -> moverArraste(dx, dy) },
                    onSoltar = { soltarArraste() },
                    onTocar = { abrirQuickOuPainel() },
                    onAbrirPainel = { rascunho -> expandirPainel(rascunho) },
                    onFecharQuick = { fecharQuick() },
                    onEnviarQuick = { texto ->
                        BolhaEnvio.enviarTexto(applicationContext as Application, texto)
                        fecharQuick()
                        CrashReporting.breadcrumb("bolha_quick_send")
                    },
                    onEnviarAudioQuick = { clip ->
                        BolhaEnvio.enviarAudio(applicationContext as Application, clip)
                        fecharQuick()
                        CrashReporting.breadcrumb("bolha_quick_send")
                    },
                )
            }
        }
        view = compose
        runCatching { windowManager.addView(compose, params) }
        compose.post { alinharLadoAposLayout() }
    }

    private fun abrirQuickOuPainel() {
        cancelarPeek()
        BolhaSinal.limparBadge()
        marcarMsgsVistas()
        if (ui.value.quickAberto) {
            expandirPainel()
        } else {
            // Quick precisa de foco pro teclado.
            params.flags = FLAGS_QUICK
            params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            ui.update { it.copy(quickAberto = true) }
            atualizar()
            CrashReporting.breadcrumb("bolha_quick_open")
        }
    }

    private fun fecharQuick() {
        if (!ui.value.quickAberto) return
        ui.update { it.copy(quickAberto = false) }
        params.flags = FLAGS_BOLHA
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
        atualizar()
        agendarPeek()
    }

    private fun alinharLadoAposLayout() {
        if (ui.value.telaCheia || peekAtivo) return
        val w = view?.width?.takeIf { it > 0 } ?: return
        val alvo = if (PrefsRepository.bolhaLadoEsquerdo) 0 else recursos().widthPixels - w
        if (params.x != alvo) {
            params.x = alvo
            ui.update { it.copy(offsetX = alvo) }
            atualizar()
        }
    }

    private fun iniciarArraste() {
        cancelarPeek()
        fecharQuick()
        snapAnimator?.cancel()
        snapAnimator = null
        jaAvisouZonaDismiss = false
        arrastando = true
        // Mantém WRAP_CONTENT e move params.x/y. Virar MATCH_PARENT no meio do gesto
        // cancelava o pointer no Compose — FAB ficava “preso”.
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = FLAGS_BOLHA
        ui.update {
            it.copy(
                offsetX = params.x,
                offsetY = params.y,
                telaCheia = false,
                sobreDismiss = false,
                quickAberto = false,
            )
        }
    }

    private fun moverArraste(dx: Int, dy: Int) {
        if (!arrastando) return
        val fab = tamanhoFabPx()
        val nx = (params.x + dx).coerceIn(0, (recursos().widthPixels - fab).coerceAtLeast(0))
        val ny = clampY(params.y + dy, fab)
        params.x = nx
        params.y = ny
        val zona = estaNaZonaDismiss(ny, fab)
        if (zona && !jaAvisouZonaDismiss) {
            jaAvisouZonaDismiss = true
            tickHaptic()
        }
        if (!zona) jaAvisouZonaDismiss = false
        ui.update { it.copy(offsetX = nx, offsetY = ny, sobreDismiss = zona) }
        atualizar()
    }

    private fun soltarArraste() {
        if (!arrastando) return
        arrastando = false
        val st = ui.value
        if (st.sobreDismiss) {
            tickHaptic(forte = true)
            CrashReporting.breadcrumb("bolha_dismiss")
            pararSozinho()
            return
        }
        ui.update { it.copy(sobreDismiss = false, telaCheia = false) }
        view?.post {
            grudarNaBordaAnimado(params.x, params.y)
            agendarPeek()
        }
    }

    private fun grudarNaBordaAnimado(fromX: Int, fromY: Int) {
        snapAnimator?.cancel()
        val w = view?.width?.takeIf { it > 0 } ?: tamanhoFabPx()
        val screenW = recursos().widthPixels
        val targetX = if (fromX + w / 2 < screenW / 2) 0 else screenW - w
        val targetY = clampY(fromY, w)
        params.y = targetY

        fun fim() {
            params.x = targetX
            persistirPosicao(targetX, targetY)
            atualizar()
            tickHaptic()
            CrashReporting.breadcrumb("bolha_snap")
            snapAnimator = null
        }

        if (fromX == targetX) {
            fim()
            return
        }

        snapAnimator = ValueAnimator.ofInt(fromX, targetX).apply {
            duration = 240
            interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
            addUpdateListener { anim ->
                params.x = anim.animatedValue as Int
                ui.update { it.copy(offsetX = params.x, offsetY = params.y) }
                atualizar()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = fim()
                override fun onAnimationCancel(animation: Animator) {
                    snapAnimator = null
                }
            })
            start()
        }
    }

    private fun agendarPeek() {
        peekJob?.cancel()
        if (appEmPrimeiroPlano || _painelAberto.value || ui.value.telaCheia || ui.value.quickAberto) {
            return
        }
        peekJob = scope.launch {
            delay(2_800)
            if (appEmPrimeiroPlano || _painelAberto.value || ui.value.telaCheia || ui.value.quickAberto) {
                return@launch
            }
            aplicarPeek(true)
        }
    }

    private fun cancelarPeek() {
        peekJob?.cancel()
        peekJob = null
        if (peekAtivo) aplicarPeek(false)
    }

    private fun aplicarPeek(ligar: Boolean) {
        val w = view?.width?.takeIf { it > 0 } ?: tamanhoFabPx()
        val metade = (w * 0.45f).toInt()
        if (ligar && !peekAtivo) {
            xAntesDoPeek = params.x
            val esq = PrefsRepository.bolhaLadoEsquerdo
            params.x = if (esq) -metade else xAntesDoPeek + metade
            peekAtivo = true
            ui.update { it.copy(offsetX = params.x) }
            atualizar()
        } else if (!ligar && peekAtivo) {
            params.x = xAntesDoPeek
            peekAtivo = false
            ui.update { it.copy(offsetX = params.x) }
            atualizar()
        }
    }

    private fun persistirPosicao(x: Int, y: Int) {
        val ladoEsq = x + (view?.width ?: tamanhoFabPx()) / 2 < recursos().widthPixels / 2
        PrefsRepository.setBolhaPosicao(ladoEsq, y)
        ui.update { it.copy(offsetX = x, offsetY = y) }
    }

    private fun estaNaZonaDismiss(offsetY: Int, fabPx: Int): Boolean {
        val screenH = recursos().heightPixels
        val centroY = offsetY + fabPx / 2f
        return centroY >= screenH * 0.78f
    }

    private fun clampY(y: Int, fabPx: Int): Int {
        val d = recursos().density
        val top = (20f * d).toInt()
        val bottomPad = (56f * d).toInt()
        val maxY = (recursos().heightPixels - fabPx - bottomPad).coerceAtLeast(top)
        return y.coerceIn(top, maxY)
    }

    private fun tamanhoFabPx(): Int {
        val w = view?.width ?: 0
        return if (w > 0 && !ui.value.telaCheia) w else estimativaFabPx()
    }

    private fun estimativaFabPx(): Int = (80f * recursos().density).toInt()

    /** Long-press / expandir do quick → painel completo. */
    private fun expandirPainel(rascunho: String = "") {
        cancelarPeek()
        fecharQuick()
        BolhaSinal.limparBadge()
        marcarMsgsVistas()
        snapAnimator?.cancel()
        val v = view
        val st = ui.value
        val fabCx: Float
        val fabCy: Float
        if (st.telaCheia) {
            fabCx = st.offsetX + tamanhoFabPx() / 2f
            fabCy = st.offsetY + tamanhoFabPx() / 2f
        } else {
            fabCx = params.x + (v?.width?.takeIf { it > 0 } ?: estimativaFabPx()) / 2f
            fabCy = params.y + (v?.height?.takeIf { it > 0 } ?: estimativaFabPx()) / 2f
        }
        // B2: FAB some no mesmo instante em que o painel sobe (evita frame vazio).
        _painelAberto.value = true
        view?.visibility = View.GONE
        CrashReporting.breadcrumb("bolha_open_panel")
        val ok = runCatching {
            startActivity(BolhaPainelActivity.intent(this, fabCx, fabCy, rascunho))
        }.isSuccess
        if (!ok) {
            _painelAberto.value = false
            atualizarVisibilidadeFab()
        }
    }

    private fun marcarMsgsVistas() {
        val last = ChatRepository.getConversa(ChatRepository.conversaPrincipal())
            ?.mensagens?.lastOrNull { it.isLuna }
        PrefsRepository.setBolhaLastLunaMsgId(last?.id)
        BolhaSinal.limparBadge()
    }

    private fun aoFecharPainel() {
        ui.update { it.copy(enterNonce = it.enterNonce + 1) }
        atualizarVisibilidadeFab()
        agendarPeek()
    }

    private fun atualizarVisibilidadeFab() {
        val mostrar = !_painelAberto.value && !appEmPrimeiroPlano
        view?.visibility = if (mostrar) View.VISIBLE else View.GONE
    }

    private fun atualizar() {
        val v = view ?: return
        runCatching { windowManager.updateViewLayout(v, params) }
    }

    private fun pararSozinho() {
        snapAnimator?.cancel()
        peekJob?.cancel()
        PrefsRepository.setBolhaAtiva(false)
        stopSelf()
    }

    private fun tickHaptic(forte: Boolean = false) {
        if (!PrefsRepository.vibracao.value) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(
                    if (forte) VibrationEffect.EFFECT_CLICK else VibrationEffect.EFFECT_TICK,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (forte) 24 else 12)
        }
    }

    private fun recursos() = resources.displayMetrics

    override fun onDestroy() {
        snapAnimator?.cancel()
        peekJob?.cancel()
        scope.cancel()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
        if (instancia === this) instancia = null
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        _rodando.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun irParaPrimeiroPlano() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL,
                "Bolha da Luna",
                NotificationManager.IMPORTANCE_MIN,
            ).apply { description = "Bolha ativa em segundo plano." }
            manager.createNotificationChannel(canal)
        }
        val abrir = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notif: Notification = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Bolha da Luna ativa")
            .setContentText("Some com o app aberto · toque pra abrir o Orbit")
            .setContentIntent(abrir)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, ID_NOTIF, notif, tipo)
    }

    companion object {
        private const val CANAL = "bolha_luna"
        private const val ID_NOTIF = 4201
        const val EXTRA_ABRIR_LUNA = "abrir_luna"

        private const val FLAGS_BOLHA =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        /** Quick reply: focável pro IME, sem ser modal no app de baixo. */
        private const val FLAGS_QUICK =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        private val _rodando = MutableStateFlow(false)
        val rodando: StateFlow<Boolean> = _rodando.asStateFlow()

        private val _painelAberto = MutableStateFlow(false)
        val painelAberto: StateFlow<Boolean> = _painelAberto.asStateFlow()

        @Volatile
        private var instancia: BolhaLunaService? = null

        fun avisarPainelAberto() {
            _painelAberto.value = true
            instancia?.cancelarPeek()
            instancia?.fecharQuick()
            BolhaSinal.limparBadge()
            instancia?.marcarMsgsVistas()
            instancia?.atualizarVisibilidadeFab()
        }

        fun avisarPainelFechado() {
            _painelAberto.value = false
            instancia?.aoFecharPainel()
        }

        fun ligar(context: Context) {
            PrefsRepository.setBolhaAtiva(true)
            context.startForegroundService(Intent(context, BolhaLunaService::class.java))
        }

        fun desligar(context: Context) {
            PrefsRepository.setBolhaAtiva(false)
            context.stopService(Intent(context, BolhaLunaService::class.java))
        }

        fun tentarReligarSePreferida(context: Context): Boolean {
            if (!PrefsRepository.bolhaAtiva.value) return false
            if (!OverlayPermissao.concedida(context)) return false
            if (_rodando.value) return true
            ligar(context)
            return true
        }

        private fun tipoOverlay(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
    }
}

private data class BolhaUiState(
    val offsetX: Int,
    val offsetY: Int,
    val telaCheia: Boolean,
    val sobreDismiss: Boolean,
    val quickAberto: Boolean,
    val enterNonce: Int,
)
