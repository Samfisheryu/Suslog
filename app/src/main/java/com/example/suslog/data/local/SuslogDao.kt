package com.example.suslog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.suslog.domain.setup.SetupConfig
import com.example.suslog.domain.setup.SetupState

@Dao
interface SuslogDao {
    @Transaction
    @Query("SELECT * FROM cars ORDER BY name")
    suspend fun getCarsWithAdjusters(): List<CarWithAdjusters>

    @Query("SELECT * FROM setup_states WHERE carId = :carId ORDER BY id ASC")
    suspend fun getSetupStates(carId: String): List<SetupStateEntity>

    @Query("SELECT * FROM setup_clicks WHERE stateId IN (:stateIds)")
    suspend fun getClicksForStates(stateIds: List<Long>): List<SetupClickEntity>

    @Query("SELECT * FROM setup_configs ORDER BY updatedAtMillis DESC")
    suspend fun getSetupConfigs(): List<SetupConfigEntity>

    @Query("SELECT * FROM setup_config_clicks WHERE configId IN (:configIds)")
    suspend fun getClicksForConfigs(configIds: List<String>): List<SetupConfigClickEntity>

    @Query("SELECT * FROM setup_config_states WHERE configId IN (:configIds) ORDER BY id ASC")
    suspend fun getSetupConfigStates(configIds: List<String>): List<SetupConfigStateEntity>

    @Query("SELECT * FROM setup_config_state_clicks WHERE stateId IN (:stateIds)")
    suspend fun getClicksForConfigStates(stateIds: List<Long>): List<SetupConfigStateClickEntity>

    @Query("SELECT * FROM tuning_documents ORDER BY createdAtMillis DESC")
    suspend fun getTuningDocuments(): List<TuningDocumentEntity>

    @Insert
    suspend fun insertCar(car: CarEntity)

    @Insert
    suspend fun insertAdjusters(adjusters: List<AdjusterEntity>)

    @Query("UPDATE cars SET name = :name, suspensionType = :suspensionType WHERE id = :carId")
    suspend fun updateCar(
        carId: String,
        name: String,
        suspensionType: String,
    )

    @Query("DELETE FROM adjusters WHERE carId = :carId")
    suspend fun deleteAdjustersForCar(carId: String)

    @Insert
    suspend fun insertSetupState(state: SetupStateEntity): Long

    @Insert
    suspend fun insertSetupClicks(clicks: List<SetupClickEntity>)

    @Insert
    suspend fun insertSetupConfig(config: SetupConfigEntity)

    @Insert
    suspend fun insertSetupConfigClicks(clicks: List<SetupConfigClickEntity>)

    @Insert
    suspend fun insertSetupConfigState(state: SetupConfigStateEntity): Long

    @Insert
    suspend fun insertSetupConfigStateClicks(clicks: List<SetupConfigStateClickEntity>)

    @Insert
    suspend fun insertTuningDocument(document: TuningDocumentEntity)

    @Query(
        """
        UPDATE setup_config_states
        SET cornerEntryBalance = :cornerEntryBalance,
            cornerExitBalance = :cornerExitBalance,
            lapTimeMillis = :lapTimeMillis,
            timestampMillis = :timestampMillis,
            note = :note
        WHERE id = :stateId
        """
    )
    suspend fun updateSetupConfigState(
        stateId: Long,
        cornerEntryBalance: Int,
        cornerExitBalance: Int,
        lapTimeMillis: Long?,
        timestampMillis: Long,
        note: String?,
    )

    @Query("DELETE FROM setup_config_state_clicks WHERE stateId = :stateId")
    suspend fun deleteSetupConfigStateClicks(stateId: Long)

    @Query(
        """
        UPDATE setup_configs
        SET name = :name,
            cornerEntryBalance = :cornerEntryBalance,
            cornerExitBalance = :cornerExitBalance,
            lapTimeMillis = :lapTimeMillis,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :configId
        """
    )
    suspend fun updateSetupConfig(
        configId: String,
        name: String,
        cornerEntryBalance: Int,
        cornerExitBalance: Int,
        lapTimeMillis: Long?,
        updatedAtMillis: Long,
    )

    @Query("DELETE FROM setup_config_clicks WHERE configId = :configId")
    suspend fun deleteSetupConfigClicks(configId: String)

    @Transaction
    suspend fun insertCarWithInitialState(
        car: CarEntity,
        adjusters: List<AdjusterEntity>,
        initialState: SetupState,
    ) {
        insertCar(car)
        insertAdjusters(adjusters)
        insertSetupStateSnapshot(car.id, initialState)
    }

    @Transaction
    suspend fun updateCarWithAdjusters(
        car: CarEntity,
        adjusters: List<AdjusterEntity>,
    ) {
        updateCar(
            carId = car.id,
            name = car.name,
            suspensionType = car.suspensionType
        )
        deleteAdjustersForCar(car.id)
        insertAdjusters(adjusters)
    }

    @Transaction
    suspend fun insertSetupStateSnapshot(
        carId: String,
        state: SetupState,
    ) {
        val stateId = insertSetupState(
            SetupStateEntity(
                carId = carId,
                timestampMillis = state.timestampMillis
            )
        )
        insertSetupClicks(state.toClickEntities(stateId))
    }

    @Transaction
    suspend fun insertSetupConfigSnapshot(config: SetupConfig): Long {
        insertSetupConfig(config.toEntity())
        val stateId = upsertCurrentSetupConfigState(config)
        insertSetupConfigClicks(config.toLegacyClickEntities())

        return stateId
    }

    @Transaction
    suspend fun updateSetupConfigSnapshot(config: SetupConfig): Long {
        val entity = config.toEntity()

        updateSetupConfig(
            configId = entity.id,
            name = entity.name,
            cornerEntryBalance = entity.cornerEntryBalance,
            cornerExitBalance = entity.cornerExitBalance,
            lapTimeMillis = entity.lapTimeMillis,
            updatedAtMillis = entity.updatedAtMillis
        )
        deleteSetupConfigClicks(entity.id)
        insertSetupConfigClicks(config.toLegacyClickEntities())

        return upsertCurrentSetupConfigState(config)
    }

    private suspend fun upsertCurrentSetupConfigState(config: SetupConfig): Long {
        val state = requireNotNull(config.currentState) {
            "SetupConfig must have at least one state before persistence."
        }
        val existingStateId = state.id
        val stateId = if (existingStateId == null) {
            insertSetupConfigState(config.currentStateEntity())
        } else {
            updateSetupConfigState(
                stateId = existingStateId,
                cornerEntryBalance = state.cornerEntryBalance,
                cornerExitBalance = state.cornerExitBalance,
                lapTimeMillis = state.lapTimeMillis,
                timestampMillis = state.timestampMillis,
                note = state.note
            )
            deleteSetupConfigStateClicks(existingStateId)
            existingStateId
        }

        insertSetupConfigStateClicks(state.toClickEntities(stateId))

        return stateId
    }
}
