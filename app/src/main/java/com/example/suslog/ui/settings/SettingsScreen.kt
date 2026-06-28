package com.example.suslog.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.suslog.settings.BiometricAuthStatus
import com.example.suslog.ui.theme.SuslogTheme

@Composable
fun SettingsScreen(
    appLockEnabled: Boolean,
    biometricStatus: BiometricAuthStatus,
    accountMessage: String?,
    onAppLockChange: (Boolean) -> Unit,
    onUnlockNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Account, security, and future AI connections.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsSection(title = "Account") {
            AccountLockRow(
                enabled = appLockEnabled,
                biometricStatus = biometricStatus,
                onAppLockChange = onAppLockChange,
                onUnlockNow = onUnlockNow
            )
            accountMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsSection(title = "AI Connections") {
            DisabledSettingRow(
                title = "Google / Gemini",
                value = "Not connected"
            )
            DisabledSettingRow(
                title = "OpenAI",
                value = "Not connected"
            )
        }

        SettingsSection(title = "Data") {
            DisabledSettingRow(
                title = "Export Data",
                value = "Coming later"
            )
            DisabledSettingRow(
                title = "Clear Local Data",
                value = "Coming later"
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun AccountLockRow(
    enabled: Boolean,
    biometricStatus: BiometricAuthStatus,
    onAppLockChange: (Boolean) -> Unit,
    onUnlockNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = biometricStatus == BiometricAuthStatus.AVAILABLE

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "App Lock",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = accountStatusText(
                        enabled = enabled,
                        biometricStatus = biometricStatus
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onAppLockChange,
                enabled = available || enabled
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onUnlockNow,
                enabled = available,
                modifier = Modifier.weight(1f)
            ) {
                Text("Verify Now")
            }
            OutlinedButton(
                onClick = { onAppLockChange(false) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Turn Off")
            }
        }
    }
}

@Composable
private fun DisabledSettingRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Soon",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun accountStatusText(
    enabled: Boolean,
    biometricStatus: BiometricAuthStatus,
): String =
    when {
        biometricStatus == BiometricAuthStatus.AVAILABLE && enabled ->
            "Protected by device security."
        biometricStatus == BiometricAuthStatus.AVAILABLE ->
            "Use fingerprint, face, PIN, pattern, or password."
        biometricStatus == BiometricAuthStatus.NOT_ENROLLED ->
            "Set up device security in Android Settings first."
        biometricStatus == BiometricAuthStatus.NO_HARDWARE ->
            "This device does not support biometric hardware."
        else ->
            "Device security is unavailable."
    }

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun SettingsScreenPreview() {
    SuslogTheme {
        SettingsScreen(
            appLockEnabled = true,
            biometricStatus = BiometricAuthStatus.AVAILABLE,
            accountMessage = "Last verification succeeded.",
            onAppLockChange = {},
            onUnlockNow = {}
        )
    }
}
