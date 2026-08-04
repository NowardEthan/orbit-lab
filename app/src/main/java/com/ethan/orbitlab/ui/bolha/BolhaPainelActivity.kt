package com.ethan.orbitlab.ui.bolha

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ethan.orbitlab.MainActivity
import com.ethan.orbitlab.ui.theme.OrbitLabTheme

/**
 * Painel da bolha em Activity translúcida — precisa ser Activity de verdade pra o
 * [com.ethan.orbitlab.ui.chat.ChatInputArea] (pickers, câmera, mic, sheets) funcionar.
 *
 * Tarefa própria ([taskAffinity]): ao fechar, o usuário volta pro app que estava
 * (WhatsApp etc.); a bolha no [BolhaLunaService] reaparece.
 *
 * Recebe o centro do FAB em coords de tela pra o sheet nascer/recolher dali.
 */
class BolhaPainelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sem transição de Activity — a animação é só o Compose (a partir do FAB).
        overridePendingTransition(0, 0)
        enableEdgeToEdge()
        val fabCx = intent.getFloatExtra(EXTRA_FAB_CX, -1f)
        val fabCy = intent.getFloatExtra(EXTRA_FAB_CY, -1f)
        val rascunho = intent.getStringExtra(EXTRA_RASCUNHO).orEmpty()
        setContent {
            OrbitLabTheme {
                BackHandler { fechar() }
                BolhaLunaPainel(
                    onFechar = { fechar() },
                    onAbrirNoApp = { abrirNoApp() },
                    fabOrigemX = fabCx,
                    fabOrigemY = fabCy,
                    rascunhoInicial = rascunho,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        BolhaLunaService.avisarPainelAberto()
    }

    override fun onDestroy() {
        BolhaLunaService.avisarPainelFechado()
        super.onDestroy()
    }

    private fun fechar() {
        finish()
        overridePendingTransition(0, 0)
    }

    private fun abrirNoApp() {
        BolhaNav.pedirAbrirChat()
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(BolhaLunaService.EXTRA_ABRIR_LUNA, true)
            },
        )
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_FAB_CX = "fab_cx"
        const val EXTRA_FAB_CY = "fab_cy"
        const val EXTRA_RASCUNHO = "rascunho"

        fun intent(
            context: Context,
            fabCx: Float,
            fabCy: Float,
            rascunho: String = "",
        ): Intent =
            Intent(context, BolhaPainelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_FAB_CX, fabCx)
                putExtra(EXTRA_FAB_CY, fabCy)
                if (rascunho.isNotBlank()) putExtra(EXTRA_RASCUNHO, rascunho)
            }
    }
}
