package com.example.suslog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.suslog.settings.AppSettingsStore
import com.example.suslog.settings.BiometricAuth
import com.example.suslog.ui.SuslogApp
import com.example.suslog.ui.theme.SuslogTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsStore = AppSettingsStore(this)
        val biometricAuth = BiometricAuth(this)

        setContent {
            SuslogTheme {
                SuslogApp(
                    settingsStore = settingsStore,
                    biometricAuth = biometricAuth
                )
            }
        }
    }
}
