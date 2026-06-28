package app.suslog.settings

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuth(private val activity: FragmentActivity) {
    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun status(): BiometricAuthStatus {
        val manager = BiometricManager.from(activity)

        return when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAuthStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAuthStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAuthStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAuthStatus.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAuthStatus.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAuthStatus.UNAVAILABLE
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAuthStatus.UNAVAILABLE
            else -> BiometricAuthStatus.UNAVAILABLE
        }
    }

    fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (status() != BiometricAuthStatus.AVAILABLE) {
            onError("Device security is not available.")
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onError("Authentication failed.")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }
}

enum class BiometricAuthStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE,
}
