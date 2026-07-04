package app.suslog.data.repository

import app.suslog.data.local.SuslogDao
import app.suslog.data.local.SuslogDatabase
import app.suslog.data.local.setupConfigFromEntities
import app.suslog.data.local.setupStateFromEntities
import app.suslog.data.local.toAdjusterEntities
import app.suslog.data.local.toDomain
import app.suslog.data.local.toEntity
import app.suslog.domain.car.CarProfile
import app.suslog.domain.setup.SetupConfig
import app.suslog.domain.setup.SetupState
import app.suslog.domain.setup.SetupStateMachine
import app.suslog.domain.setup.initialSetupStateMachine
import app.suslog.domain.tuning.TuningDocument

data class PersistedSetupData(
    val cars: List<CarProfile>,
    val setupStateMachinesByCar: Map<String, SetupStateMachine>,
    val setupConfigs: List<SetupConfig>,
    val tuningDocuments: List<TuningDocument>,
)

class SetupRepository(
    private val dao: SuslogDao,
) {
    suspend fun load(localUserId: String): PersistedSetupData {
        dao.adoptLegacyCars(
            localUserId = localUserId,
            legacyLocalUserId = SuslogDatabase.LEGACY_LOCAL_USER_ID
        )

        val cars = dao.getCarsWithAdjusters(localUserId).map { it.toDomain() }
        val machines = cars.associate { car ->
            car.id to loadStateMachine(car)
        }

        return PersistedSetupData(
            cars = cars,
            setupStateMachinesByCar = machines,
            setupConfigs = loadSetupConfigs(localUserId),
            tuningDocuments = loadTuningDocuments(localUserId)
        )
    }

    suspend fun addCar(
        car: CarProfile,
        localUserId: String,
    ): SetupStateMachine {
        val stateMachine = car.initialSetupStateMachine()
        val initialState = requireNotNull(stateMachine.currentState)

        dao.insertCarWithInitialState(
            car = car.toEntity(localUserId),
            adjusters = car.toAdjusterEntities(),
            initialState = initialState
        )

        return stateMachine
    }

    suspend fun updateCar(
        car: CarProfile,
        localUserId: String,
    ) {
        dao.updateCarWithAdjusters(
            car = car.toEntity(localUserId),
            adjusters = car.toAdjusterEntities()
        )
    }

    suspend fun appendSetupState(
        carId: String,
        state: SetupState,
    ) {
        dao.insertSetupStateSnapshot(carId, state)
    }

    suspend fun addSetupConfig(config: SetupConfig): SetupConfig {
        val stateId = dao.insertSetupConfigSnapshot(config)

        return config.withCurrentStateId(stateId)
    }

    suspend fun updateSetupConfig(config: SetupConfig): SetupConfig {
        val stateId = dao.updateSetupConfigSnapshot(config)

        return config.withCurrentStateId(stateId)
    }

    suspend fun deleteSetupConfig(configId: String) {
        dao.deleteSetupConfig(configId)
    }

    suspend fun clearLocalData(localUserId: String) {
        dao.deleteCarsForLocalUser(localUserId)
    }

    suspend fun addTuningDocument(document: TuningDocument) {
        dao.insertTuningDocumentWithContent(document)
    }

    private suspend fun loadTuningDocuments(localUserId: String): List<TuningDocument> {
        val documents = dao.getTuningDocuments(localUserId)
        if (documents.isEmpty()) {
            return emptyList()
        }

        val contentsByDocumentId = dao.getTuningDocumentContents(documents.map { it.id })
            .associateBy { it.documentId }

        return documents.map { document ->
            document.toDomain(contentsByDocumentId[document.id])
        }
    }

    private suspend fun loadSetupConfigs(localUserId: String): List<SetupConfig> {
        val configEntities = dao.getSetupConfigs(localUserId)
        if (configEntities.isEmpty()) {
            return emptyList()
        }

        val configIds = configEntities.map { it.id }
        val stateEntities = dao.getSetupConfigStates(configIds)
        val statesByConfig = stateEntities.groupBy { it.configId }
        val clicksByState = if (stateEntities.isEmpty()) {
            emptyMap()
        } else {
            dao.getClicksForConfigStates(stateEntities.map { it.id })
                .groupBy { it.stateId }
        }
        return configEntities.map { config ->
            val states = statesByConfig[config.id].orEmpty().map { state ->
                state.toDomain(clicksByState[state.id].orEmpty())
            }

            setupConfigFromEntities(config = config, states = states)
        }
    }

    private suspend fun loadStateMachine(car: CarProfile): SetupStateMachine {
        val stateEntities = dao.getSetupStates(car.id)
        if (stateEntities.isEmpty()) {
            return car.initialSetupStateMachine()
        }

        val clicksByState = dao.getClicksForStates(stateEntities.map { it.id })
            .groupBy { it.stateId }

        return SetupStateMachine(
            states = stateEntities.map { state ->
                setupStateFromEntities(
                    state = state,
                    clicks = clicksByState[state.id].orEmpty()
                )
            }
        )
    }
}
