package com.example.suslog.data.repository

import com.example.suslog.data.local.SuslogDao
import com.example.suslog.data.local.legacySetupConfigFromEntities
import com.example.suslog.data.local.setupConfigFromEntities
import com.example.suslog.data.local.setupStateFromEntities
import com.example.suslog.data.local.toAdjusterEntities
import com.example.suslog.data.local.toDomain
import com.example.suslog.data.local.toEntity
import com.example.suslog.domain.car.CarProfile
import com.example.suslog.domain.setup.SetupConfig
import com.example.suslog.domain.setup.SetupState
import com.example.suslog.domain.setup.SetupStateMachine
import com.example.suslog.domain.setup.initialSetupStateMachine
import com.example.suslog.domain.tuning.TuningDocument

data class PersistedSetupData(
    val cars: List<CarProfile>,
    val setupStateMachinesByCar: Map<String, SetupStateMachine>,
    val setupConfigs: List<SetupConfig>,
    val tuningDocuments: List<TuningDocument>,
)

class SetupRepository(
    private val dao: SuslogDao,
) {
    suspend fun load(): PersistedSetupData {
        val cars = dao.getCarsWithAdjusters().map { it.toDomain() }
        val machines = cars.associate { car ->
            car.id to loadStateMachine(car)
        }

        return PersistedSetupData(
            cars = cars,
            setupStateMachinesByCar = machines,
            setupConfigs = loadSetupConfigs(),
            tuningDocuments = dao.getTuningDocuments().map { it.toDomain() }
        )
    }

    suspend fun addCar(car: CarProfile): SetupStateMachine {
        val stateMachine = car.initialSetupStateMachine()
        val initialState = requireNotNull(stateMachine.currentState)

        dao.insertCarWithInitialState(
            car = car.toEntity(),
            adjusters = car.toAdjusterEntities(),
            initialState = initialState
        )

        return stateMachine
    }

    suspend fun updateCar(car: CarProfile) {
        dao.updateCarWithAdjusters(
            car = car.toEntity(),
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

    suspend fun addTuningDocument(document: TuningDocument) {
        dao.insertTuningDocument(document.toEntity())
    }

    private suspend fun loadSetupConfigs(): List<SetupConfig> {
        val configEntities = dao.getSetupConfigs()
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
        val legacyClicksByConfig = dao.getClicksForConfigs(configIds)
            .groupBy { it.configId }

        return configEntities.map { config ->
            val states = statesByConfig[config.id].orEmpty().map { state ->
                state.toDomain(clicksByState[state.id].orEmpty())
            }

            if (states.isNotEmpty()) {
                setupConfigFromEntities(config = config, states = states)
            } else {
                legacySetupConfigFromEntities(
                    config = config,
                    clicks = legacyClicksByConfig[config.id].orEmpty()
                )
            }
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
