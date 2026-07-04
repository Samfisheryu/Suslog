package app.suslog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import app.suslog.data.ai.AiUsageByModel
import app.suslog.domain.setup.SetupConfig
import app.suslog.domain.setup.SetupState
import app.suslog.domain.tuning.TuningDocument

@Dao
interface SuslogDao {
    @Transaction
    @Query("SELECT * FROM cars WHERE localUserId = :localUserId ORDER BY name")
    suspend fun getCarsWithAdjusters(localUserId: String): List<CarWithAdjusters>

    @Query("SELECT * FROM setup_states WHERE carId = :carId ORDER BY id ASC")
    suspend fun getSetupStates(carId: String): List<SetupStateEntity>

    @Query("SELECT * FROM setup_clicks WHERE stateId IN (:stateIds)")
    suspend fun getClicksForStates(stateIds: List<Long>): List<SetupClickEntity>

    @Query(
        """
        SELECT setup_configs.*
        FROM setup_configs
        INNER JOIN cars ON setup_configs.carId = cars.id
        WHERE cars.localUserId = :localUserId
        ORDER BY setup_configs.updatedAtMillis DESC
        """
    )
    suspend fun getSetupConfigs(localUserId: String): List<SetupConfigEntity>

    @Query("SELECT * FROM setup_config_states WHERE configId IN (:configIds) ORDER BY id ASC")
    suspend fun getSetupConfigStates(configIds: List<String>): List<SetupConfigStateEntity>

    @Query("SELECT * FROM setup_config_state_clicks WHERE stateId IN (:stateIds)")
    suspend fun getClicksForConfigStates(stateIds: List<Long>): List<SetupConfigStateClickEntity>

    @Query(
        """
        SELECT tuning_documents.*
        FROM tuning_documents
        INNER JOIN cars ON tuning_documents.carId = cars.id
        WHERE cars.localUserId = :localUserId
        ORDER BY tuning_documents.createdAtMillis DESC
        """
    )
    suspend fun getTuningDocuments(localUserId: String): List<TuningDocumentEntity>

    @Query("SELECT * FROM tuning_document_contents WHERE documentId IN (:documentIds)")
    suspend fun getTuningDocumentContents(
        documentIds: List<String>,
    ): List<TuningDocumentContentEntity>

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
    suspend fun insertSetupConfigState(state: SetupConfigStateEntity): Long

    @Insert
    suspend fun insertSetupConfigStateClicks(clicks: List<SetupConfigStateClickEntity>)

    @Insert
    suspend fun insertTuningDocument(document: TuningDocumentEntity)

    @Insert
    suspend fun insertTuningDocumentContent(content: TuningDocumentContentEntity)

    @Query("SELECT * FROM ai_conversations WHERE configId = :configId ORDER BY updatedAtMillis DESC")
    suspend fun getAiConversationsForConfig(configId: String): List<AiConversationEntity>

    @Query(
        """
        SELECT * FROM ai_conversations
        WHERE localUserId = :localUserId
            AND configId = :configId
            AND provider = :provider
        ORDER BY updatedAtMillis DESC
        LIMIT 1
        """
    )
    suspend fun getLatestAiConversation(
        localUserId: String,
        configId: String,
        provider: String,
    ): AiConversationEntity?

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY seq ASC")
    suspend fun getAiMessages(conversationId: String): List<AiMessageEntity>

    @Query(
        """
        SELECT COALESCE(ai_messages.modelId, ai_conversations.modelId, 'unknown') AS modelId,
            COALESCE(SUM(COALESCE(ai_messages.inputTokenCount, 0)), 0) AS inputTokens,
            COALESCE(SUM(COALESCE(ai_messages.outputTokenCount, 0)), 0) AS outputTokens,
            COUNT(*) AS requestCount
        FROM ai_messages
        INNER JOIN ai_conversations ON ai_messages.conversationId = ai_conversations.id
        WHERE ai_conversations.localUserId = :localUserId
            AND ai_conversations.provider = :provider
            AND ai_messages.status = 'complete'
            AND ai_messages.createdAtMillis >= :startMillis
            AND ai_messages.createdAtMillis <= :endMillis
            AND (
                ai_messages.inputTokenCount IS NOT NULL OR
                ai_messages.outputTokenCount IS NOT NULL
            )
        GROUP BY COALESCE(ai_messages.modelId, ai_conversations.modelId, 'unknown')
        """
    )
    suspend fun getAiUsageByModel(
        localUserId: String,
        provider: String,
        startMillis: Long,
        endMillis: Long,
    ): List<AiUsageByModel>

    @Insert
    suspend fun insertAiConversation(conversation: AiConversationEntity)

    @Insert
    suspend fun insertAiMessage(message: AiMessageEntity)

    @Query(
        """
        UPDATE ai_conversations
        SET providerThreadRef = :providerThreadRef,
            lastTurnRef = :lastTurnRef,
            builtInDocsHash = :builtInDocsHash,
            userDocsHash = :userDocsHash,
            lastSentStateCount = :lastSentStateCount,
            currentSetupHash = :currentSetupHash,
            summary = :summary,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :conversationId
        """
    )
    suspend fun updateAiConversationState(
        conversationId: String,
        providerThreadRef: String?,
        lastTurnRef: String?,
        builtInDocsHash: String?,
        userDocsHash: String?,
        lastSentStateCount: Int,
        currentSetupHash: String?,
        summary: String?,
        updatedAtMillis: Long,
    )

    @Query(
        """
        UPDATE ai_messages
        SET status = :status,
            errorMessage = :errorMessage,
            content = :content,
            providerResponseId = :providerResponseId,
            structuredRecommendationJson = :structuredRecommendationJson,
            inputTokenCount = :inputTokenCount,
            outputTokenCount = :outputTokenCount
        WHERE id = :messageId
        """
    )
    suspend fun updateAiMessageResult(
        messageId: String,
        status: String,
        errorMessage: String?,
        content: String,
        providerResponseId: String?,
        structuredRecommendationJson: String?,
        inputTokenCount: Int?,
        outputTokenCount: Int?,
    )

    @Query("UPDATE ai_conversations SET updatedAtMillis = :updatedAtMillis WHERE id = :conversationId")
    suspend fun touchAiConversation(
        conversationId: String,
        updatedAtMillis: Long,
    )

    @Query(
        """
        UPDATE ai_conversations
        SET lastTurnRef = :lastTurnRef,
            lastSentStateCount = :lastSentStateCount,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :conversationId
        """
    )
    suspend fun updateAiConversationLastTurnRef(
        conversationId: String,
        lastTurnRef: String?,
        lastSentStateCount: Int,
        updatedAtMillis: Long,
    )

    @Query(
        """
        UPDATE setup_config_states
        SET cornerEntryBalance = :cornerEntryBalance,
            cornerMidBalance = :cornerMidBalance,
            cornerExitBalance = :cornerExitBalance,
            overallGrip = :overallGrip,
            bodyControlBalance = :bodyControlBalance,
            lapTimeMillis = :lapTimeMillis,
            timestampMillis = :timestampMillis,
            note = :note,
            hasFeedback = :hasFeedback
        WHERE id = :stateId
        """
    )
    suspend fun updateSetupConfigState(
        stateId: Long,
        cornerEntryBalance: Int,
        cornerMidBalance: Int,
        cornerExitBalance: Int,
        overallGrip: Int,
        bodyControlBalance: Int,
        lapTimeMillis: Long?,
        timestampMillis: Long,
        note: String?,
        hasFeedback: Boolean,
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

    @Query("DELETE FROM setup_configs WHERE id = :configId")
    suspend fun deleteSetupConfig(configId: String)

    @Query("DELETE FROM cars WHERE localUserId = :localUserId")
    suspend fun deleteCarsForLocalUser(localUserId: String)

    @Query("UPDATE cars SET localUserId = :localUserId WHERE localUserId = :legacyLocalUserId")
    suspend fun adoptLegacyCars(
        localUserId: String,
        legacyLocalUserId: String,
    )

    @Transaction
    suspend fun insertTuningDocumentWithContent(document: TuningDocument) {
        insertTuningDocument(document.toEntity())
        insertTuningDocumentContent(document.toContentEntity())
    }

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
                cornerMidBalance = state.cornerMidBalance,
                cornerExitBalance = state.cornerExitBalance,
                overallGrip = state.overallGrip,
                bodyControlBalance = state.bodyControlBalance,
                lapTimeMillis = state.lapTimeMillis,
                timestampMillis = state.timestampMillis,
                note = state.note,
                hasFeedback = state.hasFeedback
            )
            deleteSetupConfigStateClicks(existingStateId)
            existingStateId
        }

        insertSetupConfigStateClicks(state.toClickEntities(stateId))

        return stateId
    }
}
