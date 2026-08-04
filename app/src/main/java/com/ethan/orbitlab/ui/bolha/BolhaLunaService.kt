package com.ethan.orbitlab.ui.bolha

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
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
import com.ethan.orbitlab.data.PrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A bolha da Luna (chat-head) — flutua sobre qualquer app.
 *
 * Overlay = só o FAB (WRAP). No arraste vira tela cheia pra zona “Guardar”.
 * Painel = [BolhaPainelActivity]. FAB some com o OrbitLab em foreground.
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

    private lateinit var windowManager: WindowManager
    private var view: ComposeView? = null
    private var snapAnimator: ValueAnimator? = null

    private var appEmPrimeiroPlano = false
    private var jaAvisouZonaDismiss = false

    private val ui = MutableStateFlow(
        BolhaUiState(
            offsetX = 0,
            offsetY = PrefsRepository.bolhaY,
            telaCheia = false,
            sobreDismiss = false,
        ),
    )

    private val appLifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                appEmPrimeiroPlano = true
                atualizarVisibilidadeFab()
            }
            Lifecycle.Event.ON_STOP -> {
                appEmPrimeiroPlano = false
                atualizarVisibilidadeFab()
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
        _rodando.value = true
        atualizarVisibilidadeFab()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        irParaPrimeiroPlano()
        return START_STICKY
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
        ui.value = BolhaUiState(offsetX = x, offsetY = y, telaCheia = false, sobreDismiss = false)
    }

    private fun montarBolha() {
        val compose = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BolhaLunaService)
            setViewTreeViewModelStoreOwner(this@BolhaLunaService)
            setViewTreeSavedStateRegistryOwner(this@BolhaLunaService)
            setContent {
                val state by ui.collectAsState()
                val vibracao by PrefsRepository.vibracao.collectAsState()
                BolhaOverlay(
                    telaCheia = state.telaCheia,
                    offsetX = state.offsetX,
                    offsetY = state.offsetY,
                    sobreDismiss = state.sobreDismiss,
                    hapticsLigados = vibracao,
                    onDragStart = { iniciarArraste() },
                    onArrastar = { dx, dy -> moverArraste(dx, dy) },
                    onSoltar = { soltarArraste() },
                    onTocar = { expandir() },
                )
            }
        }
        view = compose
        runCatching { windowManager.addView(compose, params) }
        // Após layout, corrige X da direita com a largura real.
        compose.post { alinharLadoAposLayout() }
    }

    private fun alinharLadoAposLayout() {
        if (ui.value.telaCheia) return
        val w = view?.width?.takeIf { it > 0 } ?: return
        val alvo = if (PrefsRepository.bolhaLadoEsquerdo) 0 else recursos().widthPixels - w
        if (params.x != alvo) {
            params.x = alvo
            ui.update { it.copy(offsetX = alvo) }
            atualizar()
        }
    }

    private fun iniciarArraste() {
        snapAnimator?.cancel()
        snapAnimator = null
        jaAvisouZonaDismiss = false
        val x = params.x
        val y = params.y
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.x = 0
        params.y = 0
        ui.value = BolhaUiState(
            offsetX = x,
            offsetY = y,
            telaCheia = true,
            sobreDismiss = false,
        )
        atualizar()
    }

    private fun moverArraste(dx: Int, dy: Int) {
        val st = ui.value
        if (!st.telaCheia) return
        val fab = tamanhoFabPx()
        val nx = (st.offsetX + dx).coerceIn(0, (recursos().widthPixels - fab).coerceAtLeast(0))
        val ny = clampY(st.offsetY + dy, fab)
        val zona = estaNaZonaDismiss(ny, fab)
        if (zona && !jaAvisouZonaDismiss) {
            jaAvisouZonaDismiss = true
            tickHaptic()
        }
        if (!zona) jaAvisouZonaDismiss = false
        ui.value = st.copy(offsetX = nx, offsetY = ny, sobreDismiss = zona)
    }

    private fun soltarArraste() {
        val st = ui.value
        if (!st.telaCheia) {
            grudarNaBordaAnimado(params.x, params.y)
            return
        }
        if (st.sobreDismiss) {
            tickHaptic(forte = true)
            pararSozinho()
            return
        }
        // Volta pro WRAP na posição atual e anima o snap horizontal.
        val fab = tamanhoFabPx()
        val x = st.offsetX.coerceIn(0, (recursos().widthPixels - fab).coerceAtLeast(0))
        val y = clampY(st.offsetY, fab)
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.x = x
        params.y = y
        ui.value = BolhaUiState(offsetX = x, offsetY = y, telaCheia = false, sobreDismiss = false)
        atualizar()
        view?.post { grudarNaBordaAnimado(params.x, params.y) }
    }

    private fun grudarNaBordaAnimado(fromX: Int, fromY: Int) {
        snapAnimator?.cancel()
        val w = view?.width?.takeIf { it > 0 } ?: tamanhoFabPx()
        val screenW = recursos().widthPixels
        val targetX = if (fromX + w / 2 < screenW / 2) 0 else screenW - w
        val targetY = clampY(fromY, w)
        params.y = targetY

        if (fromX == targetX) {
            params.x = targetX
            persistirPosicao(targetX, targetY)
            atualizar()
            tickHaptic()
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
                override fun onAnimationEnd(animation: Animator) {
                    params.x = targetX
                    persistirPosicao(targetX, targetY)
                    atualizar()
                    tickHaptic()
                    snapAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    snapAnimator = null
                }
            })
            start()
        }
    }

    private fun tickHaptic(forte: Boolean = false) {
        if (!PrefsRepository.vibracao.value) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = getSystemService(VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val efeito = if (forte) {
                VibrationEffect.EFFECT_CLICK
            } else {
                VibrationEffect.EFFECT_TICK
            }
            vibrator.vibrate(VibrationEffect.createPredefined(efeito))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (forte) 24 else 12)
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

    private fun estimativaFabPx(): Int {
        // 56dp + folga 12dp × 2.
        return (80f * recursos().density).toInt()
    }

    /** Toque na bolha → Activity translúcida com o chat completo. */
    private fun expandir() {
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
        _painelAberto.value = true
        atualizarVisibilidadeFab()
        runCatching { startActivity(BolhaPainelActivity.intent(this, fabCx, fabCy)) }
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
        PrefsRepository.setBolhaAtiva(false)
        stopSelf()
    }

    private fun recursos() = resources.displayMetrics

    override fun onDestroy() {
        snapAnimator?.cancel()
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
            ).apply { description = "A Luna flutuando sobre outros apps." }
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
            .setContentTitle("Luna por perto 🌙")
            .setContentText("Toque na bolha pra falar · arraste pra baixo pra guardar.")
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

        private val _rodando = MutableStateFlow(false)
        val rodando: StateFlow<Boolean> = _rodando.asStateFlow()

        private val _painelAberto = MutableStateFlow(false)

        @Volatile
        private var instancia: BolhaLunaService? = null

        fun avisarPainelAberto() {
            _painelAberto.value = true
            instancia?.atualizarVisibilidadeFab()
        }

        fun avisarPainelFechado() {
            _painelAberto.value = false
            instancia?.atualizarVisibilidadeFab()
        }

        fun ligar(context: Context) {
            PrefsRepository.setBolhaAtiva(true)
            val intent = Intent(context, BolhaLunaService::class.java)
            context.startForegroundService(intent)
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
)
