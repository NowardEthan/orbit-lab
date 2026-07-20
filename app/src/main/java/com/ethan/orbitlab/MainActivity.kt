package com.ethan.orbitlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.shell.OrbitShell
import com.ethan.orbitlab.ui.theme.OrbitLabTheme
import com.ethan.orbitlab.updates.OrbitUpdatesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrbitLabTheme {
                val updatesVm: OrbitUpdatesViewModel = viewModel()
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, updatesVm) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            updatesVm.onAppForeground()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                OrbitShell(updatesVm = updatesVm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        UpdatesRepository.refresh()
    }
}
