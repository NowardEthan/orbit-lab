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
import android.view.View
import android.view.WindowManager
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
 * Overlay = só o FAB. O painel de conversa abre em [BolhaPainelActivity] (Activity
 * translúcida) pra o composer completo (anexos/mic/câmera) funcionar de verdade.
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
        instancia = this
        montarBolha()
        _rodando.value = true
        // Se o painel já estava aberto (recreate), mantém FAB escondida.
        atualizarVisibilidadeFab()
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
        view = compose
        runCatching { windowManager.addView(compose, params) }
    }

    private fun grudarNaBorda() {
        val largura = recursos().widthPixels
        params.x = if (params.x + (view?.width ?: 0) / 2 < largura / 2) 0 else largura - (view?.width ?: 0)
        bolhaX = params.x
        bolhaY = params.y
        atualizar()
    }

    /** Toque na bolha → Activity translúcida com o chat completo. */
    private fun expandir() {
        _painelAberto.value = true
        atualizarVisibilidadeFab()
        runCatching { startActivity(BolhaPainelActivity.intent(this)) }
    }

    private fun atualizarVisibilidadeFab() {
        view?.visibility = if (_painelAberto.value) View.GONE else View.VISIBLE
    }

    private fun atualizar() {
        val v = view ?: return
        runCatching { windowManager.updateViewLayout(v, params) }
    }

    private fun pararSozinho() {
        PrefsRepository.setBolhaAtiva(false)
        stopSelf()
    }

    private fun recursos() = resources.displayMetrics

    override fun onDestroy() {
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

        private const val FLAGS_BOLHA =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        private val _rodando = MutableStateFlow(false)
        val rodando: StateFlow<Boolean> = _rodando

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
