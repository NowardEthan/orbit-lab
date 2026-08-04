package com.ethan.orbitlab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.shell.OrbitShell
import com.ethan.orbitlab.ui.bolha.BolhaNav
import com.ethan.orbitlab.ui.theme.OrbitLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BolhaNav.consumirIntent(intent)
        enableEdgeToEdge()
        setContent {
            OrbitLabTheme {
                OrbitShell()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        BolhaNav.consumirIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        UpdatesRepository.refresh()
    }
}
