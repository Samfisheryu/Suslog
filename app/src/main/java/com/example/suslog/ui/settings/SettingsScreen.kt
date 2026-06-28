package com.example.suslog.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.suslog.ui.common.SectionHeader
import com.example.suslog.ui.theme.SuslogTheme

@Composable
fun SettingsScreen(
    appLockEnabled: Boolean,
    biometricStatus: BiometricAuthStatus,
    accountMessage: String?,
    dataMessage: String?,
    setupDefaults: SetupDefaults,
    tuningPreferences: TuningPreferences,
    onAppLockChange: (Boolean) -> Unit,
    onUnlockNow: () -> Unit,
    onExportData: () -> Unit,
    onClearLocalData: () -> Unit,
    onDefaultAxleLockChange: (Boolean) -> Unit,
    onDefaultSuspensionTypeChange: (SuspensionType) -> Unit,
    onDefaultMaxClicksChange: (Int) -> Unit,
    onDefaultStiffSideChange: (StiffSide) -> Unit,
    onRequireFeedbackBeforeExitConfigChange: (Boolean) -> Unit,
    onShowPreviousFeedbackReferenceChange: (Boolean) -> Unit,
    onShowSetupDebugInfoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showCloudTransferDialog by remember { mutableStateOf(false) }

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
            DataActionsRow(
                onExportData = onExportData,
                onMoveToCloud = { showCloudTransferDialog = true },
                onRequestClearLocalData = { showClearDataDialog = true }
            )
            dataMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsSection(title = "About") {
            DisabledSettingRow(
                title = "App Version",
                value = "0.0.1 early testing",
                badge = null
            )
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear local data?") },
            text = {
                Text(
                    "This deletes cars, current setup history, setup configs, and tuning docs. " +
                        "Settings and App Lock stay unchanged."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        onClearLocalData()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCloudTransferDialog) {
        CloudTransferDialog(
            onDismiss = { showCloudTransferDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }

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
            SectionHeader(
                title = title,
                modifier = Modifier.clickable { expanded = !expanded },
                trailing = {
                    Text(
                        text = if (expanded) "-" else "+",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content()
                }
            }
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
private fun DataActionsRow(
    onExportData: () -> Unit,
    onMoveToCloud: () -> Unit,
    onRequestClearLocalData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingLabel(
            title = "Local Data",
            body = "Export a JSON backup or clear this device's saved cars and tuning data."
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onExportData,
                modifier = Modifier.weight(1f)
            ) {
                Text("Export JSON")
            }
            OutlinedButton(
                onClick = onMoveToCloud,
                modifier = Modifier.weight(1f)
            ) {
                Text("Move to Cloud")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onRequestClearLocalData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear Data")
            }
        }
    }
}

@Composable
private fun CloudTransferDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Cloud") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Google sign-in will be used for cloud ownership. Cloud storage is not connected yet because Firebase setup is still required.",
                    style = MaterialTheme.typography.bodyMedium
                )
                DisabledSettingRow(
                    title = "Google Account",
                    value = "Not connected",
                    badge = "Setup"
                )
                DisabledSettingRow(
                    title = "Cloud Backup",
                    value = "Will upload one JSON snapshot after Google/Firebase setup.",
                    badge = "Soon"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = false
            ) {
                Text("Connect Google")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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
            dataMessage = "Export complete.",
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
            onExportData = {},
            onClearLocalData = {},
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
