package app.suslog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CarEntity::class,
        AdjusterEntity::class,
        SetupStateEntity::class,
        SetupClickEntity::class,
        SetupConfigEntity::class,
        SetupConfigStateEntity::class,
        SetupConfigStateClickEntity::class,
        TuningDocumentEntity::class
    ],
    version = 1,
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
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
