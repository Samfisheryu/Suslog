package app.suslog.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.suslog.data.ai.AiCredentialStore
import app.suslog.data.ai.AiUsageRepository
import app.suslog.data.ai.AiUsageSummary
import app.suslog.data.ai.OpenAiTuningAiClient
import app.suslog.data.local.SuslogDatabase
import app.suslog.domain.suspension.StiffSide
import app.suslog.domain.suspension.SuspensionType
import app.suslog.settings.BiometricAuthStatus
import app.suslog.settings.SetupDefaults
import app.suslog.settings.TuningPreferences
import app.suslog.ui.common.ChoiceButton
import app.suslog.ui.common.SectionHeader
import app.suslog.ui.theme.SuslogTheme
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    appLockEnabled: Boolean,
    biometricStatus: BiometricAuthStatus,
    activeAccountId: String?,
    activeAccountEmail: String?,
    carCount: Int,
    setupConfigCount: Int,
    accountMessage: String?,
    dataMessage: String?,
    setupDefaults: SetupDefaults,
    tuningPreferences: TuningPreferences,
    onAppLockChange: (Boolean) -> Unit,
    onLogOff: () -> Unit,
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
            AccountRow(
                appLockEnabled = appLockEnabled,
                biometricStatus = biometricStatus,
                activeAccountEmail = activeAccountEmail,
                carCount = carCount,
                setupConfigCount = setupConfigCount,
                onAppLockChange = onAppLockChange,
                onLogOff = onLogOff
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
            OpenAiConnectionRow(activeAccountId = activeAccountId)
            DisabledSettingRow(
                title = "Google / Gemini",
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
private fun AccountRow(
    appLockEnabled: Boolean,
    biometricStatus: BiometricAuthStatus,
    activeAccountEmail: String?,
    carCount: Int,
    setupConfigCount: Int,
    onAppLockChange: (Boolean) -> Unit,
    onLogOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = biometricStatus == BiometricAuthStatus.AVAILABLE
    val accountText = activeAccountEmail ?: "Not logged in"
    val summaryText = "${countLabel(carCount, "car")} · ${countLabel(setupConfigCount, "config")}"

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
                        enabled = appLockEnabled,
                        biometricStatus = biometricStatus
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = appLockEnabled,
                onCheckedChange = onAppLockChange,
                enabled = available || appLockEnabled
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Current User",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = accountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onLogOff,
                enabled = activeAccountEmail != null
            ) {
                Text("Log Off")
            }
        }
    }
}

private fun countLabel(
    count: Int,
    singular: String,
): String =
    "$count ${if (count == 1) singular else "${singular}s"}"

private fun Double.formatBudgetInput(): String =
    String.format(Locale.US, "%.2f", this)

private fun Double.formatUsd(): String =
    "$" + String.format(Locale.US, "%.2f", this)

private fun Long.formatTokens(): String =
    String.format(Locale.US, "%,d", this)

private fun String.parseUsdInput(): Double? {
    val normalized = trim()
        .removePrefix("$")
        .replace(",", "")

    if (normalized.isBlank()) return null

    return normalized.toDoubleOrNull()?.takeIf { it > 0.0 }
}

private fun previewAiUsageSummary(): AiUsageSummary =
    AiUsageSummary(
        inputTokens = 124_300,
        outputTokens = 18_420,
        requestCount = 12,
        estimatedCostUsd = 0.42,
        unpricedModelCount = 0,
        periodStartMillis = 0L,
        periodEndMillis = 0L
    )

@Composable
private fun OpenAiConnectionRow(
    activeAccountId: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val credentials = remember(activeAccountId) { AiCredentialStore(context, activeAccountId) }
    var apiKey by remember(activeAccountId) { mutableStateOf("") }
    var model by remember(activeAccountId) { mutableStateOf(credentials.model) }
    var connected by remember(activeAccountId) { mutableStateOf(credentials.isConnected) }
    var editingConnection by remember(activeAccountId) {
        mutableStateOf(!credentials.isConnected)
    }
    var monthlyBudgetUsd by remember(activeAccountId) {
        mutableStateOf(credentials.monthlyBudgetUsd)
    }
    var budgetInput by remember(activeAccountId) {
        mutableStateOf(monthlyBudgetUsd?.formatBudgetInput().orEmpty())
    }
    var budgetError by remember(activeAccountId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val client = remember { OpenAiTuningAiClient() }
    val usageRepository = remember(inPreview) {
        if (inPreview) {
            null
        } else {
            AiUsageRepository(SuslogDatabase.getInstance(context).suslogDao())
        }
    }
    var usage by remember(activeAccountId, inPreview) {
        mutableStateOf(
            if (inPreview) {
                previewAiUsageSummary()
            } else {
                null
            }
        )
    }
    var usageLoading by remember(activeAccountId) { mutableStateOf(false) }
    var usageError by remember(activeAccountId) { mutableStateOf<String?>(null) }
    var models by remember(activeAccountId) { mutableStateOf<List<String>>(emptyList()) }
    var loadingModels by remember(activeAccountId) { mutableStateOf(false) }
    var modelsError by remember(activeAccountId) { mutableStateOf<String?>(null) }
    var modelMenuOpen by remember(activeAccountId) { mutableStateOf(false) }
    val enabled = activeAccountId != null
    val detailsLocked = enabled && connected && !editingConnection
    val detailFieldsEnabled = enabled && !detailsLocked
    val apiKeyFieldValue = if (detailsLocked) "************" else apiKey

    fun loadModels() {
        if (!enabled || loadingModels) return
        loadingModels = true
        modelsError = null
        val pendingApiKey = apiKey
        scope.launch {
            val key = pendingApiKey.ifBlank { credentials.apiKey }
            if (key.isBlank()) {
                modelsError = "Save an OpenAI API key first."
                loadingModels = false
                return@launch
            }
            client.listChatModels(key)
                .onSuccess { models = it }
                .onFailure { modelsError = it.message ?: "Could not load models." }
            loadingModels = false
        }
    }

    LaunchedEffect(connected) {
        if (connected && models.isEmpty()) loadModels()
    }

    LaunchedEffect(activeAccountId, usageRepository) {
        val userId = activeAccountId
        if (userId == null || usageRepository == null) {
            usage = if (inPreview) previewAiUsageSummary() else null
            usageLoading = false
            return@LaunchedEffect
        }

        usageLoading = true
        usageError = null
        usage = runCatching {
            usageRepository.loadCurrentMonthOpenAiUsage(userId)
        }
            .onFailure { usageError = it.message ?: "Could not load AI usage." }
            .getOrNull()
        usageLoading = false
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OpenAI",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (connected) {
                        "Connected. Your key stays on this device."
                    } else if (!enabled) {
                        "Login to a local account first."
                    } else {
                        "Bring your own API key."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (connected) {
                Text(text = "●", color = MaterialTheme.colorScheme.primary)
            }
        }
        OutlinedTextField(
            value = apiKeyFieldValue,
            onValueChange = {
                apiKey = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API key (sk-…)") },
            placeholder = {
                when {
                    detailsLocked -> Text("************")
                    connected -> Text("Leave blank to keep saved key")
                }
            },
            enabled = detailFieldsEnabled,
            singleLine = true,
            visualTransformation = if (detailsLocked) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            }
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    if (models.isNotEmpty()) modelMenuOpen = true else loadModels()
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (loadingModels) "Loading models…" else "Model: $model",
                    modifier = Modifier.weight(1f)
                )
                Text(text = if (models.isEmpty()) "Load" else "▾")
            }
            DropdownMenu(
                expanded = modelMenuOpen,
                onDismissRequest = { modelMenuOpen = false }
            ) {
                models.forEach { candidate ->
                    DropdownMenuItem(
                        text = { Text(candidate) },
                        onClick = {
                            model = candidate
                            credentials.model = candidate
                            modelMenuOpen = false
                        }
                    )
                }
            }
        }
        modelsError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (detailsLocked) {
                        editingConnection = true
                        apiKey = ""
                        budgetInput = monthlyBudgetUsd?.formatBudgetInput().orEmpty()
                        budgetError = null
                        return@Button
                    }

                    val parsedBudget = budgetInput.parseUsdInput()
                    if (budgetInput.isNotBlank() && parsedBudget == null) {
                        budgetError = "Enter a positive dollar amount."
                        return@Button
                    }

                    if (apiKey.isNotBlank()) {
                        credentials.apiKey = apiKey
                    }
                    credentials.model = model
                    credentials.monthlyBudgetUsd = parsedBudget
                    monthlyBudgetUsd = parsedBudget
                    budgetInput = parsedBudget?.formatBudgetInput().orEmpty()
                    budgetError = null
                    connected = credentials.isConnected
                    apiKey = ""
                    editingConnection = !connected
                },
                enabled = enabled && (detailsLocked || connected || apiKey.isNotBlank()),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (detailsLocked) "Edit" else "Save")
            }
            OutlinedButton(
                onClick = {
                    credentials.clear()
                    apiKey = ""
                    model = credentials.model
                    monthlyBudgetUsd = null
                    budgetInput = ""
                    budgetError = null
                    models = emptyList()
                    connected = false
                    editingConnection = true
                },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
        }
        Text(
            text = "When you use the assistant your setup history and messages are sent to OpenAI.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AiUsageCard(
            usage = usage,
            loading = usageLoading,
            error = usageError,
            monthlyBudgetUsd = monthlyBudgetUsd
        )
        MonthlyBudgetEditor(
            value = budgetInput,
            error = budgetError,
            enabled = detailFieldsEnabled,
            onValueChange = {
                budgetInput = it
                budgetError = null
            }
        )
    }
}

@Composable
private fun AiUsageCard(
    usage: AiUsageSummary?,
    loading: Boolean,
    error: String?,
    monthlyBudgetUsd: Double?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Usage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "This month",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                loading -> Text(
                    text = "Loading usage...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                error != null -> Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                usage == null -> Text(
                    text = "Login to a local account to view usage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> AiUsageDetails(
                    usage = usage,
                    monthlyBudgetUsd = monthlyBudgetUsd
                )
            }
        }
    }
}

@Composable
private fun AiUsageDetails(
    usage: AiUsageSummary,
    monthlyBudgetUsd: Double?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UsageMetricRow(label = "Input", value = usage.inputTokens.formatTokens())
        UsageMetricRow(label = "Output", value = usage.outputTokens.formatTokens())
        UsageMetricRow(label = "Total", value = usage.totalTokens.formatTokens())
        UsageMetricRow(label = "Requests", value = usage.requestCount.formatTokens())

        val costText = usage.estimatedCostUsd?.formatUsd() ?: "Unavailable"
        UsageMetricRow(label = "Estimated cost", value = costText)

        if (monthlyBudgetUsd != null && usage.estimatedCostUsd != null) {
            val progress = (usage.estimatedCostUsd / monthlyBudgetUsd).coerceAtLeast(0.0)
            UsageBudgetProgress(
                progress = progress,
                costUsd = usage.estimatedCostUsd,
                monthlyBudgetUsd = monthlyBudgetUsd
            )
        } else if (monthlyBudgetUsd == null) {
            Text(
                text = "Set a monthly budget to show a progress bar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (usage.unpricedModelCount > 0) {
            Text(
                text = "Some models do not have local pricing, so cost progress is hidden.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Estimate uses local Suslog requests only; OpenAI billing may differ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UsageMetricRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun UsageBudgetProgress(
    progress: Double,
    costUsd: Double,
    monthlyBudgetUsd: Double,
) {
    val clamped = progress.coerceIn(0.0, 1.0).toFloat()
    val progressColor = when {
        progress >= 1.0 -> MaterialTheme.colorScheme.error
        progress >= 0.8 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${costUsd.formatUsd()} / ${monthlyBudgetUsd.formatUsd()}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(progress * 100.0).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .background(progressColor)
            )
        }
    }
}

@Composable
private fun MonthlyBudgetEditor(
    value: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Monthly budget (optional)") },
            placeholder = { Text("5.00") },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = error != null,
            supportingText = {
                Text(error ?: "USD budget for local Suslog AI usage.")
            }
        )
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
            activeAccountId = "preview-account",
            activeAccountEmail = "driver@suslog.local",
            carCount = 2,
            setupConfigCount = 5,
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
            onLogOff = {},
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

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun AiUsageCardPreview() {
    SuslogTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AiUsageCard(
                usage = previewAiUsageSummary(),
                loading = false,
                error = null,
                monthlyBudgetUsd = 5.00
            )
        }
    }
}
