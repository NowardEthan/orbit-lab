package com.ethan.orbitlab.data.captura

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Escuta avisos dos bancos no aparelho. Processamento local — nada sobe
 * até o usuário confirmar a sugestão na UI.
 */
class CapturaNotificacaoService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "listener conectado (regras v${CapturaRegras.VERSAO})")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (!CapturaRepository.podeCapturar()) return
        val pacote = sbn.packageName ?: return
        if (CapturaRegras.bancoPorPacote(pacote) == null) return
        // Ignora grupo/sumário
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val extras = sbn.notification.extras ?: return
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val texto = sequenceOf(
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
        ).filterNotNull().firstOrNull { it.isNotBlank() }

        runCatching {
            CapturaRepository.onNotificacao(
                pacote = pacote,
                titulo = titulo,
                texto = texto,
                quandoMs = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
                notifKey = sbn.key ?: "${pacote}_${sbn.id}_${sbn.postTime}",
            )
        }.onFailure { e ->
            Log.w(TAG, "falha ao parsear aviso de $pacote", e)
        }
    }

    companion object {
        private const val TAG = "OrbitCaptura"
    }
}
