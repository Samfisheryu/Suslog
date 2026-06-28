package com.example.suslog.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.suslog.domain.car.CarProfile
import com.example.suslog.ui.common.FitText
import com.example.suslog.domain.setup.SetupConfig
import com.example.suslog.domain.setup.SetupConfigFeedback
import com.example.suslog.domain.setup.SetupConfigState
import com.example.suslog.domain.setup.SetupState
import com.example.suslog.domain.setup.SetupStateMachine
import com.example.suslog.domain.setup.SetupValues
import com.example.suslog.domain.setup.defaultSetup
import com.example.suslog.domain.suspension.AdjusterSpec
import com.example.suslog.domain.suspension.Corner
import com.example.suslog.domain.suspension.StiffSide
import com.example.suslog.domain.suspension.SuspensionType
import com.example.suslog.domain.tuning.Axle
import com.example.suslog.domain.tuning.AxleSideMismatch
import com.example.suslog.domain.tuning.findAxleSideMismatches
import com.example.suslog.settings.SetupDefaults
import com.example.suslog.ui.common.BalanceSelector
import com.example.suslog.ui.common.ChoiceButton
import com.example.suslog.ui.common.LabeledScaleSelector
import com.example.suslog.ui.common.SectionHeader
import com.example.suslog.ui.common.SetupCornerCard
import com.example.suslog.ui.common.bodyControlLabel
import com.example.suslog.ui.common.formatLapTime
import com.example.suslog.ui.common.gripLabel
import com.example.suslog.ui.common.parseLapTimeMillis
import com.example.suslog.ui.theme.SuslogTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    cars: List<CarProfile>,
    setupStateMachinesByCar: Map<String, SetupStateMachine>,
    draftSetupByCar: Map<String, SetupValues>,
    setupConfigs: List<SetupConfig>,
    selectedConfigIdByCar: Map<String, String?>,
    activeConfigIdByCar: Map<String, String?>,
    selectedCarId: String?,
    setupRecommendation: SetupRecommendation?,
    setupDefaults: SetupDefaults,
    requireFeedbackBeforeExitConfig: Boolean,
    showPreviousFeedbackReference: Boolean,
    axleLockEnabled: Boolean,
    showDebugInfo: Boolean,
    showAddCar: Boolean,
    onShowAddCarChange: (Boolean) -> Unit,
    onSelectCar: (String) -> Unit,
    onSelectConfig: (String, String?) -> Unit,
    onEnterConfig: (String, String) -> Unit,
    onExitConfig: (String) -> Unit,
    onAddCar: (CarProfile) -> Unit,
    onUpdateCar: (CarProfile) -> Unit,
    onAdjustClick: (CarProfile, Corner, AdjusterSpec, Int) -> Unit,
    onClearDraft: (CarProfile) -> Unit,
    onSaveSetup: (CarProfile, SetupValues) -> Unit,
    onSaveConfigFeedback: (CarProfile, SetupValues, SetupConfigFeedback) -> Unit,
    onClearRecommendation: () -> Unit,
    onAxleLockEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCar = cars.firstOrNull { it.id == selectedCarId } ?: cars.firstOrNull()

    LaunchedEffect(cars.size, selectedCarId) {
        if (cars.isNotEmpty() && selectedCar == null) {
            onSelectCar(cars.first().id)
        }
    }

    if (showAddCar) {
        AddCarScreen(
            setupDefaults = setupDefaults,
            onAddCar = {
                onAddCar(it)
                onShowAddCarChange(false)
            },
            onCancel = { onShowAddCarChange(false) },
            modifier = modifier.fillMaxSize()
        )
    } else {
        val stateMachine = selectedCar?.let { setupStateMachinesByCar[it.id] }
        val currentState = stateMachine?.currentState
        val selectedConfig = selectedCar?.let { car ->
            val selectedConfigId = selectedConfigIdByCar[car.id]
            setupConfigs.firstOrNull { it.id == selectedConfigId && it.carId == car.id }
        }
        val activeConfig = selectedCar?.let { car ->
            val activeConfigId = activeConfigIdByCar[car.id]
            setupConfigs.firstOrNull { it.id == activeConfigId && it.carId == car.id }
        }

        CurrentSetupScreen(
            cars = cars,
            selectedCar = selectedCar,
            currentState = currentState,
            stateCount = stateMachine?.states?.size ?: 0,
            setup = selectedCar?.let { car ->
                draftSetupByCar[car.id]
                    ?: currentState?.setup
                    ?: car.defaultSetup()
            }.orEmpty(),
            setupConfigs = selectedCar?.let { car ->
                setupConfigs.filter { it.carId == car.id }
            }.orEmpty(),
            selectedConfig = selectedConfig,
            activeConfig = activeConfig,
            setupRecommendation = selectedCar?.let { car ->
                setupRecommendation?.takeIf { it.carId == car.id }
            },
            requireFeedbackBeforeExitConfig = requireFeedbackBeforeExitConfig,
            showPreviousFeedbackReference = showPreviousFeedbackReference,
            axleLockEnabled = axleLockEnabled,
            showDebugInfo = showDebugInfo,
            onSelectCar = onSelectCar,
            onUpdateCar = onUpdateCar,
            onSelectConfig = { configId ->
                selectedCar?.let { car -> onSelectConfig(car.id, configId) }
            },
            onEnterConfig = { configId ->
                selectedCar?.let { car -> onEnterConfig(car.id, configId) }
            },
            onExitConfig = {
                selectedCar?.let { car -> onExitConfig(car.id) }
            },
            onAdjustClick = { corner, adjuster, delta ->
                selectedCar?.let { car ->
                    onAdjustClick(car, corner, adjuster, delta)
                }
            },
            onClearDraft = {
                selectedCar?.let { car -> onClearDraft(car) }
            },
            onSaveSetup = { setup ->
                selectedCar?.let { car -> onSaveSetup(car, setup) }
            },
            onSaveConfigFeedback = { setup, feedback ->
                selectedCar?.let { car -> onSaveConfigFeedback(car, setup, feedback) }
            },
            onClearRecommendation = onClearRecommendation,
            onAxleLockEnabledChange = onAxleLockEnabledChange,
            modifier = modifier
        )
    }
}

@Composable
private fun CurrentSetupScreen(
    cars: List<CarProfile>,
    selectedCar: CarProfile?,
    currentState: SetupState?,
    stateCount: Int,
    setup: SetupValues,
    setupConfigs: List<SetupConfig>,
    selectedConfig: SetupConfig?,
    activeConfig: SetupConfig?,
    setupRecommendation: SetupRecommendation?,
    requireFeedbackBeforeExitConfig: Boolean,
    showPreviousFeedbackReference: Boolean,
    axleLockEnabled: Boolean,
    showDebugInfo: Boolean,
    onSelectCar: (String) -> Unit,
    onUpdateCar: (CarProfile) -> Unit,
    onSelectConfig: (String?) -> Unit,
    onEnterConfig: (String) -> Unit,
    onExitConfig: () -> Unit,
    onAdjustClick: (Corner, AdjusterSpec, Int) -> Unit,
    onClearDraft: () -> Unit,
    onSaveSetup: (SetupValues) -> Unit,
    onSaveConfigFeedback: (SetupValues, SetupConfigFeedback) -> Unit,
    onClearRecommendation: () -> Unit,
    onAxleLockEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controlsEnabled = selectedCar != null
    val hasDraftChanges = selectedCar != null && currentState != null && setup != currentState.setup
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val activeConfigState = activeConfig?.currentState
    val currentSetupHasFeedback = activeConfig != null &&
        activeConfigState?.setup == setup &&
        activeConfigState.hasFeedback
    val referenceFeedbackState = activeConfigState?.takeIf { state ->
        showPreviousFeedbackReference &&
        state.hasFeedback && state.setup != setup
    }
    var cornerEntryBalance by remember(activeConfig?.id, setup) {
        mutableStateOf(if (currentSetupHasFeedback) activeConfig?.cornerEntryBalance else null)
    }
    var cornerMidBalance by remember(activeConfig?.id, setup) {
        mutableStateOf(if (currentSetupHasFeedback) activeConfig?.cornerMidBalance else null)
    }
    var cornerExitBalance by remember(activeConfig?.id, setup) {
        mutableStateOf(if (currentSetupHasFeedback) activeConfig?.cornerExitBalance else null)
    }
    var overallGrip by remember(activeConfig?.id, setup) {
        mutableStateOf(if (currentSetupHasFeedback) activeConfig?.overallGrip else null)
    }
    var bodyControlBalance by remember(activeConfig?.id, setup) {
        mutableStateOf(if (currentSetupHasFeedback) activeConfig?.bodyControlBalance else null)
    }
    var lapTimeText by remember(activeConfig?.id, setup) {
        mutableStateOf(
            if (currentSetupHasFeedback) {
                activeConfig?.lapTimeMillis?.formatLapTime().orEmpty()
            } else {
                ""
            }
        )
    }
    var note by remember(activeConfig?.id, setup) {
        mutableStateOf(if (currentSetupHasFeedback) activeConfig?.currentState?.note.orEmpty() else "")
    }
    val parsedLapTime = parseLapTimeMillis(lapTimeText)
    val lapTimeIsValid = lapTimeText.trim().isEmpty() || parsedLapTime != null
    val feedbackHasContent = cornerEntryBalance != null ||
        cornerMidBalance != null ||
        cornerExitBalance != null ||
        overallGrip != null ||
        bodyControlBalance != null ||
        parsedLapTime != null ||
        note.trim().isNotEmpty()
    val feedbackIsComplete = cornerEntryBalance != null &&
        cornerMidBalance != null &&
        cornerExitBalance != null &&
        overallGrip != null &&
        bodyControlBalance != null
    val feedback = if (activeConfig != null && lapTimeIsValid && feedbackIsComplete) {
        SetupConfigFeedback(
            cornerEntryBalance = cornerEntryBalance,
            cornerMidBalance = cornerMidBalance,
            cornerExitBalance = cornerExitBalance,
            overallGrip = overallGrip,
            bodyControlBalance = bodyControlBalance,
            lapTimeMillis = parsedLapTime,
            note = note.trim().ifEmpty { null }
        )
    } else {
        null
    }
    val hasFeedbackChanges = activeConfig != null && feedback != null && (
        activeConfigState?.setup != setup ||
            activeConfigState?.hasFeedback != true ||
            feedback.cornerEntryBalance != activeConfig.cornerEntryBalance ||
            feedback.cornerMidBalance != activeConfig.cornerMidBalance ||
            feedback.cornerExitBalance != activeConfig.cornerExitBalance ||
            feedback.overallGrip != activeConfig.overallGrip ||
            feedback.bodyControlBalance != activeConfig.bodyControlBalance ||
            feedback.lapTimeMillis != activeConfig.lapTimeMillis ||
            feedback.note != activeConfig.currentState?.note
        )
    val shouldShowFeedbackWarning = activeConfig != null && !currentSetupHasFeedback
    val canSaveFeedback = activeConfig != null &&
        !hasDraftChanges &&
        lapTimeIsValid &&
        feedbackIsComplete &&
        (!currentSetupHasFeedback || hasFeedbackChanges)
    val branchConfig = activeConfig ?: selectedConfig
    val targetSetup = setupRecommendation?.setup ?: branchConfig?.setup
    val recommendationLabel = setupRecommendation?.stateLabel?.let { "Recommend $it" } ?: "Recommend"
    val axleSideMismatches = selectedCar?.let { car ->
        findAxleSideMismatches(car = car, setup = setup)
    }.orEmpty()

    fun showWarningAndScrollToFeedback() {
        scope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun handleAdjustClick(corner: Corner, adjuster: AdjusterSpec, delta: Int) {
        onAdjustClick(corner, adjuster, delta)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SetupTopBar(
            cars = cars,
            selectedCar = selectedCar,
            currentState = currentState,
            stateCount = stateCount,
            setupConfigs = setupConfigs,
            selectedConfig = selectedConfig,
            activeConfig = activeConfig,
            onSelectConfig = onSelectConfig,
            onEnterConfig = onEnterConfig,
            onExitConfig = {
                if (requireFeedbackBeforeExitConfig && activeConfig != null && !currentSetupHasFeedback) {
                    showWarningAndScrollToFeedback()
                } else {
                    onExitConfig()
                }
            },
            onSelectCar = onSelectCar,
            onUpdateCar = onUpdateCar
        )

        AxleLockToggle(
            checked = axleLockEnabled,
            enabled = controlsEnabled,
            onCheckedChange = onAxleLockEnabledChange
        )

        if (axleSideMismatches.isNotEmpty()) {
            AxleSideMismatchWarningBar(mismatches = axleSideMismatches)
        }

        if (shouldShowFeedbackWarning) {
            ConfigFeedbackWarningBar(
                onClick = { showWarningAndScrollToFeedback() }
            )
        }

        if (setupRecommendation != null) {
            SetupRecommendationBar(
                recommendation = setupRecommendation,
                onClear = onClearRecommendation
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        if (showDebugInfo) {
            ConfigStateDebugInfo(
                activeConfig = activeConfig,
                setup = setup,
                hasDraftChanges = hasDraftChanges,
                currentSetupHasFeedback = currentSetupHasFeedback,
                feedbackHasContent = feedbackHasContent,
                feedbackIsComplete = feedbackIsComplete,
                canSaveFeedback = canSaveFeedback,
                warningVisible = shouldShowFeedbackWarning
            )
        }

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
                car = selectedCar,
                values = setup[Corner.LEFT_FRONT].orEmpty(),
                targetValues = targetSetup?.get(Corner.LEFT_FRONT),
                onAdjustClick = ::handleAdjustClick,
                enabled = controlsEnabled,
                recommendationLabel = recommendationLabel,
                modifier = Modifier.weight(1f)
            )
            SetupCornerCard(
                corner = Corner.RIGHT_FRONT,
                car = selectedCar,
                values = setup[Corner.RIGHT_FRONT].orEmpty(),
                targetValues = targetSetup?.get(Corner.RIGHT_FRONT),
                onAdjustClick = ::handleAdjustClick,
                enabled = controlsEnabled,
                recommendationLabel = recommendationLabel,
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
                car = selectedCar,
                values = setup[Corner.LEFT_REAR].orEmpty(),
                targetValues = targetSetup?.get(Corner.LEFT_REAR),
                onAdjustClick = ::handleAdjustClick,
                enabled = controlsEnabled,
                recommendationLabel = recommendationLabel,
                modifier = Modifier.weight(1f)
            )
            SetupCornerCard(
                corner = Corner.RIGHT_REAR,
                car = selectedCar,
                values = setup[Corner.RIGHT_REAR].orEmpty(),
                targetValues = targetSetup?.get(Corner.RIGHT_REAR),
                onAdjustClick = ::handleAdjustClick,
                enabled = controlsEnabled,
                recommendationLabel = recommendationLabel,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onClearDraft,
                enabled = controlsEnabled && hasDraftChanges,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear Draft")
            }
            Button(
                onClick = { onSaveSetup(setup) },
                enabled = controlsEnabled && hasDraftChanges,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Current Setup")
            }
        }

        ConfigFeedbackPanel(
            activeConfig = activeConfig,
            referenceState = referenceFeedbackState,
            cornerEntryBalance = cornerEntryBalance,
            cornerMidBalance = cornerMidBalance,
            cornerExitBalance = cornerExitBalance,
            overallGrip = overallGrip,
            bodyControlBalance = bodyControlBalance,
            lapTimeText = lapTimeText,
            note = note,
            lapTimeIsValid = lapTimeIsValid,
            canSaveFeedback = canSaveFeedback,
            onCornerEntryBalanceChange = { cornerEntryBalance = it },
            onCornerMidBalanceChange = { cornerMidBalance = it },
            onCornerExitBalanceChange = { cornerExitBalance = it },
            onOverallGripChange = { overallGrip = it },
            onBodyControlBalanceChange = { bodyControlBalance = it },
            onLapTimeTextChange = { lapTimeText = it },
            onNoteChange = { note = it },
            onSaveFeedback = {
                feedback?.let {
                    onSaveConfigFeedback(setup, it)
                }
            }
        )
        }
    }
}

@Composable
private fun AxleSideMismatchWarningBar(
    mismatches: List<AxleSideMismatch>,
    modifier: Modifier = Modifier,
) {
    val first = mismatches.first()
    val mismatchCount = mismatches.size
    val detail = "${first.axle.label()} ${first.adjusterLabel}: L ${first.leftValue} / R ${first.rightValue}"
    val message = if (mismatchCount == 1) {
        "Left/right values differ. $detail"
    } else {
        "Left/right values differ in $mismatchCount places. $detail"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 3.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun Axle.label(): String =
    when (this) {
        Axle.FRONT -> "Front"
        Axle.REAR -> "Rear"
    }

@Composable
private fun AxleLockToggle(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FitText(
                    text = "Axle L/R Lock",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                FitText(
                    text = if (checked) "Matched" else "Independent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun ConfigStateDebugInfo(
    activeConfig: SetupConfig?,
    setup: SetupValues,
    hasDraftChanges: Boolean,
    currentSetupHasFeedback: Boolean,
    feedbackHasContent: Boolean,
    feedbackIsComplete: Boolean,
    canSaveFeedback: Boolean,
    warningVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Config State Debug",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Active config: ${activeConfig?.name ?: "none"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Current setup has config feedback: ${currentSetupHasFeedback.yesNo()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Latest config setup matches current setup: ${(activeConfig?.currentState?.setup == setup).yesNo()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Draft changed: ${hasDraftChanges.yesNo()} | Local feedback: ${feedbackHasContent.yesNo()} | Complete: ${feedbackIsComplete.yesNo()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Can save feedback: ${canSaveFeedback.yesNo()} | Warning visible: ${warningVisible.yesNo()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun Boolean.yesNo(): String =
    if (this) "yes" else "no"

@Composable
private fun ConfigFeedbackWarningBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 3.dp
    ) {
        Text(
            text = "If you want a full tuning state, share feedback for the current setup before adjusting.",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SetupRecommendationBar(
    recommendation: SetupRecommendation,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FitText(
                text = "Recommended: ${recommendation.configName} / ${recommendation.stateLabel}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onClear) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun ConfigFeedbackPanel(
    activeConfig: SetupConfig?,
    referenceState: SetupConfigState?,
    cornerEntryBalance: Int?,
    cornerMidBalance: Int?,
    cornerExitBalance: Int?,
    overallGrip: Int?,
    bodyControlBalance: Int?,
    lapTimeText: String,
    note: String,
    lapTimeIsValid: Boolean,
    canSaveFeedback: Boolean,
    onCornerEntryBalanceChange: (Int?) -> Unit,
    onCornerMidBalanceChange: (Int?) -> Unit,
    onCornerExitBalanceChange: (Int?) -> Unit,
    onOverallGripChange: (Int?) -> Unit,
    onBodyControlBalanceChange: (Int?) -> Unit,
    onLapTimeTextChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = activeConfig != null
    val referenceLapTime = referenceState?.lapTimeMillis?.formatLapTime()
    val referenceNote = referenceState?.note?.takeIf { it.isNotBlank() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Config Feedback",
                trailing = {
                    FitText(
                        text = activeConfig?.name ?: "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }
            )

            BalanceSelector(
                title = "Corner Entry",
                value = cornerEntryBalance,
                onValueChange = onCornerEntryBalanceChange,
                enabled = enabled,
                referenceValue = referenceState?.cornerEntryBalance
            )
            BalanceSelector(
                title = "Mid Corner",
                value = cornerMidBalance,
                onValueChange = onCornerMidBalanceChange,
                enabled = enabled,
                referenceValue = referenceState?.cornerMidBalance
            )
            BalanceSelector(
                title = "Corner Exit",
                value = cornerExitBalance,
                onValueChange = onCornerExitBalanceChange,
                enabled = enabled,
                referenceValue = referenceState?.cornerExitBalance
            )
            LabeledScaleSelector(
                title = "Overall Grip",
                value = overallGrip,
                options = (1..5).toList(),
                labelForValue = ::gripLabel,
                onValueChange = onOverallGripChange,
                enabled = enabled,
                referenceValue = referenceState?.overallGrip,
                startLabel = "Low",
                endLabel = "High"
            )
            LabeledScaleSelector(
                title = "Body Control",
                value = bodyControlBalance,
                options = (-2..2).toList(),
                labelForValue = ::bodyControlLabel,
                onValueChange = onBodyControlBalanceChange,
                enabled = enabled,
                referenceValue = referenceState?.bodyControlBalance,
                startLabel = "Too Stiff",
                endLabel = "Too Much Roll"
            )

            OutlinedTextField(
                value = lapTimeText,
                onValueChange = onLapTimeTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Lap Time") },
                placeholder = referenceLapTime?.let { { Text("Previous: $it") } },
                singleLine = true,
                enabled = enabled,
                isError = enabled && !lapTimeIsValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            if (enabled && !lapTimeIsValid) {
                Text(
                    text = "Use seconds or m:ss.SSS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note") },
                placeholder = referenceNote?.let { { Text("Previous: $it") } },
                minLines = 2,
                maxLines = 4,
                enabled = enabled
            )

            Button(
                onClick = onSaveFeedback,
                enabled = canSaveFeedback,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Feedback")
            }
        }
    }
}

private enum class CarDialogMode {
    DETAILS,
    CHANGE_CAR,
    EDIT,
}

@Composable
private fun SetupTopBar(
    cars: List<CarProfile>,
    selectedCar: CarProfile?,
    currentState: SetupState?,
    stateCount: Int,
    setupConfigs: List<SetupConfig>,
    selectedConfig: SetupConfig?,
    activeConfig: SetupConfig?,
    onSelectConfig: (String?) -> Unit,
    onEnterConfig: (String) -> Unit,
    onExitConfig: () -> Unit,
    onSelectCar: (String) -> Unit,
    onUpdateCar: (CarProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var configExpanded by remember { mutableStateOf(false) }
    var showCarDialog by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf(CarDialogMode.DETAILS) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val branchButtonLabel = when {
            activeConfig != null -> "Exit Config"
            selectedConfig != null -> "Enter Config"
            else -> "Current"
        }
        Button(
            onClick = {
                if (activeConfig != null) {
                    onExitConfig()
                } else {
                    selectedConfig?.let { onEnterConfig(it.id) }
                }
            },
            enabled = activeConfig != null || selectedConfig != null,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(branchButtonLabel)
        }

        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { configExpanded = true },
                enabled = selectedCar != null && activeConfig == null,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                FitText(
                    text = activeConfig?.name ?: selectedConfig?.name ?: "Current",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Text("v")
            }
            DropdownMenu(
                expanded = configExpanded,
                onDismissRequest = { configExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Current") },
                    onClick = {
                        onSelectConfig(null)
                        configExpanded = false
                    }
                )
                if (setupConfigs.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "No saved configs",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { configExpanded = false }
                    )
                } else {
                    setupConfigs.forEach { config ->
                        DropdownMenuItem(
                            text = { Text(config.name) },
                            onClick = {
                                onSelectConfig(config.id)
                                configExpanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                dialogMode = CarDialogMode.DETAILS
                showCarDialog = true
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            FitText(
                text = selectedCar?.name ?: "No Car",
                modifier = Modifier.widthIn(max = 92.dp)
            )
        }
    }

    if (showCarDialog) {
        CarDialog(
            cars = cars,
            selectedCar = selectedCar,
            currentState = currentState,
            stateCount = stateCount,
            mode = dialogMode,
            onModeChange = { dialogMode = it },
            onSelectCar = {
                onSelectCar(it)
                showCarDialog = false
            },
            onUpdateCar = {
                onUpdateCar(it)
                showCarDialog = false
            },
            onDismiss = { showCarDialog = false }
        )
    }
}

@Composable
private fun CarDialog(
    cars: List<CarProfile>,
    selectedCar: CarProfile?,
    currentState: SetupState?,
    stateCount: Int,
    mode: CarDialogMode,
    onModeChange: (CarDialogMode) -> Unit,
    onSelectCar: (String) -> Unit,
    onUpdateCar: (CarProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp
        ) {
            when (mode) {
                CarDialogMode.DETAILS -> CarDetailsDialogContent(
                    selectedCar = selectedCar,
                    currentState = currentState,
                    stateCount = stateCount,
                    onEdit = { onModeChange(CarDialogMode.EDIT) },
                    onChangeCar = { onModeChange(CarDialogMode.CHANGE_CAR) }
                )

                CarDialogMode.CHANGE_CAR -> ChangeCarDialogContent(
                    cars = cars,
                    selectedCar = selectedCar,
                    onSelectCar = onSelectCar,
                    onBack = { onModeChange(CarDialogMode.DETAILS) }
                )

                CarDialogMode.EDIT -> EditCarDialogContent(
                    selectedCar = selectedCar,
                    onUpdateCar = onUpdateCar,
                    onBack = { onModeChange(CarDialogMode.DETAILS) }
                )
            }
        }
    }
}

@Composable
private fun CarDetailsDialogContent(
    selectedCar: CarProfile?,
    currentState: SetupState?,
    stateCount: Int,
    onEdit: () -> Unit,
    onChangeCar: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = selectedCar?.name ?: "No Car",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (selectedCar == null) {
            Text(
                text = "Tap the center + button to add a car.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "${selectedCar.suspensionType.label} coilover",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            selectedCar.adjusters.forEach { adjuster ->
                Text(
                    text = "${adjuster.label}: ${adjuster.stiffSide.label(adjuster.maxClicks)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            Text(
                text = "Current state: ${currentState?.timestampMillis?.formatTimestamp() ?: "-"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "States recorded: $stateCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                enabled = selectedCar != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Edit")
            }
            Button(
                onClick = onChangeCar,
                enabled = carsCanChange(selectedCar),
                modifier = Modifier.weight(1f)
            ) {
                Text("Change Car")
            }
        }
    }
}

@Composable
private fun ChangeCarDialogContent(
    cars: List<CarProfile>,
    selectedCar: CarProfile?,
    onSelectCar: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Change Car",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (cars.isEmpty()) {
            Text(
                text = "No cars yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            cars.forEach { car ->
                OutlinedButton(
                    onClick = { onSelectCar(car.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = car.id != selectedCar?.id,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(car.name)
                        Text(
                            text = car.suspensionType.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (car.id == selectedCar?.id) {
                        Text("Current")
                    }
                }
            }
        }
        TextButton(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
private fun EditCarDialogContent(
    selectedCar: CarProfile?,
    onUpdateCar: (CarProfile) -> Unit,
    onBack: () -> Unit,
) {
    if (selectedCar == null) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Edit Car",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No car selected.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    } else {
        AddCarScreen(
            initialCar = selectedCar,
            onAddCar = onUpdateCar,
            onCancel = onBack
        )
    }
}

private fun carsCanChange(selectedCar: CarProfile?): Boolean =
    selectedCar != null

@Composable
private fun AddCarScreen(
    onAddCar: (CarProfile) -> Unit,
    setupDefaults: SetupDefaults = SetupDefaults(
        axleLockEnabled = true,
        suspensionType = SuspensionType.TWO_WAY,
        maxClicks = 30,
        stiffSide = StiffSide.HIGH_VALUE
    ),
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    initialCar: CarProfile? = null,
) {
    val initialAdjuster = initialCar?.adjusters?.firstOrNull()
    val isEditing = initialCar != null
    var carName by remember(initialCar?.id) { mutableStateOf(initialCar?.name.orEmpty()) }
    var suspensionType by remember(initialCar?.id) {
        mutableStateOf(initialCar?.suspensionType ?: setupDefaults.suspensionType)
    }
    var maxClicksText by remember(initialCar?.id) {
        mutableStateOf((initialAdjuster?.maxClicks ?: setupDefaults.maxClicks).toString())
    }
    var stiffSide by remember(initialCar?.id) {
        mutableStateOf(initialAdjuster?.stiffSide ?: setupDefaults.stiffSide)
    }

    val maxClicks = maxClicksText.toIntOrNull()
    val canAdd = carName.trim().isNotEmpty() && maxClicks != null && maxClicks in 1..99

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isEditing) "Edit Car" else "Add Car",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isEditing) {
                    "Update this suspension profile."
                } else {
                    "Create a suspension profile before tracking setup clicks."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = carName,
            onValueChange = { carName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Car name") },
            singleLine = true
        )

        FormSection(title = "Suspension type") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuspensionType.entries.forEach { type ->
                    ChoiceButton(
                        label = type.label,
                        selected = type == suspensionType,
                        onClick = { suspensionType = type },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        FormSection(title = "Click range") {
            OutlinedTextField(
                value = maxClicksText,
                onValueChange = { maxClicksText = it.filter(Char::isDigit).take(2) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Max click") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        FormSection(title = "Stiff side") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChoiceButton(
                    label = "1 is stiff",
                    selected = stiffSide == StiffSide.LOW_VALUE,
                    onClick = { stiffSide = StiffSide.LOW_VALUE },
                    modifier = Modifier.weight(1f)
                )
                ChoiceButton(
                    label = "${maxClicks ?: 30} is stiff",
                    selected = stiffSide == StiffSide.HIGH_VALUE,
                    onClick = { stiffSide = StiffSide.HIGH_VALUE },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        FormSection(title = "Adjusters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suspensionType.adjusterLabels.forEach { label ->
                    Text(
                        text = "$label · 1-${maxClicks ?: 30} · ${stiffSide.label(maxClicks ?: 30)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
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
                    val clickCount = maxClicks ?: return@Button
                    onAddCar(
                        CarProfile(
                            id = initialCar?.id ?: UUID.randomUUID().toString(),
                            name = carName.trim(),
                            suspensionType = suspensionType,
                            adjusters = suspensionType.adjusterLabels.map { label ->
                                AdjusterSpec(
                                    label = label,
                                    maxClicks = clickCount,
                                    stiffSide = stiffSide
                                )
                            }
                        )
                    )
                },
                enabled = canAdd,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isEditing) "Save Changes" else "Add Car")
            }
        }
    }
}

@Composable
private fun FormSection(
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

private fun Long.formatTimestamp(): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(this))

@Preview(showBackground = true)
@Composable
private fun CurrentSetupPreview() {
    val car = CarProfile(
        id = "preview",
        name = "E46 M3",
        suspensionType = SuspensionType.TWO_WAY,
        adjusters = listOf(
            AdjusterSpec("Rebound", 30, StiffSide.HIGH_VALUE),
            AdjusterSpec("Compression", 30, StiffSide.HIGH_VALUE)
        )
    )
    val state = SetupState(
        setup = car.defaultSetup(),
        timestampMillis = 1_767_222_000_000
    )
    val config = SetupConfig(
        id = "preview-config",
        carId = car.id,
        name = "Thunderbolt Dry",
        states = listOf(
            SetupConfigState(
                setup = state.setup.mapValues { (_, values) ->
                    values.mapValues { (_, value) -> (value + 2).coerceAtMost(30) }
                },
                cornerEntryBalance = -1,
                cornerExitBalance = 1,
                lapTimeMillis = 92_450,
                timestampMillis = 1_767_222_000_000
            )
        ),
        createdAtMillis = 1_767_222_000_000
    )

    SuslogTheme {
        CurrentSetupScreen(
            cars = listOf(car),
            selectedCar = car,
            currentState = state,
            stateCount = 1,
            setup = state.setup,
            setupConfigs = listOf(config),
            selectedConfig = config,
            activeConfig = config,
            setupRecommendation = null,
            requireFeedbackBeforeExitConfig = true,
            showPreviousFeedbackReference = true,
            axleLockEnabled = true,
            showDebugInfo = true,
            onSelectCar = {},
            onUpdateCar = {},
            onSelectConfig = {},
            onEnterConfig = {},
            onExitConfig = {},
            onAdjustClick = { _, _, _ -> },
            onClearDraft = {},
            onSaveSetup = {},
            onSaveConfigFeedback = { _, _ -> },
            onClearRecommendation = {},
            onAxleLockEnabledChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddCarPreview() {
    SuslogTheme {
        AddCarScreen(onAddCar = {})
    }
}
