package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val gameState: Flow<GameState?> = gameDao.getGameState()
    val highScores: Flow<List<HighScore>> = gameDao.getHighScores()
    val achievements: Flow<List<Achievement>> = gameDao.getAchievements()

    suspend fun getGameStateSync(): GameState? {
        return gameDao.getGameStateSync()
    }

    suspend fun saveGameState(state: GameState) {
        gameDao.saveGameState(state)
    }

    suspend fun insertHighScore(score: HighScore) {
        gameDao.insertHighScore(score)
    }

    suspend fun unlockAchievement(id: String) {
        val ach = gameDao.getAchievement(id)
        if (ach != null && !ach.isUnlocked) {
            gameDao.updateAchievement(ach.copy(isUnlocked = true, progress = ach.maxProgress))
        }
    }

    suspend fun updateAchievementProgress(id: String, currentProgress: Int) {
        val ach = gameDao.getAchievement(id) ?: return
        if (!ach.isUnlocked) {
            val newProg = currentProgress.coerceAtMost(ach.maxProgress)
            val unlocked = newProg >= ach.maxProgress
            gameDao.updateAchievement(ach.copy(progress = newProg, isUnlocked = unlocked))
        }
    }
}
