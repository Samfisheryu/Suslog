package app.suslog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CarEntity::class,
        AdjusterEntity::class,
        SetupStateEntity::class,
        SetupClickEntity::class,
        SetupConfigEntity::class,
        SetupConfigStateEntity::class,
        SetupConfigStateClickEntity::class,
        TuningDocumentEntity::class,
        TuningDocumentContentEntity::class,
        AiConversationEntity::class,
        AiMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SuslogDatabase : RoomDatabase() {
    abstract fun suslogDao(): SuslogDao

    companion object {
        @Volatile
        private var instance: SuslogDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tuning_document_contents` (
                        `documentId` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`documentId`),
                        FOREIGN KEY(`documentId`) REFERENCES `tuning_documents`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_tuning_document_contents_documentId`
                    ON `tuning_document_contents` (`documentId`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_conversations` (
                        `id` TEXT NOT NULL,
                        `localUserId` TEXT NOT NULL,
                        `carId` TEXT NOT NULL,
                        `configId` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `modelId` TEXT,
                        `providerThreadRef` TEXT,
                        `lastTurnRef` TEXT,
                        `systemPromptVersion` INTEGER NOT NULL,
                        `builtInDocsHash` TEXT,
                        `userDocsHash` TEXT,
                        `lastSentStateCount` INTEGER NOT NULL,
                        `currentSetupHash` TEXT,
                        `summary` TEXT,
                        `title` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`carId`) REFERENCES `cars`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`configId`) REFERENCES `setup_configs`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ai_conversations_localUserId` " +
                        "ON `ai_conversations` (`localUserId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ai_conversations_carId` " +
                        "ON `ai_conversations` (`carId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ai_conversations_configId` " +
                        "ON `ai_conversations` (`configId`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_messages` (
                        `id` TEXT NOT NULL,
                        `conversationId` TEXT NOT NULL,
                        `seq` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT,
                        `modelId` TEXT,
                        `providerResponseId` TEXT,
                        `linkedConfigStateId` INTEGER,
                        `structuredRecommendationJson` TEXT,
                        `inputTokenCount` INTEGER,
                        `outputTokenCount` INTEGER,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`conversationId`) REFERENCES `ai_conversations`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`linkedConfigStateId`) REFERENCES `setup_config_states`(`id`)
                            ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ai_messages_conversationId` " +
                        "ON `ai_messages` (`conversationId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ai_messages_linkedConfigStateId` " +
                        "ON `ai_messages` (`linkedConfigStateId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_ai_messages_conversationId_seq` " +
                        "ON `ai_messages` (`conversationId`, `seq`)"
                )
            }
        }

        fun getInstance(context: Context): SuslogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SuslogDatabase::class.java,
                    "suslog.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
