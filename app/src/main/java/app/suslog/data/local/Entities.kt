package app.suslog.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey val id: String,
    val localUserId: String,
    val name: String,
    val suspensionType: String,
)

@Entity(
    tableName = "adjusters",
    primaryKeys = ["carId", "label"],
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class AdjusterEntity(
    val carId: String,
    val label: String,
    val maxClicks: Int,
    val stiffSide: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "setup_states",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class SetupStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: String,
    val timestampMillis: Long,
)

@Entity(
    tableName = "setup_clicks",
    primaryKeys = ["stateId", "corner", "adjusterLabel"],
    foreignKeys = [
        ForeignKey(
            entity = SetupStateEntity::class,
            parentColumns = ["id"],
            childColumns = ["stateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stateId")]
)
data class SetupClickEntity(
    val stateId: Long,
    val corner: String,
    val adjusterLabel: String,
    val clickValue: Int,
)

@Entity(
    tableName = "setup_configs",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class SetupConfigEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val name: String,
    val cornerEntryBalance: Int,
    val cornerExitBalance: Int,
    val lapTimeMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "setup_config_states",
    foreignKeys = [
        ForeignKey(
            entity = SetupConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["configId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("configId")]
)
data class SetupConfigStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: String,
    val cornerEntryBalance: Int,
    val cornerMidBalance: Int,
    val cornerExitBalance: Int,
    val overallGrip: Int,
    val bodyControlBalance: Int,
    val lapTimeMillis: Long?,
    val timestampMillis: Long,
    val note: String?,
    val hasFeedback: Boolean,
)

@Entity(
    tableName = "setup_config_state_clicks",
    primaryKeys = ["stateId", "corner", "adjusterLabel"],
    foreignKeys = [
        ForeignKey(
            entity = SetupConfigStateEntity::class,
            parentColumns = ["id"],
            childColumns = ["stateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stateId")]
)
data class SetupConfigStateClickEntity(
    val stateId: Long,
    val corner: String,
    val adjusterLabel: String,
    val clickValue: Int,
)

@Entity(
    tableName = "tuning_documents",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class TuningDocumentEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val name: String,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "tuning_document_contents",
    foreignKeys = [
        ForeignKey(
            entity = TuningDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class TuningDocumentContentEntity(
    @PrimaryKey val documentId: String,
    val content: String,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "ai_conversations",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SetupConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["configId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("localUserId"),
        Index("carId"),
        Index("configId")
    ]
)
data class AiConversationEntity(
    @PrimaryKey val id: String,
    val localUserId: String,
    val carId: String,
    val configId: String,
    val provider: String,
    val modelId: String?,
    val providerThreadRef: String?,
    val lastTurnRef: String?,
    val systemPromptVersion: Int,
    val builtInDocsHash: String?,
    val userDocsHash: String?,
    val lastSentStateCount: Int,
    val currentSetupHash: String?,
    val summary: String?,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SetupConfigStateEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedConfigStateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("conversationId"),
        Index("linkedConfigStateId"),
        Index(value = ["conversationId", "seq"], unique = true)
    ]
)
data class AiMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val seq: Int,
    val role: String,
    val content: String,
    val status: String,
    val errorMessage: String?,
    val modelId: String?,
    val providerResponseId: String?,
    val linkedConfigStateId: Long?,
    val structuredRecommendationJson: String?,
    val inputTokenCount: Int?,
    val outputTokenCount: Int?,
    val createdAtMillis: Long,
)
