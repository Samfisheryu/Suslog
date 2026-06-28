package app.suslog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import app.suslog.settings.AppSettingsStore
import app.suslog.settings.BiometricAuth
import app.suslog.settings.LocalAccountStore
import app.suslog.ui.SuslogApp
import app.suslog.ui.theme.SuslogTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsStore = AppSettingsStore(this)
        val biometricAuth = BiometricAuth(this)
        val localAccountStore = LocalAccountStore(this)

        setContent {
            SuslogTheme {
                SuslogApp(
                    settingsStore = settingsStore,
                    biometricAuth = biometricAuth,
                    localAccountStore = localAccountStore
                )
            }
        }
    }
}
