package com.example.suslog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CarEntity::class,
        AdjusterEntity::class,
        SetupStateEntity::class,
        SetupClickEntity::class,
        SetupConfigEntity::class,
        SetupConfigClickEntity::class,
        SetupConfigStateEntity::class,
        SetupConfigStateClickEntity::class,
        TuningDocumentEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class SuslogDatabase : RoomDatabase() {
    abstract fun suslogDao(): SuslogDao

    companion object {
        @Volatile
        private var instance: SuslogDatabase? = null

        fun getInstance(context: Context): SuslogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SuslogDatabase::class.java,
                    "suslog.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS setup_configs (
                        id TEXT NOT NULL PRIMARY KEY,
                        carId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        cornerEntryBalance INTEGER NOT NULL,
                        cornerExitBalance INTEGER NOT NULL,
                        lapTimeMillis INTEGER,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        FOREIGN KEY(carId) REFERENCES cars(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_setup_configs_carId ON setup_configs(carId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS setup_config_clicks (
                        configId TEXT NOT NULL,
                        corner TEXT NOT NULL,
                        adjusterLabel TEXT NOT NULL,
                        clickValue INTEGER NOT NULL,
                        PRIMARY KEY(configId, corner, adjusterLabel),
                        FOREIGN KEY(configId) REFERENCES setup_configs(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_setup_config_clicks_configId ON setup_config_clicks(configId)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS setup_config_states (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        configId TEXT NOT NULL,
                        cornerEntryBalance INTEGER NOT NULL,
                        cornerExitBalance INTEGER NOT NULL,
                        lapTimeMillis INTEGER,
                        timestampMillis INTEGER NOT NULL,
                        FOREIGN KEY(configId) REFERENCES setup_configs(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_setup_config_states_configId ON setup_config_states(configId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS setup_config_state_clicks (
                        stateId INTEGER NOT NULL,
                        corner TEXT NOT NULL,
                        adjusterLabel TEXT NOT NULL,
                        clickValue INTEGER NOT NULL,
                        PRIMARY KEY(stateId, corner, adjusterLabel),
                        FOREIGN KEY(stateId) REFERENCES setup_config_states(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_setup_config_state_clicks_stateId ON setup_config_state_clicks(stateId)"
                )
                db.execSQL(
                    """
                    INSERT INTO setup_config_states (
                        configId,
                        cornerEntryBalance,
                        cornerExitBalance,
                        lapTimeMillis,
                        timestampMillis
                    )
                    SELECT
                        id,
                        cornerEntryBalance,
                        cornerExitBalance,
                        lapTimeMillis,
                        updatedAtMillis
                    FROM setup_configs
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO setup_config_state_clicks (
                        stateId,
                        corner,
                        adjusterLabel,
                        clickValue
                    )
                    SELECT
                        setup_config_states.id,
                        setup_config_clicks.corner,
                        setup_config_clicks.adjusterLabel,
                        setup_config_clicks.clickValue
                    FROM setup_config_clicks
                    INNER JOIN setup_config_states
                        ON setup_config_states.configId = setup_config_clicks.configId
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE setup_config_states ADD COLUMN note TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tuning_documents (
                        id TEXT NOT NULL PRIMARY KEY,
                        carId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        FOREIGN KEY(carId) REFERENCES cars(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_tuning_documents_carId ON tuning_documents(carId)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE setup_config_states ADD COLUMN cornerMidBalance INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE setup_config_states ADD COLUMN overallGrip INTEGER NOT NULL DEFAULT 3"
                )
                db.execSQL(
                    "ALTER TABLE setup_config_states ADD COLUMN bodyControlBalance INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE setup_config_states ADD COLUMN hasFeedback INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }
}
