package app.suslog.ui.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.suslog.domain.car.CarProfile
import app.suslog.ui.common.FitText
import app.suslog.domain.setup.SetupConfig
import app.suslog.domain.setup.SetupConfigState
import app.suslog.domain.setup.SetupValues
import app.suslog.domain.setup.defaultClick
import app.suslog.domain.setup.defaultSetup
import app.suslog.domain.setup.withAdjustedClick
import app.suslog.domain.suspension.AdjusterSpec
import app.suslog.domain.suspension.Corner
import app.suslog.domain.suspension.StiffSide
import app.suslog.domain.suspension.SuspensionType
import app.suslog.ui.common.BalanceSelector
import app.suslog.ui.common.LabeledScaleSelector
import app.suslog.ui.common.SetupCornerCard
import app.suslog.ui.common.balanceLabel
import app.suslog.ui.common.bodyControlLabel
import app.suslog.ui.common.formatLapTime
import app.suslog.ui.common.gripLabel
import app.suslog.ui.common.parseLapTimeMillis
import app.suslog.ui.theme.SuslogTheme
import java.util.UUID

@Composable
fun ConfigScreen(
    cars: List<CarProfile>,
    selectedCarId: String?,
    setupConfigs: List<SetupConfig>,
    baseSetupByCar: Map<String, SetupValues>,
    showAddConfig: Boolean,
    onShowAddConfigChange: (Boolean) -> Unit,
    onSelectCar: (String) -> Unit,
    onAddConfig: (SetupConfig) -> Unit,
    onUpdateConfig: (SetupConfig) -> Unit,
    onDeleteConfig: (SetupConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingConfigId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteConfig by remember { mutableStateOf<SetupConfig?>(null) }
    val selectedCar = cars.firstOrNull { it.id == selectedCarId } ?: cars.firstOrNull()
    val carConfigs = selectedCar?.let { car ->
        setupConfigs.filter { it.carId == car.id }
    }.orEmpty()
    val editingConfig = carConfigs.firstOrNull { it.id == editingConfigId }

    LaunchedEffect(cars.size, selectedCarId) {
        if (cars.isNotEmpty() && selectedCar == null) {
            onSelectCar(cars.first().id)
        }
    }

    LaunchedEffect(selectedCar?.id, showAddConfig) {
        if (showAddConfig || editingConfig?.carId != selectedCar?.id) {
            editingConfigId = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConfigTopNavigator(
            cars = cars,
            selectedCar = selectedCar,
            onSelectCar = {
                onSelectCar(it)
                editingConfigId = null
                onShowAddConfigChange(false)
            }
        )

        when {
            selectedCar == null -> ConfigEmptyState(
                title = "No Car",
                body = "Add a car before saving setup configs."
            )

            showAddConfig -> ConfigEditor(
                car = selectedCar,
                initialConfig = null,
                initialSetup = baseSetupByCar[selectedCar.id] ?: selectedCar.defaultSetup(),
                onCancel = { onShowAddConfigChange(false) },
                onSave = onAddConfig
            )

            editingConfig != null -> ConfigEditor(
                car = selectedCar,
                initialConfig = editingConfig,
                initialSetup = editingConfig.setup,
                onCancel = { editingConfigId = null },
                onSave = { updatedConfig ->
                    onUpdateConfig(updatedConfig)
                    editingConfigId = null
                }
            )

            else -> ConfigSummaryList(
                car = selectedCar,
                setupConfigs = carConfigs,
                onCreateConfig = { onShowAddConfigChange(true) },
                onEditConfig = { editingConfigId = it.id },
                onDeleteConfig = { pendingDeleteConfig = it }
            )
        }
    }

    pendingDeleteConfig?.let { config ->
        AlertDialog(
            onDismissRequest = { pendingDeleteConfig = null },
            title = { Text("Delete Config") },
            text = {
                Text("Delete \"${config.name}\"? This will remove its setup history from this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConfig(config)
                        pendingDeleteConfig = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeleteConfig = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ConfigTopNavigator(
    cars: List<CarProfile>,
    selectedCar: CarProfile?,
    onSelectCar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var carExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { carExpanded = true },
            enabled = cars.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            FitText(
                text = selectedCar?.name ?: "Car",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Text("v")
        }
        DropdownMenu(
            expanded = carExpanded,
            onDismissRequest = { carExpanded = false }
        ) {
            cars.forEach { car ->
                DropdownMenuItem(
                    text = { Text(car.name) },
                    onClick = {
                        onSelectCar(car.id)
                        carExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ConfigSummaryList(
    car: CarProfile,
    setupConfigs: List<SetupConfig>,
    onCreateConfig: () -> Unit,
    onEditConfig: (SetupConfig) -> Unit,
    onDeleteConfig: (SetupConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (setupConfigs.isEmpty()) {
        ConfigEmptyState(
            title = "No Config",
            body = "Save this car's current setup as a reusable setup config.",
            actionLabel = "Create Config",
            onAction = onCreateConfig,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setup Configs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = onCreateConfig,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Create")
            }
        }

        setupConfigs.forEach { config ->
            SetupConfigSummaryCard(
                car = car,
                config = config,
                onEdit = { onEditConfig(config) },
                onDelete = { onDeleteConfig(config) }
            )
        }
    }
}

@Composable
private fun SetupConfigSummaryCard(
    car: CarProfile,
    config: SetupConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FitText(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = config.lapTimeMillis?.formatLapTime() ?: "No lap time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (config.hasFeedback) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    BalanceSummary(
                        title = "Entry",
                        value = config.cornerEntryBalance,
                        modifier = Modifier.weight(1f)
                    )
                    BalanceSummary(
                        title = "Exit",
                        value = config.cornerExitBalance,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "Feedback not shared",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SetupSnapshotSummary(
                car = car,
                setup = config.setup
            )
        }
    }
}

@Composable
private fun BalanceSummary(
    title: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    FitText(
        text = "$title: ${balanceLabel(value)}",
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SetupSnapshotSummary(
    car: CarProfile,
    setup: SetupValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Setup",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CornerSetupSummary(
                corner = Corner.LEFT_FRONT,
                car = car,
                values = setup[Corner.LEFT_FRONT].orEmpty(),
                modifier = Modifier.weight(1f)
            )
            CornerSetupSummary(
                corner = Corner.RIGHT_FRONT,
                car = car,
                values = setup[Corner.RIGHT_FRONT].orEmpty(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CornerSetupSummary(
                corner = Corner.LEFT_REAR,
                car = car,
                values = setup[Corner.LEFT_REAR].orEmpty(),
                modifier = Modifier.weight(1f)
            )
            CornerSetupSummary(
                corner = Corner.RIGHT_REAR,
                car = car,
                values = setup[Corner.RIGHT_REAR].orEmpty(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CornerSetupSummary(
    corner: Corner,
    car: CarProfile,
    values: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = corner.shortLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        car.adjusters.forEach { adjuster ->
            FitText(
                text = "${adjuster.label}: ${values[adjuster.label] ?: adjuster.defaultClick()}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConfigEditor(
    car: CarProfile,
    initialConfig: SetupConfig?,
    initialSetup: SetupValues,
    onCancel: (() -> Unit)?,
    onSave: (SetupConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditing = initialConfig != null
    var name by remember(car.id, initialConfig?.id) {
        mutableStateOf(initialConfig?.name.orEmpty())
    }
    var setup by remember(car.id, initialConfig?.id, initialSetup) {
        mutableStateOf((initialConfig?.setup ?: initialSetup).alignedTo(car))
    }
    var cornerEntryBalance by remember(car.id, initialConfig?.id) {
        mutableStateOf(if (initialConfig?.hasFeedback == true) initialConfig.cornerEntryBalance else null)
    }
    var cornerMidBalance by remember(car.id, initialConfig?.id) {
        mutableStateOf(if (initialConfig?.hasFeedback == true) initialConfig.cornerMidBalance else null)
    }
    var cornerExitBalance by remember(car.id, initialConfig?.id) {
        mutableStateOf(if (initialConfig?.hasFeedback == true) initialConfig.cornerExitBalance else null)
    }
    var overallGrip by remember(car.id, initialConfig?.id) {
        mutableStateOf(if (initialConfig?.hasFeedback == true) initialConfig.overallGrip else null)
    }
    var bodyControlBalance by remember(car.id, initialConfig?.id) {
        mutableStateOf(if (initialConfig?.hasFeedback == true) initialConfig.bodyControlBalance else null)
    }
    var lapTimeText by remember(car.id, initialConfig?.id) {
        mutableStateOf(initialConfig?.lapTimeMillis?.formatLapTime().orEmpty())
    }
    var note by remember(car.id, initialConfig?.id) {
        mutableStateOf(initialConfig?.currentState?.note.orEmpty())
    }

    val parsedLapTime = parseLapTimeMillis(lapTimeText)
    val lapTimeIsValid = lapTimeText.trim().isEmpty() || parsedLapTime != null
    val feedbackCoreHasContent = cornerEntryBalance != null ||
        cornerMidBalance != null ||
        cornerExitBalance != null ||
        overallGrip != null ||
        bodyControlBalance != null
    val feedbackIsComplete = cornerEntryBalance != null &&
        cornerMidBalance != null &&
        cornerExitBalance != null &&
        overallGrip != null &&
        bodyControlBalance != null
    val canSave = name.trim().isNotEmpty() &&
        lapTimeIsValid &&
        (!feedbackCoreHasContent || feedbackIsComplete)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isEditing) "Setup Config" else "Add Config",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = car.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Config Name") },
            singleLine = true
        )

        ConfigSection(title = "Setup") {
            Text(
                text = "Saved snapshot for this track or street baseline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "FRONT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupCornerCard(
                    corner = Corner.LEFT_FRONT,
                    car = car,
                    values = setup[Corner.LEFT_FRONT].orEmpty(),
                    targetValues = null,
                    enabled = true,
                    onAdjustClick = { corner, adjuster, delta ->
                        setup = setup.withAdjustedClick(corner, adjuster, delta)
                    },
                    modifier = Modifier.weight(1f)
                )
                SetupCornerCard(
                    corner = Corner.RIGHT_FRONT,
                    car = car,
                    values = setup[Corner.RIGHT_FRONT].orEmpty(),
                    targetValues = null,
                    enabled = true,
                    onAdjustClick = { corner, adjuster, delta ->
                        setup = setup.withAdjustedClick(corner, adjuster, delta)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "REAR",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupCornerCard(
                    corner = Corner.LEFT_REAR,
                    car = car,
                    values = setup[Corner.LEFT_REAR].orEmpty(),
                    targetValues = null,
                    enabled = true,
                    onAdjustClick = { corner, adjuster, delta ->
                        setup = setup.withAdjustedClick(corner, adjuster, delta)
                    },
                    modifier = Modifier.weight(1f)
                )
                SetupCornerCard(
                    corner = Corner.RIGHT_REAR,
                    car = car,
                    values = setup[Corner.RIGHT_REAR].orEmpty(),
                    targetValues = null,
                    enabled = true,
                    onAdjustClick = { corner, adjuster, delta ->
                        setup = setup.withAdjustedClick(corner, adjuster, delta)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ConfigSection(title = "Feedback (Optional)") {
            BalanceSelector(
                title = "Corner Entry",
                value = cornerEntryBalance,
                onValueChange = { cornerEntryBalance = it }
            )
            BalanceSelector(
                title = "Mid Corner",
                value = cornerMidBalance,
                onValueChange = { cornerMidBalance = it }
            )
            BalanceSelector(
                title = "Corner Exit",
                value = cornerExitBalance,
                onValueChange = { cornerExitBalance = it }
            )
            LabeledScaleSelector(
                title = "Overall Grip",
                value = overallGrip,
                options = (1..5).toList(),
                labelForValue = ::gripLabel,
                onValueChange = { overallGrip = it },
                startLabel = "Low",
                endLabel = "High"
            )
            LabeledScaleSelector(
                title = "Body Control",
                value = bodyControlBalance,
                options = (-2..2).toList(),
                labelForValue = ::bodyControlLabel,
                onValueChange = { bodyControlBalance = it },
                startLabel = "Too Stiff",
                endLabel = "Too Much Roll"
            )
            if (feedbackCoreHasContent && !feedbackIsComplete) {
                Text(
                    text = "Complete all five feedback fields, or clear them to save setup only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        ConfigSection(title = "Lap Time") {
            OutlinedTextField(
                value = lapTimeText,
                onValueChange = { lapTimeText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Optional") },
                singleLine = true,
                isError = !lapTimeIsValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            if (!lapTimeIsValid) {
                Text(
                    text = "Use seconds or m:ss.SSS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        ConfigSection(title = "Note") {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Optional") },
                minLines = 2,
                maxLines = 4
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onCancel != null) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val shouldSaveFeedback = feedbackIsComplete
                    val nextState = SetupConfigState(
                        setup = setup.alignedTo(car),
                        cornerEntryBalance = cornerEntryBalance ?: 0,
                        cornerMidBalance = cornerMidBalance ?: 0,
                        cornerExitBalance = cornerExitBalance ?: 0,
                        overallGrip = overallGrip ?: 3,
                        bodyControlBalance = bodyControlBalance ?: 0,
                        lapTimeMillis = parsedLapTime,
                        timestampMillis = now,
                        note = note.trim().ifEmpty { null },
                        hasFeedback = shouldSaveFeedback
                    )
                    onSave(
                        initialConfig?.recordSnapshot(
                            state = nextState,
                            name = name.trim()
                        ) ?: SetupConfig(
                            id = UUID.randomUUID().toString(),
                            carId = car.id,
                            name = name.trim(),
                            states = listOf(nextState),
                            createdAtMillis = now
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isEditing) "Save Changes" else "Save Config")
            }
        }
    }
}

@Composable
private fun ConfigSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun ConfigEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

private fun Corner.shortLabel(): String =
    when (this) {
        Corner.LEFT_FRONT -> "LF"
        Corner.RIGHT_FRONT -> "RF"
        Corner.LEFT_REAR -> "LR"
        Corner.RIGHT_REAR -> "RR"
    }

private fun SetupValues.alignedTo(car: CarProfile): SetupValues =
    Corner.entries.associateWith { corner ->
        car.adjusters.associate { adjuster ->
            val existingValue = this[corner]?.get(adjuster.label)
            adjuster.label to (existingValue ?: adjuster.defaultClick())
        }
    }

@Preview(showBackground = true)
@Composable
private fun ConfigScreenPreview() {
    val car = CarProfile(
        id = "preview",
        name = "E46 M3",
        suspensionType = SuspensionType.TWO_WAY,
        adjusters = listOf(
            AdjusterSpec("Rebound", 30, StiffSide.HIGH_VALUE),
            AdjusterSpec("Compression", 30, StiffSide.HIGH_VALUE)
        )
    )
    val config = SetupConfig(
        id = "preview-config",
        carId = car.id,
        name = "Thunderbolt Dry",
        states = listOf(
            SetupConfigState(
                setup = car.defaultSetup(),
                cornerEntryBalance = -1,
                cornerExitBalance = 1,
                lapTimeMillis = 92_450,
                timestampMillis = 1_767_222_000_000
            )
        ),
        createdAtMillis = 1_767_222_000_000
    )

    SuslogTheme {
        ConfigScreen(
            cars = listOf(car),
            selectedCarId = car.id,
            setupConfigs = listOf(config),
            baseSetupByCar = mapOf(car.id to car.defaultSetup()),
            showAddConfig = false,
            onShowAddConfigChange = {},
            onSelectCar = {},
            onAddConfig = {},
            onUpdateConfig = {},
            onDeleteConfig = {}
        )
    }
}
