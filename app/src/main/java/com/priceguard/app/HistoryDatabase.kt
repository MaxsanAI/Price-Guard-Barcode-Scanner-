package com.priceguard.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "scan_history",
    indices = [Index(value = ["barcode"])] // Optimizacija: indeks za brže pretraživanje
)
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val brand: String,
    val imageUrl: String,
    val price: String, 
    val nutriscore: String,
    val categories: String,
    val novaGroup: Int = 0,
    val additivesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val countryCode: String = ""
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM scan_history")
    suspend fun clearAllHistory()
}

@Database(entities = [HistoryItem::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "priceguard_database"
                )
                .fallbackToDestructiveMigration() // Sigurno za razvoj, za produkciju kasnije dodaj migracije
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}