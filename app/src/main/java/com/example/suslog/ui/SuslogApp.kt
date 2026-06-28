package com.example.suslog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.suslog.data.local.SuslogDatabase
import com.example.suslog.data.repository.SetupRepository
import com.example.suslog.domain.car.CarProfile
import com.example.suslog.domain.setup.SetupConfig
import com.example.suslog.domain.setup.SetupStateMachine
import com.example.suslog.domain.setup.SetupValues
import com.example.suslog.domain.setup.defaultSetup
import com.example.suslog.domain.setup.initialSetupStateMachine
import com.example.suslog.domain.setup.withAdjustedAxleMatchedClick
import com.example.suslog.domain.setup.withAdjustedClick
import com.example.suslog.domain.setup.defaultClick
import com.example.suslog.domain.suspension.AdjusterSpec
import com.example.suslog.domain.suspension.Corner
import com.example.suslog.domain.tuning.TuningDocument
import com.example.suslog.settings.AppSettingsStore
import com.example.suslog.settings.BiometricAuth
import com.example.suslog.settings.BiometricAuthStatus
import com.example.suslog.settings.SetupDefaults
import com.example.suslog.settings.TuningPreferences
import com.example.suslog.ui.config.ConfigScreen
import com.example.suslog.ui.navigation.AppBottomBar
import com.example.suslog.ui.navigation.AppDestination
import com.example.suslog.ui.settings.SettingsScreen
import com.example.suslog.ui.setup.SetupRecommendation
import com.example.suslog.ui.setup.SetupScreen
import com.example.suslog.ui.tuning.TuningScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SuslogApp(
    settingsStore: AppSettingsStore,
    biometricAuth: BiometricAuth,
) {
    val context = LocalContext.current
    val repository = remember {
        SetupRepository(SuslogDatabase.getInstance(context).suslogDao())
    }
    val scope = rememberCoroutineScope()
    val biometricStatus = remember { biometricAuth.status() }

    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.SETUP) }
    var appLockEnabled by remember { mutableStateOf(settingsStore.isAppLockEnabled()) }
    var appUnlocked by remember { mutableStateOf(!appLockEnabled) }
    var accountMessage by remember { mutableStateOf<String?>(null) }
    var cars by remember { mutableStateOf<List<CarProfile>>(emptyList()) }
    var setupStateMachinesByCar by remember {
        mutableStateOf<Map<String, SetupStateMachine>>(emptyMap())
    }
    var draftSetupByCar by remember {
        mutableStateOf<Map<String, SetupValues>>(emptyMap())
    }
    var setupConfigs by remember { mutableStateOf<List<SetupConfig>>(emptyList()) }
    var tuningDocuments by remember { mutableStateOf<List<TuningDocument>>(emptyList()) }
    var setupRecommendationByCar by remember {
        mutableStateOf<Map<String, SetupRecommendation>>(emptyMap())
    }
    var selectedConfigIdByCar by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var activeConfigIdByCar by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var selectedCarId by remember { mutableStateOf<String?>(null) }
    var showAddCar by remember { mutableStateOf(false) }
    var showAddConfig by remember { mutableStateOf(false) }
    var showAddDocument by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var axleLockEnabled by remember { mutableStateOf(settingsStore.isDefaultAxleLockEnabled()) }
    var defaultSuspensionType by remember { mutableStateOf(settingsStore.defaultSuspensionType()) }
    var defaultMaxClicks by remember { mutableStateOf(settingsStore.defaultMaxClicks()) }
    var defaultStiffSide by remember { mutableStateOf(settingsStore.defaultStiffSide()) }
    var requireFeedbackBeforeExitConfig by remember {
        mutableStateOf(settingsStore.requireFeedbackBeforeExitConfig())
    }
    var showPreviousFeedbackReference by remember {
        mutableStateOf(settingsStore.showPreviousFeedbackReference())
    }
    var showSetupDebugInfo by remember { mutableStateOf(settingsStore.showSetupDebugInfo()) }
    val setupDefaults = SetupDefaults(
        axleLockEnabled = axleLockEnabled,
        suspensionType = defaultSuspensionType,
        maxClicks = defaultMaxClicks,
        stiffSide = defaultStiffSide
    )
    val tuningPreferences = TuningPreferences(
        requireFeedbackBeforeExitConfig = requireFeedbackBeforeExitConfig,
        showPreviousFeedbackReference = showPreviousFeedbackReference,
        showSetupDebugInfo = showSetupDebugInfo
    )

    fun requestUnlock(
        title: String = "Unlock Suslog",
        subtitle: String = "Use device security to continue.",
        onSuccess: () -> Unit = {},
    ) {
        biometricAuth.authenticate(
            title = title,
            subtitle = subtitle,
            onSuccess = {
                appUnlocked = true
                accountMessage = "Verification succeeded."
                onSuccess()
            },
            onError = { error ->
                accountMessage = error
            }
        )
    }

    fun requestAppLockChange(enabled: Boolean) {
        if (enabled == appLockEnabled) return

        requestUnlock(
            title = if (enabled) "Enable App Lock" else "Turn Off App Lock",
            subtitle = "Verify with device security.",
            onSuccess = {
                settingsStore.setAppLockEnabled(enabled)
                appLockEnabled = enabled
                appUnlocked = true
                accountMessage = if (enabled) {
                    "App Lock is enabled."
                } else {
                    "App Lock is off."
                }
            }
        )
    }

    LaunchedEffect(repository) {
        val persisted = withContext(Dispatchers.IO) {
            repository.load()
        }

        cars = persisted.cars
        setupStateMachinesByCar = persisted.setupStateMachinesByCar
        draftSetupByCar = persisted.cars.associate { car ->
            val currentSetup = persisted.setupStateMachinesByCar[car.id]?.currentState?.setup
                ?: car.defaultSetup()
            car.id to currentSetup
        }
        setupConfigs = persisted.setupConfigs
        tuningDocuments = persisted.tuningDocuments
        selectedCarId = persisted.cars.firstOrNull()?.id
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (appUnlocked) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    plusExpanded = showCreateMenu,
                    onDestinationClick = {
                        currentDestination = it
                        showAddCar = false
                        showAddConfig = false
                        showAddDocument = false
                        showCreateMenu = false
                    },
                    onPlusClick = { showCreateMenu = !showCreateMenu }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentDestination) {
                AppDestination.SETUP -> SetupScreen(
                    cars = cars,
                    setupStateMachinesByCar = setupStateMachinesByCar,
                    draftSetupByCar = draftSetupByCar,
                    setupConfigs = setupConfigs,
                    selectedConfigIdByCar = selectedConfigIdByCar,
                    activeConfigIdByCar = activeConfigIdByCar,
                    selectedCarId = selectedCarId,
                    setupRecommendation = selectedCarId?.let { setupRecommendationByCar[it] },
                    setupDefaults = setupDefaults,
                    requireFeedbackBeforeExitConfig = requireFeedbackBeforeExitConfig,
                    showPreviousFeedbackReference = showPreviousFeedbackReference,
                    axleLockEnabled = axleLockEnabled,
                    showDebugInfo = showSetupDebugInfo,
                    showAddCar = showAddCar,
                    onShowAddCarChange = { showAddCar = it },
                    onSelectCar = { selectedCarId = it },
                    onSelectConfig = { carId, configId ->
                        selectedConfigIdByCar = selectedConfigIdByCar + (carId to configId)
                    },
                    onEnterConfig = { carId, configId ->
                        selectedConfigIdByCar = selectedConfigIdByCar + (carId to configId)
                        activeConfigIdByCar = activeConfigIdByCar + (carId to configId)
                    },
                    onExitConfig = { carId ->
                        selectedConfigIdByCar = selectedConfigIdByCar + (carId to null)
                        activeConfigIdByCar = activeConfigIdByCar + (carId to null)
                    },
                    onAddCar = { car ->
                        scope.launch {
                            val stateMachine = withContext(Dispatchers.IO) {
                                repository.addCar(car)
                            }
                            val currentSetup = requireNotNull(stateMachine.currentState).setup

                            cars = cars + car
                            setupStateMachinesByCar = setupStateMachinesByCar + (car.id to stateMachine)
                            draftSetupByCar = draftSetupByCar + (car.id to currentSetup)
                            selectedCarId = car.id
                            showAddCar = false
                        }
                    },
                    onUpdateCar = { updatedCar ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.updateCar(updatedCar)
                            }

                            cars = cars.map { car ->
                                if (car.id == updatedCar.id) updatedCar else car
                            }

                            val currentDraft = draftSetupByCar[updatedCar.id]
                                ?: setupStateMachinesByCar[updatedCar.id]?.currentState?.setup
                                ?: updatedCar.defaultSetup()
                            draftSetupByCar = draftSetupByCar + (
                                updatedCar.id to currentDraft.alignedTo(updatedCar)
                            )
                        }
                    },
                    onAdjustClick = { car, corner, adjuster, delta ->
                        val currentMachine = setupStateMachinesByCar[car.id]
                            ?: car.initialSetupStateMachine()
                        val currentDraft = draftSetupByCar[car.id]
                            ?: currentMachine.currentState?.setup
                            ?: car.defaultSetup()
                        val nextDraft = if (axleLockEnabled) {
                            currentDraft.withAdjustedAxleMatchedClick(corner, adjuster, delta)
                        } else {
                            currentDraft.withAdjustedClick(corner, adjuster, delta)
                        }

                        draftSetupByCar = draftSetupByCar + (car.id to nextDraft)
                    },
                    onClearDraft = { car ->
                        val currentSetup = setupStateMachinesByCar[car.id]?.currentState?.setup
                            ?: car.defaultSetup()
                        draftSetupByCar = draftSetupByCar + (car.id to currentSetup)
                    },
                    onSaveSetup = { car, setup ->
                        scope.launch {
                            val currentMachine = setupStateMachinesByCar[car.id]
                                ?: car.initialSetupStateMachine()
                            val currentSetup = currentMachine.currentState?.setup
                            val shouldRecordCurrentSetup = setup != currentSetup
                            val nextMachine = if (shouldRecordCurrentSetup) {
                                currentMachine.recordSnapshot(setup)
                            } else {
                                currentMachine
                            }
                            val nextState = nextMachine.currentState
                            withContext(Dispatchers.IO) {
                                if (shouldRecordCurrentSetup && nextState != null) {
                                    repository.appendSetupState(car.id, nextState)
                                }
                            }

                            setupStateMachinesByCar = setupStateMachinesByCar + (car.id to nextMachine)
                            draftSetupByCar = draftSetupByCar + (car.id to setup)
                        }
                    },
                    onSaveConfigFeedback = { car, setup, feedback ->
                        scope.launch {
                            val activeConfigId = activeConfigIdByCar[car.id]
                            val activeConfig = setupConfigs.firstOrNull {
                                it.id == activeConfigId && it.carId == car.id
                            } ?: return@launch
                            val updatedConfig = activeConfig.saveFeedback(
                                setup = setup,
                                feedback = feedback
                            )
                            val persistedConfig = withContext(Dispatchers.IO) {
                                repository.updateSetupConfig(updatedConfig)
                            }

                            setupConfigs = setupConfigs.map { config ->
                                if (config.id == persistedConfig.id) persistedConfig else config
                            }
                            selectedConfigIdByCar =
                                selectedConfigIdByCar + (persistedConfig.carId to persistedConfig.id)
                            activeConfigIdByCar =
                                activeConfigIdByCar + (persistedConfig.carId to persistedConfig.id)
                        }
                    },
                    onClearRecommendation = {
                        selectedCarId?.let { carId ->
                            setupRecommendationByCar = setupRecommendationByCar - carId
                        }
                    },
                    onAxleLockEnabledChange = {
                        axleLockEnabled = it
                        settingsStore.setDefaultAxleLockEnabled(it)
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestination.CONFIG -> ConfigScreen(
                    cars = cars,
                    selectedCarId = selectedCarId,
                    setupConfigs = setupConfigs,
                    baseSetupByCar = cars.associate { car ->
                        val currentSetup = setupStateMachinesByCar[car.id]?.currentState?.setup
                            ?: car.defaultSetup()
                        car.id to currentSetup
                    },
                    showAddConfig = showAddConfig,
                    onShowAddConfigChange = { showAddConfig = it },
                    onSelectCar = { selectedCarId = it },
                    onAddConfig = { config ->
                        scope.launch {
                            val persistedConfig = withContext(Dispatchers.IO) {
                                repository.addSetupConfig(config)
                            }

                            setupConfigs = setupConfigs + persistedConfig
                            selectedConfigIdByCar =
                                selectedConfigIdByCar + (persistedConfig.carId to persistedConfig.id)
                            showAddConfig = false
                        }
                    },
                    onUpdateConfig = { updatedConfig ->
                        scope.launch {
                            val persistedConfig = withContext(Dispatchers.IO) {
                                repository.updateSetupConfig(updatedConfig)
                            }

                            setupConfigs = setupConfigs.map { config ->
                                if (config.id == persistedConfig.id) persistedConfig else config
                            }
                            selectedConfigIdByCar =
                                selectedConfigIdByCar + (persistedConfig.carId to persistedConfig.id)
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestination.TUNING -> TuningScreen(
                    cars = cars,
                    selectedCarId = selectedCarId,
                    tuningDocuments = tuningDocuments,
                    setupConfigs = setupConfigs,
                    activeConfigId = selectedCarId?.let { activeConfigIdByCar[it] },
                    showAddDocument = showAddDocument,
                    onShowAddDocumentChange = { showAddDocument = it },
                    onSelectCar = { selectedCarId = it },
                    onAddDocument = { document ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.addTuningDocument(document)
                            }
                            tuningDocuments = listOf(document) + tuningDocuments
                        }
                    },
                    onApplyConfigState = { car, config, stateIndex, state ->
                        setupRecommendationByCar = setupRecommendationByCar + (
                            car.id to SetupRecommendation(
                                carId = car.id,
                                configId = config.id,
                                configName = config.name,
                                stateLabel = "S${stateIndex + 1}",
                                setup = state.setup
                            )
                        )
                        selectedCarId = car.id
                        selectedConfigIdByCar = selectedConfigIdByCar + (car.id to config.id)
                        currentDestination = AppDestination.SETUP
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestination.SETTINGS -> SettingsScreen(
                    appLockEnabled = appLockEnabled,
                    biometricStatus = biometricStatus,
                    accountMessage = accountMessage,
                    setupDefaults = setupDefaults,
                    tuningPreferences = tuningPreferences,
                    onAppLockChange = ::requestAppLockChange,
                    onUnlockNow = { requestUnlock() },
                    onDefaultAxleLockChange = {
                        axleLockEnabled = it
                        settingsStore.setDefaultAxleLockEnabled(it)
                    },
                    onDefaultSuspensionTypeChange = {
                        defaultSuspensionType = it
                        settingsStore.setDefaultSuspensionType(it)
                    },
                    onDefaultMaxClicksChange = {
                        defaultMaxClicks = it
                        settingsStore.setDefaultMaxClicks(it)
                    },
                    onDefaultStiffSideChange = {
                        defaultStiffSide = it
                        settingsStore.setDefaultStiffSide(it)
                    },
                    onRequireFeedbackBeforeExitConfigChange = {
                        requireFeedbackBeforeExitConfig = it
                        settingsStore.setRequireFeedbackBeforeExitConfig(it)
                    },
                    onShowPreviousFeedbackReferenceChange = {
                        showPreviousFeedbackReference = it
                        settingsStore.setShowPreviousFeedbackReference(it)
                    },
                    onShowSetupDebugInfoChange = {
                        showSetupDebugInfo = it
                        settingsStore.setShowSetupDebugInfo(it)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            if (showCreateMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showCreateMenu = false }
                )
            }

            AnimatedVisibility(
                visible = showCreateMenu,
                enter = fadeIn(animationSpec = tween(durationMillis = 140)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 180),
                        initialOffsetY = { it / 3 }
                    ) +
                    scaleIn(
                        animationSpec = tween(durationMillis = 180),
                        initialScale = 0.96f
                    ),
                exit = fadeOut(animationSpec = tween(durationMillis = 110)) +
                    slideOutVertically(
                        animationSpec = tween(durationMillis = 130),
                        targetOffsetY = { it / 3 }
                    ) +
                    scaleOut(
                        animationSpec = tween(durationMillis = 130),
                        targetScale = 0.96f
                    ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 84.dp
                    )
            ) {
                CreateActionMenu(
                    onAddCar = {
                        currentDestination = AppDestination.SETUP
                        showAddCar = true
                        showAddConfig = false
                        showAddDocument = false
                        showCreateMenu = false
                    },
                    onAddConfig = {
                        currentDestination = AppDestination.CONFIG
                        showAddCar = false
                        showAddConfig = true
                        showAddDocument = false
                        showCreateMenu = false
                    },
                    onAddDocument = {
                        currentDestination = AppDestination.TUNING
                        showAddCar = false
                        showAddConfig = false
                        showAddDocument = true
                        showCreateMenu = false
                    }
                )
            }

            if (!appUnlocked) {
                AppLockOverlay(
                    biometricStatus = biometricStatus,
                    accountMessage = accountMessage,
                    onUnlock = { requestUnlock() }
                )
            }
        }
    }
}

@Composable
private fun AppLockOverlay(
    biometricStatus: BiometricAuthStatus,
    accountMessage: String?,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Suslog Locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (biometricStatus == BiometricAuthStatus.AVAILABLE) {
                    "Use device security to unlock your setup data."
                } else {
                    "Device security is unavailable on this device."
                },
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            accountMessage?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onUnlock,
                enabled = biometricStatus == BiometricAuthStatus.AVAILABLE,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
            ) {
                Text("Unlock")
            }
        }
    }
}

@Composable
private fun CreateActionMenu(
    onAddCar: () -> Unit,
    onAddConfig: () -> Unit,
    onAddDocument: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CreateActionButton(
            label = "Add Document",
            onClick = onAddDocument
        )
        CreateActionButton(
            label = "Add Config",
            onClick = onAddConfig
        )
        CreateActionButton(
            label = "Add Car",
            onClick = onAddCar
        )
    }
}

@Composable
private fun CreateActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun SetupValues.alignedTo(car: CarProfile): SetupValues =
    Corner.entries.associateWith { corner ->
        car.adjusters.associate { adjuster ->
            val existingValue = this[corner]?.get(adjuster.label)
            adjuster.label to (existingValue ?: adjuster.defaultClick())
        }
    }
