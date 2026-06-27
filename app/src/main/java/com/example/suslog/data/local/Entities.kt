package com.example.suslog.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey val id: String,
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
    tableName = "setup_config_clicks",
    primaryKeys = ["configId", "corner", "adjusterLabel"],
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
data class SetupConfigClickEntity(
    val configId: String,
    val corner: String,
    val adjusterLabel: String,
    val clickValue: Int,
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
    val cornerExitBalance: Int,
    val lapTimeMillis: Long?,
    val timestampMillis: Long,
    val note: String?,
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
