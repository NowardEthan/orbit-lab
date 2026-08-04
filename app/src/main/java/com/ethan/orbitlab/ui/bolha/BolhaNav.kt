package com.ethan.orbitlab.ui.bolha

import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Ponte bolha → app: o serviço manda abrir a conversa principal no OrbitLab;
 * o [com.ethan.orbitlab.shell.OrbitShell] observa e abre o chat.
 */
object BolhaNav {
    private val _abrirChat = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val abrirChat: SharedFlow<Unit> = _abrirChat.asSharedFlow()

    fun pedirAbrirChat() {
        _abrirChat.tryEmit(Unit)
    }

    /** Lê o extra do intent (MainActivity onCreate / onNewIntent). */
    fun consumirIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(BolhaLunaService.EXTRA_ABRIR_LUNA, false) != true) return
        intent.removeExtra(BolhaLunaService.EXTRA_ABRIR_LUNA)
        pedirAbrirChat()
    }
}
