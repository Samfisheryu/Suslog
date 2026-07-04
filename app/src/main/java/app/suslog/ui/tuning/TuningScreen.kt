package app.suslog.ui.tuning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.suslog.domain.car.CarProfile
import app.suslog.domain.setup.SetupConfig
import app.suslog.domain.setup.SetupConfigState
import app.suslog.domain.setup.defaultSetup
import app.suslog.domain.suspension.AdjusterSpec
import app.suslog.domain.suspension.StiffSide
import app.suslog.domain.suspension.SuspensionType
import app.suslog.domain.tuning.TuningDocument
import app.suslog.ui.common.FitText
import app.suslog.ui.setup.SetupRecommendation
import app.suslog.ui.theme.SuslogTheme
import java.util.UUID

private enum class TuningSection(
    val label: String,
) {
    DOCS("Tuning Docs"),
    CONFIGS("Setup Configs"),
}

@Composable
fun TuningScreen(
    cars: List<CarProfile>,
    selectedCarId: String?,
    localUserId: String?,
    tuningDocuments: List<TuningDocument>,
    setupConfigs: List<SetupConfig>,
    activeConfigId: String?,
    showAddDocument: Boolean,
    onShowAddDocumentChange: (Boolean) -> Unit,
    onSelectCar: (String) -> Unit,
    onAddDocument: (TuningDocument) -> Unit,
    onApplyConfigState: (CarProfile, SetupConfig, Int, SetupConfigState) -> Unit,
    onApplyAiRecommendation: (SetupRecommendation) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSection by rememberSaveable { mutableStateOf(TuningSection.DOCS) }
    var openedConfigId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCar = cars.firstOrNull { it.id == selectedCarId } ?: cars.firstOrNull()
    val carDocuments = selectedCar?.let { car ->
        tuningDocuments.filter { it.carId == car.id }
    }.orEmpty()
    val carConfigs = selectedCar?.let { car ->
        setupConfigs.filter { it.carId == car.id }
    }.orEmpty()
    val openedConfig = carConfigs.firstOrNull { it.id == openedConfigId }

    LaunchedEffect(cars.size, selectedCarId) {
        if (cars.isNotEmpty() && selectedCar == null) {
            onSelectCar(cars.first().id)
        }
    }

    LaunchedEffect(showAddDocument) {
        if (showAddDocument) {
            selectedSection = TuningSection.DOCS
            openedConfigId = null
        }
    }

    LaunchedEffect(activeConfigId, selectedCar?.id) {
        if (activeConfigId != null && carConfigs.any { it.id == activeConfigId }) {
            openedConfigId = activeConfigId
        } else if (openedConfigId != null && carConfigs.none { it.id == openedConfigId }) {
            openedConfigId = null
        }
    }

    if (selectedCar != null && openedConfig != null) {
        TuningHistoryScreen(
            car = selectedCar,
            config = openedConfig,
            userTuningDocuments = carDocuments,
            localUserId = localUserId,
            isActiveConfig = openedConfig.id == activeConfigId,
            onBack = { openedConfigId = null },
            onApplyState = { stateIndex, state ->
                onApplyConfigState(selectedCar, openedConfig, stateIndex, state)
            },
            onApplyAiRecommendation = onApplyAiRecommendation,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TuningCarSelector(
            cars = cars,
            selectedCar = selectedCar,
            onSelectCar = {
                onSelectCar(it)
                onShowAddDocumentChange(false)
                openedConfigId = null
            }
        )

        TuningSectionNavigator(
            selectedSection = selectedSection,
            onSelectSection = {
                selectedSection = it
                if (it != TuningSection.DOCS) {
                    onShowAddDocumentChange(false)
                }
            }
        )

        if (showAddDocument) {
            AddDocumentForm(
                selectedCar = selectedCar,
                onCancel = { onShowAddDocumentChange(false) },
                onSave = { document ->
                    onAddDocument(document)
                    onShowAddDocumentChange(false)
                }
            )
        }

        when (selectedSection) {
            TuningSection.DOCS -> TuningNameList(
                items = carDocuments.map { it.name },
                emptyTitle = if (selectedCar == null) "No Car" else "No tuning docs yet",
                modifier = Modifier.weight(1f)
            )

            TuningSection.CONFIGS -> TuningNameList(
                items = carConfigs.map { it.name },
                emptyTitle = if (selectedCar == null) "No Car" else "No setup configs yet",
                onItemClick = { index ->
                    openedConfigId = carConfigs.getOrNull(index)?.id
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TuningCarSelector(
    cars: List<CarProfile>,
    selectedCar: CarProfile?,
    onSelectCar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var carExpanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { carExpanded = true },
            enabled = cars.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            FitText(
                text = selectedCar?.name ?: "No Car",
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
private fun TuningSectionNavigator(
    selectedSection: TuningSection,
    onSelectSection: (TuningSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TuningSection.entries.forEach { section ->
            if (section == selectedSection) {
                Button(
                    onClick = { onSelectSection(section) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    FitText(text = section.label)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelectSection(section) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    FitText(text = section.label)
                }
            }
        }
    }
}

@Composable
private fun AddDocumentForm(
    selectedCar: CarProfile?,
    onCancel: () -> Unit,
    onSave: (TuningDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(selectedCar?.id) { mutableStateOf("") }
    var content by rememberSaveable(selectedCar?.id) { mutableStateOf("") }
    val canSave = selectedCar != null &&
        name.trim().isNotEmpty() &&
        content.trim().isNotEmpty()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add Document",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Document Name") },
                enabled = selectedCar != null,
                singleLine = true
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Document Content") },
                enabled = selectedCar != null,
                minLines = 6,
                placeholder = {
                    Text("Paste tuning notes, damper guide excerpts, or your own setup rules.")
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val car = selectedCar ?: return@Button
                        onSave(
                            TuningDocument(
                                id = UUID.randomUUID().toString(),
                                carId = car.id,
                                name = name.trim(),
                                content = content.trim(),
                                createdAtMillis = System.currentTimeMillis()
                            )
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun TuningNameList(
    items: List<String>,
    emptyTitle: String,
    modifier: Modifier = Modifier,
    onItemClick: ((Int) -> Unit)? = null,
) {
    if (items.isEmpty()) {
        TuningEmptyState(
            title = emptyTitle,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items.size) { index ->
            TuningNameRow(
                name = items[index],
                onClick = onItemClick?.let { { it(index) } }
            )
        }
    }
}

@Composable
private fun TuningNameRow(
    name: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Surface(
        modifier = rowModifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        FitText(
            text = name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TuningEmptyState(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TuningScreenPreview() {
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
                cornerEntryBalance = 0,
                cornerExitBalance = 0,
                lapTimeMillis = null,
                timestampMillis = 1_767_222_000_000
            )
        ),
        createdAtMillis = 1_767_222_000_000
    )
    val document = TuningDocument(
        id = "preview-doc",
        carId = car.id,
        name = "KW 2-Way Adjustment Notes",
        content = "Front rebound changes mostly affect entry response on this car.",
        createdAtMillis = 1_767_222_000_000
    )

    SuslogTheme {
        TuningScreen(
            cars = listOf(car),
            selectedCarId = car.id,
            localUserId = "preview-account",
            tuningDocuments = listOf(document),
            setupConfigs = listOf(config),
            activeConfigId = null,
            showAddDocument = false,
            onShowAddDocumentChange = {},
            onSelectCar = {},
            onAddDocument = {},
            onApplyConfigState = { _, _, _, _ -> },
            onApplyAiRecommendation = {}
        )
    }
}
