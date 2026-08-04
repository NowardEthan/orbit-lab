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
 */
class BolhaPainelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrbitLabTheme {
                BackHandler { fechar() }
                BolhaLunaPainel(
                    onFechar = { fechar() },
                    onAbrirNoApp = { abrirNoApp() },
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
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, BolhaPainelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}
