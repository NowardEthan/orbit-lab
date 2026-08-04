package com.ethan.orbitlab.ui.bolha

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * A permissão especial "desenhar sobre outros apps" (SYSTEM_ALERT_WINDOW).
 *
 * Diferente das permissões normais: não dá pra pedir num diálogo — o Android manda o usuário
 * pra uma tela de Ajustes e ele liga na mão. Aqui só checamos e abrimos essa tela.
 */
object OverlayPermissao {
    /** Em Android < 6 a permissão vinha concedida pela instalação; daí em diante é manual. */
    fun concedida(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    /** Abre a tela de Ajustes já apontando pro nosso app. */
    fun abrirAjustes(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
