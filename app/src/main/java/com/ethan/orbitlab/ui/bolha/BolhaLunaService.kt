package com.ethan.orbitlab.ui.bolha

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
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
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

/**
 * A bolha da Luna (chat-head) — flutua sobre qualquer app.
 *
 * É um Service em primeiro plano (pra sobreviver com o OrbitLab fechado) que pendura uma
 * [ComposeView] num overlay do [WindowManager]. Compose fora de uma Activity precisa de três
 * "donos" que a Activity normalmente fornece — lifecycle, savedState e viewModelStore — então
 * o serviço faz esse papel. A bolha em si (arrastar, grudar na borda) vive na [BolhaOverlay].
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

    /** Bolha recolhida (só o círculo) vs painel de conversa aberto. */
    private val expandido = MutableStateFlow(false)
    // Onde a bolha ficou quando recolhida — pra voltar pro mesmo canto ao fechar o painel.
    private var bolhaX = 0
    private var bolhaY = 240

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        tipoOverlay(),
        FLAGS_BOLHA,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = bolhaX
        y = bolhaY
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        montarBolha()
        _rodando.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        irParaPrimeiroPlano()
        return START_STICKY
    }

    private fun montarBolha() {
        val compose = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BolhaLunaService)
            setViewTreeViewModelStoreOwner(this@BolhaLunaService)
            setViewTreeSavedStateRegistryOwner(this@BolhaLunaService)
            setContent {
                val aberto by expandido.collectAsState()
                if (aberto) {
                    BolhaLunaPainel(
                        onFechar = { recolher() },
                        onAbrirNoApp = { abrirApp(); recolher() },
                    )
                } else {
                    BolhaOverlay(
                        onArrastar = { dx, dy ->
                            params.x += dx
                            params.y += dy
                            bolhaX = params.x
                            bolhaY = params.y
                            atualizar()
                        },
                        onSoltar = { grudarNaBorda() },
                        onTocar = { expandir() },
                        onFechar = { pararSozinho() },
                    )
                }
            }
        }
        view = compose
        runCatching { windowManager.addView(compose, params) }
    }

    /** Solta a bolha na borda mais perto (esquerda/direita), como as chat-heads clássicas. */
    private fun grudarNaBorda() {
        val largura = recursos().widthPixels
        params.x = if (params.x + (view?.width ?: 0) / 2 < largura / 2) 0 else largura - (view?.width ?: 0)
        bolhaX = params.x
        bolhaY = params.y
        atualizar()
    }

    /** Toque na bolha → abre o painel de conversa (janela focável, tela toda, com scrim). */
    private fun expandir() {
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.flags = FLAGS_PAINEL
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        params.x = 0
        params.y = 0
        expandido.value = true
        atualizar()
    }

    /** Fecha o painel e volta a ser só a bolha, no canto onde estava. */
    private fun recolher() {
        expandido.value = false
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = FLAGS_BOLHA
        params.x = bolhaX
        params.y = bolhaY
        atualizar()
    }

    private fun atualizar() {
        val v = view ?: return
        runCatching { windowManager.updateViewLayout(v, params) }
    }

    private fun abrirApp() {
        BolhaNav.pedirAbrirChat()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_ABRIR_LUNA, true)
        }
        runCatching { startActivity(intent) }
    }

    private fun pararSozinho() {
        PrefsRepository.setBolhaAtiva(false)
        stopSelf()
    }

    private fun recursos() = resources.displayMetrics

    override fun onDestroy() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        _rodando.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Primeiro plano (notificação fixa e discreta) ──────────────────────────

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
            .setContentText("Toque na bolha pra falar comigo.")
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

        // Bolha: não rouba foco nem bloqueia toques no app de baixo (só o círculo é tocável).
        private const val FLAGS_BOLHA =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        // Painel: focável (teclado funciona) e modal (o scrim segura o toque de fora pra fechar).
        private const val FLAGS_PAINEL =
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        private val _rodando = MutableStateFlow(false)
        val rodando: StateFlow<Boolean> = _rodando

        fun ligar(context: Context) {
            PrefsRepository.setBolhaAtiva(true)
            val intent = Intent(context, BolhaLunaService::class.java)
            context.startForegroundService(intent)
        }

        fun desligar(context: Context) {
            PrefsRepository.setBolhaAtiva(false)
            context.stopService(Intent(context, BolhaLunaService::class.java))
        }

        /**
         * Se o usuário deixou a bolha ligada e ainda tem permissão de overlay,
         * sobe o serviço de novo (ex.: após abrir o app, ou voltar dos Ajustes).
         */
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
