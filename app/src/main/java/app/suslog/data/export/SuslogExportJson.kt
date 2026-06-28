package app.suslog.data.export

import app.suslog.domain.car.CarProfile
import app.suslog.domain.setup.SetupConfig
import app.suslog.domain.setup.SetupConfigState
import app.suslog.domain.setup.SetupState
import app.suslog.domain.setup.SetupStateMachine
import app.suslog.domain.setup.SetupValues
import app.suslog.domain.suspension.AdjusterSpec
import app.suslog.domain.suspension.Corner
import app.suslog.domain.tuning.TuningDocument
import app.suslog.settings.SetupDefaults
import app.suslog.settings.TuningPreferences
import org.json.JSONArray
import org.json.JSONObject

object SuslogExportJson {
    private const val SCHEMA_VERSION = 1

    fun build(
        cars: List<CarProfile>,
        setupStateMachinesByCar: Map<String, SetupStateMachine>,
        setupConfigs: List<SetupConfig>,
        tuningDocuments: List<TuningDocument>,
        setupDefaults: SetupDefaults,
        tuningPreferences: TuningPreferences,
        exportedAtMillis: Long = System.currentTimeMillis(),
    ): String {
        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put(
                "app",
                JSONObject()
                    .put("name", "Suslog")
                    .put("exportedAtMillis", exportedAtMillis)
            )
            .put("settings", settingsToJson(setupDefaults, tuningPreferences))
            .put("cars", JSONArray(cars.map(::carToJson)))
            .put(
                "setupStateMachines",
                JSONArray(
                    cars.map { car ->
                        setupStateMachineToJson(
                            carId = car.id,
                            stateMachine = setupStateMachinesByCar[car.id]
                        )
                    }
                )
            )
            .put("setupConfigs", JSONArray(setupConfigs.map(::setupConfigToJson)))
            .put("tuningDocuments", JSONArray(tuningDocuments.map(::tuningDocumentToJson)))

        return root.toString(2)
    }

    private fun settingsToJson(
        setupDefaults: SetupDefaults,
        tuningPreferences: TuningPreferences,
    ): JSONObject =
        JSONObject()
            .put("defaultAxleLockEnabled", setupDefaults.axleLockEnabled)
            .put("defaultSuspensionType", setupDefaults.suspensionType.name)
            .put("defaultMaxClicks", setupDefaults.maxClicks)
            .put("defaultStiffSide", setupDefaults.stiffSide.name)
            .put(
                "requireFeedbackBeforeExitConfig",
                tuningPreferences.requireFeedbackBeforeExitConfig
            )
            .put(
                "showPreviousFeedbackReference",
                tuningPreferences.showPreviousFeedbackReference
            )
            .put("showSetupDebugInfo", tuningPreferences.showSetupDebugInfo)

    private fun carToJson(car: CarProfile): JSONObject =
        JSONObject()
            .put("id", car.id)
            .put("name", car.name)
            .put("suspensionType", car.suspensionType.name)
            .put("adjusters", JSONArray(car.adjusters.map(::adjusterToJson)))

    private fun adjusterToJson(adjuster: AdjusterSpec): JSONObject =
        JSONObject()
            .put("label", adjuster.label)
            .put("maxClicks", adjuster.maxClicks)
            .put("stiffSide", adjuster.stiffSide.name)

    private fun setupStateMachineToJson(
        carId: String,
        stateMachine: SetupStateMachine?,
    ): JSONObject =
        JSONObject()
            .put("carId", carId)
            .put("states", JSONArray(stateMachine?.states.orEmpty().map(::setupStateToJson)))

    private fun setupStateToJson(state: SetupState): JSONObject =
        JSONObject()
            .put("timestampMillis", state.timestampMillis)
            .put("setup", setupToJson(state.setup))

    private fun setupConfigToJson(config: SetupConfig): JSONObject =
        JSONObject()
            .put("id", config.id)
            .put("carId", config.carId)
            .put("name", config.name)
            .put("createdAtMillis", config.createdAtMillis)
            .put("states", JSONArray(config.states.map(::setupConfigStateToJson)))

    private fun setupConfigStateToJson(state: SetupConfigState): JSONObject {
        val stateJson = JSONObject()
            .put("id", state.id ?: JSONObject.NULL)
            .put("timestampMillis", state.timestampMillis)
            .put("hasFeedback", state.hasFeedback)
            .put("setup", setupToJson(state.setup))

        stateJson.put(
            "feedback",
            if (state.hasFeedback) {
                JSONObject()
                    .put("cornerEntryBalance", state.cornerEntryBalance)
                    .put("cornerMidBalance", state.cornerMidBalance)
                    .put("cornerExitBalance", state.cornerExitBalance)
                    .put("overallGrip", state.overallGrip)
                    .put("bodyControlBalance", state.bodyControlBalance)
                    .put("lapTimeMillis", state.lapTimeMillis ?: JSONObject.NULL)
                    .put("note", state.note ?: JSONObject.NULL)
            } else {
                JSONObject.NULL
            }
        )

        return stateJson
    }

    private fun setupToJson(setup: SetupValues): JSONObject =
        JSONObject().apply {
            Corner.entries.forEach { corner ->
                put(
                    corner.name,
                    JSONObject().apply {
                        setup[corner].orEmpty().forEach { (adjusterLabel, clickValue) ->
                            put(adjusterLabel, clickValue)
                        }
                    }
                )
            }
        }

    private fun tuningDocumentToJson(document: TuningDocument): JSONObject =
        JSONObject()
            .put("id", document.id)
            .put("carId", document.carId)
            .put("name", document.name)
            .put("createdAtMillis", document.createdAtMillis)
}
