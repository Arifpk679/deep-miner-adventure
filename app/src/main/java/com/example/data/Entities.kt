package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 200, // Starts with some coins for first upgrade
    val currentEnergy: Float = 100f,
    val drillLevel: Int = 1,
    val energyLevel: Int = 1,
    val cargoLevel: Int = 1,
    val detectorLevel: Int = 1,
    val speedLevel: Int = 1,
    
    // Inventory cargo
    val cargoDirt: Int = 0,
    val cargoStone: Int = 0,
    val cargoGold: Int = 0,
    val cargoRuby: Int = 0,
    val cargoDiamond: Int = 0,
    val cargoEmerald: Int = 0,
    val cargoTreasure: Int = 0,
    
    val maxDepthReached: Int = 0,
    val totalCoinsEarned: Int = 200,
    val soundEnabled: Boolean = true,
    val dayCounter: Int = 1,
    val lastRewardDate: Long = 0L
)

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val depth: Int,
    val coinsEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val maxProgress: Int = 1
)
