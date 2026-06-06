package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GameState::class, HighScore::class, Achievement::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deep_miner_adventure_db"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            val dao = getDatabase(context, scope).gameDao()
                            // Set initial Game State
                            dao.saveGameState(GameState())
                            
                            // Prepopulate Achievements
                            dao.insertAchievements(
                                listOf(
                                    Achievement("first_dig", "Scraping the Dirt", "Successfully mine your first block underground", false, 0, 1),
                                    Achievement("deep_50", "Cave Diver", "Reach a depth of 50 meters", false, 0, 50),
                                    Achievement("deep_150", "The Abyssal Zone", "Descend deep to 150 meters", false, 0, 150),
                                    Achievement("gold_rush", "Aurelian Dream", "Collect 20 gold ore pieces in cargo", false, 0, 20),
                                    Achievement("diamond_hand", "Glittering Depths", "Mine a flawless Diamond block from the deep layers", false, 0, 1),
                                    Achievement("treasured", "Lost Artifacts", "Unearth a Rare Hidden Artifact", false, 0, 1),
                                    Achievement("max_drill", "Hyperboreas", "Max out the drill upgrade level (Lv. 5)", false, 0, 5),
                                    Achievement("max_cargo", "Behemoth Cargo", "Upgrade inventory cargo size to max level (Lv. 5)", false, 0, 5)
                                )
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
