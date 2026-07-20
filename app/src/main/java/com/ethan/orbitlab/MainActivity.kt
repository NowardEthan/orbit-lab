package com.ethan.orbitlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.shell.OrbitShell
import com.ethan.orbitlab.ui.theme.OrbitLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrbitLabTheme {
                OrbitShell()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        UpdatesRepository.refresh()
    }
}
