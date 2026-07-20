package com.ethan.orbitlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
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
}
