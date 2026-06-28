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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.suslog.domain.suspension.StiffSide
import com.example.suslog.domain.suspension.SuspensionType
import com.example.suslog.settings.BiometricAuthStatus
import com.example.suslog.settings.SetupDefaults
import com.example.suslog.settings.TuningPreferences
import com.example.suslog.ui.common.ChoiceButton
import com.example.suslog.ui.theme.SuslogTheme

@Composable
fun SettingsScreen(
    appLockEnabled: Boolean,
    biometricStatus: BiometricAuthStatus,
    accountMessage: String?,
    setupDefaults: SetupDefaults,
    tuningPreferences: TuningPreferences,
    onAppLockChange: (Boolean) -> Unit,
    onUnlockNow: () -> Unit,
    onDefaultAxleLockChange: (Boolean) -> Unit,
    onDefaultSuspensionTypeChange: (SuspensionType) -> Unit,
    onDefaultMaxClicksChange: (Int) -> Unit,
    onDefaultStiffSideChange: (StiffSide) -> Unit,
    onRequireFeedbackBeforeExitConfigChange: (Boolean) -> Unit,
    onShowPreviousFeedbackReferenceChange: (Boolean) -> Unit,
    onShowSetupDebugInfoChange: (Boolean) -> Unit,
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

        SettingsSection(title = "Setup Defaults") {
            SwitchSettingRow(
                title = "Default Axle L/R Lock",
                body = "New sessions keep left and right axle adjusters matched.",
                checked = setupDefaults.axleLockEnabled,
                onCheckedChange = onDefaultAxleLockChange
            )
            SuspensionTypeSelector(
                value = setupDefaults.suspensionType,
                onValueChange = onDefaultSuspensionTypeChange
            )
            MaxClicksRow(
                value = setupDefaults.maxClicks,
                onValueChange = onDefaultMaxClicksChange
            )
            StiffSideSelector(
                value = setupDefaults.stiffSide,
                maxClicks = setupDefaults.maxClicks,
                onValueChange = onDefaultStiffSideChange
            )
        }

        SettingsSection(title = "Tuning Preferences") {
            SwitchSettingRow(
                title = "Require Feedback Before Exit",
                body = "Block exiting an active config until the current setup has feedback.",
                checked = tuningPreferences.requireFeedbackBeforeExitConfig,
                onCheckedChange = onRequireFeedbackBeforeExitConfigChange
            )
            SwitchSettingRow(
                title = "Previous Feedback Reference",
                body = "Show the last config feedback as a muted reference after setup changes.",
                checked = tuningPreferences.showPreviousFeedbackReference,
                onCheckedChange = onShowPreviousFeedbackReferenceChange
            )
            SwitchSettingRow(
                title = "Setup Debug Info",
                body = "Show config state diagnostics on the setup screen.",
                checked = tuningPreferences.showSetupDebugInfo,
                onCheckedChange = onShowSetupDebugInfoChange
            )
            DisabledSettingRow(
                title = "Score Model",
                value = "V1",
                badge = null
            )
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

        SettingsSection(title = "About") {
            DisabledSettingRow(
                title = "App Version",
                value = "1.0",
                badge = null
            )
            DisabledSettingRow(
                title = "Database",
                value = "v7",
                badge = null
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
private fun SwitchSettingRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SuspensionTypeSelector(
    value: SuspensionType,
    onValueChange: (SuspensionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingLabel(
            title = "Default Suspension Type",
            body = "Used when adding a new car."
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuspensionType.entries.forEach { type ->
                ChoiceButton(
                    label = type.label,
                    selected = type == value,
                    onClick = { onValueChange(type) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MaxClicksRow(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    val parsed = text.toIntOrNull()
    val valid = parsed != null && parsed in 1..99

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingLabel(
            title = "Default Click Count",
            body = "Maximum click value for new car adjusters."
        )
        OutlinedTextField(
            value = text,
            onValueChange = { next ->
                text = next.filter(Char::isDigit).take(2)
                text.toIntOrNull()?.takeIf { it in 1..99 }?.let(onValueChange)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Max click") },
            singleLine = true,
            isError = text.isNotEmpty() && !valid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun StiffSideSelector(
    value: StiffSide,
    maxClicks: Int,
    onValueChange: (StiffSide) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingLabel(
            title = "Default Stiff Direction",
            body = "Used when adding a new car."
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceButton(
                label = "1 is stiff",
                selected = value == StiffSide.LOW_VALUE,
                onClick = { onValueChange(StiffSide.LOW_VALUE) },
                modifier = Modifier.weight(1f)
            )
            ChoiceButton(
                label = "$maxClicks is stiff",
                selected = value == StiffSide.HIGH_VALUE,
                onClick = { onValueChange(StiffSide.HIGH_VALUE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingLabel(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    badge: String? = "Soon",
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
        badge?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            setupDefaults = SetupDefaults(
                axleLockEnabled = true,
                suspensionType = SuspensionType.TWO_WAY,
                maxClicks = 30,
                stiffSide = StiffSide.HIGH_VALUE
            ),
            tuningPreferences = TuningPreferences(
                requireFeedbackBeforeExitConfig = true,
                showPreviousFeedbackReference = true,
                showSetupDebugInfo = false
            ),
            onAppLockChange = {},
            onUnlockNow = {},
            onDefaultAxleLockChange = {},
            onDefaultSuspensionTypeChange = {},
            onDefaultMaxClicksChange = {},
            onDefaultStiffSideChange = {},
            onRequireFeedbackBeforeExitConfigChange = {},
            onShowPreviousFeedbackReferenceChange = {},
            onShowSetupDebugInfoChange = {}
        )
    }
}
