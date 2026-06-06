package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSynthesizer
import com.example.data.AppDatabase
import com.example.data.GameState
import com.example.data.HighScore
import com.example.data.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// Grid configuration
const val MAP_WIDTH = 22
const val MAP_HEIGHT = 180
const val SURFACE_DEPTH = 5 // Depths 0 to 4 are air/sky

enum class BlockType(val label: String, val baseDurability: Float, val sellPrice: Int) {
    AIR("Empty Space", 0f, 0),
    SURFACE_STATION("Solar Port Terminal", 0f, 0),
    DIRT("Earthy Soil", 8f, 2),
    STONE("Tectonic Stone", 25f, 6),
    GOLD("Raw Gold Nugget", 60f, 25),
    RUBY("Glowing Ruby Ore", 100f, 60),
    EMERALD("Crystalline Emerald", 160f, 120),
    DIAMOND("Flawless Diamond Core", 240f, 250),
    TREASURE("Gilded Relic Box", 120f, 500),
    BEDROCK("Unbreakable Bedrock", Float.MAX_VALUE, 0)
}

data class BlockState(
    val col: Int,
    val row: Int,
    val type: BlockType,
    var currentHealth: Float,
    val maxHealth: Float
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository
    
    val gameState: StateFlow<GameState?>
    val highScores: StateFlow<List<HighScore>>
    val achievements: StateFlow<List<com.example.data.Achievement>>

    // Active screen navigation state
    var currentScreen by mutableStateOf("main_menu")

    // Active game variables (in-memory dive status)
    var playerX by mutableFloatStateOf(11f)
    var playerY by mutableFloatStateOf(2f)
    var velX by mutableFloatStateOf(0f)
    var velY by mutableFloatStateOf(0f)
    
    var currentEnergy by mutableFloatStateOf(100f)
    var maxEnergy by mutableFloatStateOf(100f)
    
    var cargoDirt by mutableIntStateOf(0)
    var cargoStone by mutableIntStateOf(0)
    var cargoGold by mutableIntStateOf(0)
    var cargoRuby by mutableIntStateOf(0)
    var cargoEmerald by mutableIntStateOf(0)
    var cargoDiamond by mutableIntStateOf(0)
    var cargoTreasure by mutableIntStateOf(0)
    var currentDepthMeter by mutableIntStateOf(0)

    // Drilling state
    var drillingCol by mutableIntStateOf(-1)
    var drillingRow by mutableIntStateOf(-1)
    var drillProgress by mutableFloatStateOf(0f)

    // Map Grid representation (List of rows)
    var gameGrid by mutableStateOf<List<List<BlockState>>>(emptyList())

    // Game loop control
    private var gameLoopJob: Job? = null
    var isDivingActive by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var isGameOver by mutableStateOf(false)
    var endReportCoinsEarned by mutableIntStateOf(0)
    var endReportDepthReached by mutableIntStateOf(0)

    // Docking Status Info Overlay
    var isDocked by mutableStateOf(false)
    var dockingMessage by mutableStateOf("")

    // Daily Reward
    var dailyRewardAvailable by mutableStateOf(false)

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = GameRepository(database.gameDao())
        
        gameState = repository.gameState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        highScores = repository.highScores.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        achievements = repository.achievements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            // Apply Sound toggles loaded from db
            gameState.collect { state ->
                state?.let {
                    AudioSynthesizer.enabled = it.soundEnabled
                    maxEnergy = getEnergyCapacityForLevel(it.energyLevel)
                    checkDailyRewardStatus(it)
                }
            }
        }
    }

    // Helper functions for gear levels
    fun getDrillMultiplier(level: Int): Float {
        return 1f + 0.6f * (level - 1)
    }

    fun getEnergyCapacityForLevel(level: Int): Float {
        return 100f + 50f * (level - 1)
    }

    fun getCargoMaxForLevel(level: Int): Int {
        return 15 + 20 * (level - 1)
    }

    fun getDetectorBonus(level: Int): Int {
        return level // 1 = dirt, 2 = gold, 3 = ruby, 4 = emerald, 5 = diamond / beacons
    }

    fun getMovementModifier(level: Int): Float {
        return 1f + 0.25f * (level - 1)
    }

    private fun checkDailyRewardStatus(state: GameState) {
        val lastReward = state.lastRewardDate
        val today = System.currentTimeMillis()
        
        // Formatter to check day similarity
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val lastDayStr = if (lastReward == 0L) "" else fmt.format(Date(lastReward))
        val todayStr = fmt.format(Date(today))

        dailyRewardAvailable = lastDayStr != todayStr
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val state = repository.getGameStateSync() ?: return@launch
            if (dailyRewardAvailable) {
                val updatedState = state.copy(
                    coins = state.coins + 200,
                    totalCoinsEarned = state.totalCoinsEarned + 200,
                    lastRewardDate = System.currentTimeMillis(),
                    dayCounter = state.dayCounter + 1
                )
                repository.saveGameState(updatedState)
                dailyRewardAvailable = false
                AudioSynthesizer.playUpgrade()
                repository.unlockAchievement("first_dig") // Give first dig on claim if needed
            }
        }
    }

    // Toggle Sound Settings
    fun toggleSound() {
        viewModelScope.launch {
            val state = repository.getGameStateSync() ?: return@launch
            val target = !state.soundEnabled
            repository.saveGameState(state.copy(soundEnabled = target))
            AudioSynthesizer.enabled = target
            AudioSynthesizer.playDigBreak()
        }
    }

    // Purchase Upgrades in Shop
    fun purchaseUpgrade(type: String) {
        viewModelScope.launch {
            val state = repository.getGameStateSync() ?: return@launch
            var cost = 0
            var nextLevelState = state

            when (type) {
                "drill" -> {
                    if (state.drillLevel < 5) {
                        cost = getUpgradeCost(state.drillLevel)
                        if (state.coins >= cost) {
                            nextLevelState = state.copy(
                                coins = state.coins - cost,
                                drillLevel = state.drillLevel + 1
                            )
                        }
                    }
                }
                "energy" -> {
                    if (state.energyLevel < 5) {
                        cost = getUpgradeCost(state.energyLevel)
                        if (state.coins >= cost) {
                            nextLevelState = state.copy(
                                coins = state.coins - cost,
                                energyLevel = state.energyLevel + 1
                            )
                        }
                    }
                }
                "cargo" -> {
                    if (state.cargoLevel < 5) {
                        cost = getUpgradeCost(state.cargoLevel)
                        if (state.coins >= cost) {
                            nextLevelState = state.copy(
                                coins = state.coins - cost,
                                cargoLevel = state.cargoLevel + 1
                            )
                        }
                    }
                }
                "detector" -> {
                    if (state.detectorLevel < 5) {
                        cost = getUpgradeCost(state.detectorLevel)
                        if (state.coins >= cost) {
                            nextLevelState = state.copy(
                                coins = state.coins - cost,
                                detectorLevel = state.detectorLevel + 1
                            )
                        }
                    }
                }
                "speed" -> {
                    if (state.speedLevel < 5) {
                        cost = getUpgradeCost(state.speedLevel)
                        if (state.coins >= cost) {
                            nextLevelState = state.copy(
                                coins = state.coins - cost,
                                speedLevel = state.speedLevel + 1
                            )
                        }
                    }
                }
            }

            if (cost > 0 && nextLevelState != state) {
                repository.saveGameState(nextLevelState)
                AudioSynthesizer.playUpgrade()
                
                // Track achievement upgrades
                if (nextLevelState.drillLevel >= 5) {
                    repository.unlockAchievement("max_drill")
                }
                if (nextLevelState.cargoLevel >= 5) {
                    repository.unlockAchievement("max_cargo")
                }
            }
        }
    }

    fun getUpgradeCost(currentLevel: Int): Int {
        return when (currentLevel) {
            1 -> 150
            2 -> 350
            3 -> 750
            4 -> 1500
            else -> 0
        }
    }

    // Check Total Cargo Count
    fun getCargoWeight(): Int {
        return cargoDirt + cargoStone + cargoGold + cargoRuby + cargoEmerald + cargoDiamond + cargoTreasure
    }

    // Launch a new underground diving mission
    fun startNewDive() {
        viewModelScope.launch {
            val state = repository.getGameStateSync() ?: GameState()
            
            // Map Setup
            generateMap()
            
            // Reset player states
            playerX = 11f // Mid horizontal
            playerY = 1.5f // Above ground level (which is Y = 5)
            velY = 0f
            velX = 0f
            
            currentEnergy = getEnergyCapacityForLevel(state.energyLevel)
            maxEnergy = currentEnergy
            
            cargoDirt = 0
            cargoStone = 0
            cargoGold = 0
            cargoRuby = 0
            cargoEmerald = 0
            cargoDiamond = 0
            cargoTreasure = 0
            
            currentDepthMeter = 0
            drillingCol = -1
            drillingRow = -1
            drillProgress = 0f
            
            isGameOver = false
            isPaused = false
            isDocked = false
            dockingMessage = ""
            
            isDivingActive = true
            currentScreen = "game_play"
            
            // Start primary coroutine engine loop
            startGameLoop()
        }
    }

    fun endDiveAndReturnToMenu() {
        isDivingActive = false
        gameLoopJob?.cancel()
        currentScreen = "main_menu"
    }

    fun pauseGame() {
        isPaused = true
    }

    fun resumeGame() {
        isPaused = false
    }

    private fun generateMap() {
        val rand = Random(System.currentTimeMillis())
        val newGrid = mutableListOf<List<BlockState>>()
        
        for (r in 0 until MAP_HEIGHT) {
            val rowBlocks = mutableListOf<BlockState>()
            for (c in 0 until MAP_WIDTH) {
                // Top sky/air layers Y < SURFACE_DEPTH
                if (r < SURFACE_DEPTH) {
                    // Create central Solar Docks Terminal
                    if (r == SURFACE_DEPTH - 2 && c >= 9 && c <= 12) {
                        rowBlocks.add(BlockState(c, r, BlockType.SURFACE_STATION, 0f, 0f))
                    } else if (r == SURFACE_DEPTH - 1 && c >= 9 && c <= 12) {
                        rowBlocks.add(BlockState(c, r, BlockType.SURFACE_STATION, 0f, 0f))
                    } else {
                        rowBlocks.add(BlockState(c, r, BlockType.AIR, 0f, 0f))
                    }
                    continue
                }
                
                // Borders bedrock
                if (c == 0 || c == MAP_WIDTH - 1 || r == MAP_HEIGHT - 1) {
                    rowBlocks.add(BlockState(c, r, BlockType.BEDROCK, Float.MAX_VALUE, Float.MAX_VALUE))
                    continue
                }

                // Normal layers distribution depending on depth (Y)
                val depthLevel = r - SURFACE_DEPTH
                var type = BlockType.DIRT
                
                // Procedural generation weights
                val dice = rand.nextFloat() * 100f
                
                if (depthLevel < 15) {
                    // Primitive layers: DIRT (85%), STONE (13%), GOLD (2%)
                    type = when {
                        dice < 2f -> BlockType.GOLD
                        dice < 15f -> BlockType.STONE
                        else -> BlockType.DIRT
                    }
                } else if (depthLevel < 40) {
                    // Medium layers: DIRT (45%), STONE (45%), GOLD (8%), RUBY (2%)
                    type = when {
                        dice < 2f -> BlockType.RUBY
                        dice < 10f -> BlockType.GOLD
                        dice < 55f -> BlockType.STONE
                        else -> BlockType.DIRT
                    }
                } else if (depthLevel < 80) {
                    // Abyssal granite layers: STONE (70%), DIRT (15%), GOLD (8%), RUBY (5%), EMERALD (2%)
                    type = when {
                        dice < 2f -> BlockType.EMERALD
                        dice < 7f -> BlockType.RUBY
                        dice < 15f -> BlockType.GOLD
                        dice < 85f -> BlockType.STONE
                        else -> BlockType.DIRT
                    }
                } else if (depthLevel < 120) {
                    // Diamond depths: STONE (75%), GOLD (5%), RUBY (7%), EMERALD (8%), DIAMOND (4%), TREASURE (1%)
                    type = when {
                        dice < 1f -> BlockType.TREASURE
                        dice < 5f -> BlockType.DIAMOND
                        dice < 13f -> BlockType.EMERALD
                        dice < 20f -> BlockType.RUBY
                        dice < 25f -> BlockType.GOLD
                        else -> BlockType.STONE
                    }
                } else {
                    // Dark ancient crystal caverns: STONE (60%), EMERALD (10%), DIAMOND (12%), TREASURE (5%), AIR Caves (13%)
                    type = when {
                        dice < 5f -> BlockType.TREASURE
                        dice < 17f -> BlockType.DIAMOND
                        dice < 27f -> BlockType.EMERALD
                        dice < 40f -> BlockType.AIR // Creates hollow cavities
                        else -> BlockType.STONE
                    }
                }

                val durability = type.baseDurability
                rowBlocks.add(BlockState(c, r, type, durability, durability))
            }
            newGrid.add(rowBlocks)
        }
        gameGrid = newGrid
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            var alertTicks = 0
            
            while (isDivingActive) {
                if (!isPaused && !isGameOver) {
                    val now = System.currentTimeMillis()
                    val delta = (now - lastTime) / 1000f
                    lastTime = now

                    updateGamePhysics(delta)
                    
                    // Alarm for low fuel alert sound (every 3 seconds)
                    if (currentEnergy < maxEnergy * 0.25f && currentEnergy > 0f) {
                        alertTicks++
                        if (alertTicks >= 60) { // approx 3 seconds
                            AudioSynthesizer.playLowEnergy()
                            alertTicks = 0
                        }
                    } else {
                        alertTicks = 0
                    }
                    
                    // Tick rate: ~60fps (16ms)
                    delay(16)
                } else {
                    lastTime = System.currentTimeMillis()
                    delay(100)
                }
            }
        }
    }

    // Directional actions mapping from buttons
    var btnLeftActive = false
    var btnRightActive = false
    var btnUpActive = false
    var btnDownActive = false

    private fun updateGamePhysics(dt: Float) {
        val state = gameState.value ?: return
        val speedModifier = getMovementModifier(state.speedLevel)
        
        // Apply controls to velocities
        val accelX = 14f * speedModifier
        val accelY = 12f * speedModifier
        val gravity = 2.8f
        val friction = 0.82f // Retards speed automatically

        if (btnLeftActive) {
            velX -= accelX * dt
        } else if (btnRightActive) {
            velX += accelX * dt
        } else {
            velX *= friction
        }

        if (btnUpActive) {
            // High powered booster thruster
            velY -= accelY * dt
            currentEnergy -= 0.6f * dt // Flying fuel usage
        } else if (btnDownActive) {
            velY += accelY * dt
        } else {
            // Apply slight environment gravity downwards
            velY += gravity * dt
        }

        // Limit maximum velocities for stability
        val maxVel = 8f
        velX = velX.coerceIn(-maxVel, maxVel)
        velY = velY.coerceIn(-maxVel, maxVel)

        // Apply velocities to future positions of player
        var nextX = playerX + velX * dt
        var nextY = playerY + velY * dt

        // Player bounding radius/size box (collision size offset)
        val rSize = 0.35f 

        // Boundary limitations
        if (nextX - rSize < 1f) {
            nextX = 1f + rSize
            velX = 0f
        }
        if (nextX + rSize > MAP_WIDTH - 1f) {
            nextX = MAP_WIDTH - 1f - rSize
            velX = 0f
        }
        if (nextY - rSize < 0f) {
            nextY = rSize
            velY = 0f
        }

        // Collision Checks with block tiles
        // Calculate player bounding corners
        val leftCol = (nextX - rSize).toInt()
        val rightCol = (nextX + rSize).toInt()
        val topRow = (nextY - rSize).toInt()
        val bottomRow = (nextY + rSize).toInt()

        // 1. Horizontal Resolution
        if (velX < 0) {
            // Moving Left. Check left side blocks.
            if (isSolidBlock(leftCol, topRow) || isSolidBlock(leftCol, bottomRow)) {
                nextX = (leftCol + 1) + rSize + 0.01f
                velX = 0f
                // Initiate digging to Left
                drillBlock(leftCol, (playerY).toInt(), dt)
            }
        } else if (velX > 0) {
            // Moving Right. Check right side blocks.
            if (isSolidBlock(rightCol, topRow) || isSolidBlock(rightCol, bottomRow)) {
                nextX = (rightCol) - rSize - 0.01f
                velX = 0f
                // Initiate digging to Right
                drillBlock(rightCol, (playerY).toInt(), dt)
            }
        }

        // 2. Vertical Resolution
        if (velY < 0) {
            // Moving Up/Thrusting. Check roof blocks.
            if (isSolidBlock(leftCol, topRow) || isSolidBlock(rightCol, topRow)) {
                nextY = (topRow + 1) + rSize + 0.01f
                velY = 0f
                // Drill upwards
                drillBlock((playerX).toInt(), topRow, dt)
            }
        } else if (velY > 0) {
            // Falling/Moving Down. Check floor blocks.
            if (isSolidBlock(leftCol, bottomRow) || isSolidBlock(rightCol, bottomRow)) {
                nextY = (bottomRow) - rSize - 0.01f
                velY = 0f
                // Drill downwards
                drillBlock((playerX).toInt(), bottomRow, dt)
            }
        }

        // Apply updated coordinates
        playerX = nextX
        playerY = nextY
        
        // Depth Meter (Surface depth is 5, so depth starting below is 5+)
        val actualRow = playerY.toInt()
        currentDepthMeter = (actualRow - SURFACE_DEPTH).coerceAtLeast(0)

        // General energy consumption (standing is 0.05 per sec, thruster was processed)
        currentEnergy -= 0.12f * dt
        if (currentEnergy <= 0f) {
            currentEnergy = 0f
            triggerGameOver()
        }

        // Safe port dock operations
        checkDockRefuelTerminal(dt)
    }

    private fun isSolidBlock(col: Int, row: Int): Boolean {
        if (col < 0 || col >= MAP_WIDTH) return true
        if (row < 0) return false
        if (row >= MAP_HEIGHT) return true
        
        val gridRows = gameGrid
        if (row >= gridRows.size) return true
        val block = gridRows[row][col]
        return block.type != BlockType.AIR && block.type != BlockType.SURFACE_STATION
    }

    private fun drillBlock(col: Int, row: Int, dt: Float) {
        val state = gameState.value ?: return
        if (col < 0 || col >= MAP_WIDTH || row < SURFACE_DEPTH || row >= MAP_HEIGHT) return
        
        val block = gameGrid[row][col]
        if (block.type == BlockType.AIR || block.type == BlockType.BEDROCK) return

        // Set actively drilled coordinates for tracking hud/crack representation
        drillingCol = col
        drillingRow = row

        val drillMultiplier = getDrillMultiplier(state.drillLevel)
        // Tick drill
        block.currentHealth -= 14f * drillMultiplier * dt
        AudioSynthesizer.playDigProgress()

        // Energy consumption during active excavation
        currentEnergy -= 1.8f * dt

        if (block.currentHealth <= 0f) {
            // Block successfully mined/shattered
            breakBlock(col, row, block.type)
        }
    }

    private fun breakBlock(col: Int, row: Int, type: BlockType) {
        // Clear block
        replaceBlockInGrid(col, row, BlockType.AIR)
        drillingCol = -1
        drillingRow = -1
        AudioSynthesizer.playDigBreak()

        // Gather resources
        val state = gameState.value ?: return
        val maxCargo = getCargoMaxForLevel(state.cargoLevel)
        
        if (getCargoWeight() < maxCargo) {
            // Pick up ore
            var gemFound = false
            when (type) {
                BlockType.DIRT -> {
                    cargoDirt++
                }
                BlockType.STONE -> {
                    cargoStone++
                }
                BlockType.GOLD -> {
                    cargoGold++
                    gemFound = true
                }
                BlockType.RUBY -> {
                    cargoRuby++
                    gemFound = true
                }
                BlockType.EMERALD -> {
                    cargoEmerald++
                    gemFound = true
                }
                BlockType.DIAMOND -> {
                    cargoDiamond++
                    gemFound = true
                    viewModelScope.launch {
                        repository.unlockAchievement("diamond_hand")
                    }
                }
                BlockType.TREASURE -> {
                    cargoTreasure++
                    gemFound = true
                    viewModelScope.launch {
                        repository.unlockAchievement("treasured")
                    }
                }
                else -> {}
            }
            if (gemFound) {
                AudioSynthesizer.playGemCollected()
            }
        }
        
        // Track first excavation achievement
        viewModelScope.launch {
            repository.unlockAchievement("first_dig")
        }
    }

    private fun replaceBlockInGrid(col: Int, row: Int, type: BlockType) {
        val rows = gameGrid.toMutableList()
        val specRow = rows[row].toMutableList()
        specRow[col] = BlockState(col, row, type, type.baseDurability, type.baseDurability)
        rows[row] = specRow
        gameGrid = rows
    }

    private fun checkDockRefuelTerminal(dt: Float) {
        // Safe terminal: middle columns 9 to 12 at depth row Y = 3 & 4.
        val playerCol = playerX.toInt()
        val playerRow = playerY.toInt()

        if (playerCol in 9..12 && playerRow <= 4) {
            isDocked = true
            
            // Initiate Selling & Refueling
            val cargoCount = getCargoWeight()
            if (cargoCount > 0) {
                dockAndRecuperateCargo()
            } else {
                dockingMessage = "Port Terminal: Cargo Empty. Solar Batteries Charging..."
            }
            
            // Charges full energy
            if (currentEnergy < maxEnergy) {
                currentEnergy = (currentEnergy + 65f * dt).coerceAtMost(maxEnergy)
            }
        } else {
            isDocked = false
            dockingMessage = ""
        }
    }

    private fun dockAndRecuperateCargo() {
        val state = gameState.value ?: return
        
        // Calculate earnings!
        val earnings = (cargoDirt * BlockType.DIRT.sellPrice) +
                (cargoStone * BlockType.STONE.sellPrice) +
                (cargoGold * BlockType.GOLD.sellPrice) +
                (cargoRuby * BlockType.RUBY.sellPrice) +
                (cargoEmerald * BlockType.EMERALD.sellPrice) +
                (cargoDiamond * BlockType.DIAMOND.sellPrice) +
                (cargoTreasure * BlockType.TREASURE.sellPrice)

        if (earnings > 0) {
            val prevGold = cargoGold
            val totalEarned = state.coins + earnings
            
            // Clear current dive inventory
            cargoDirt = 0
            cargoStone = 0
            cargoGold = 0
            cargoRuby = 0
            cargoEmerald = 0
            cargoDiamond = 0
            cargoTreasure = 0

            dockingMessage = "Port Terminal: Cargo Unloaded! Earned +$earnings Coins!"
            AudioSynthesizer.playSell()

            // Update database state
            viewModelScope.launch {
                val newState = state.copy(
                    coins = totalEarned,
                    totalCoinsEarned = state.totalCoinsEarned + earnings,
                    maxDepthReached = state.maxDepthReached.coerceAtLeast(currentDepthMeter)
                )
                repository.saveGameState(newState)

                // High score logic (insert high score if meaningful)
                if (currentDepthMeter > 0) {
                    repository.insertHighScore(HighScore(depth = currentDepthMeter, coinsEarned = earnings))
                }

                // Achievements progress
                repository.updateAchievementProgress("deep_50", currentDepthMeter)
                repository.updateAchievementProgress("deep_150", currentDepthMeter)
                
                // Gold streak achievement
                if (newState.totalCoinsEarned >= 2000) {
                    repository.unlockAchievement("gold_rush")
                }
            }
        }
    }

    private fun triggerGameOver() {
        isGameOver = true
        isDivingActive = false
        gameLoopJob?.cancel()

        // Penalize: Player loses cargo and 15% of their active cash in deep wreckage extraction!
        viewModelScope.launch {
            val state = repository.getGameStateSync() ?: return@launch
            val penalty = (state.coins * 0.15f).toInt()
            val finalCoins = (state.coins - penalty).coerceAtLeast(0)

            val finalState = state.copy(
                coins = finalCoins,
                maxDepthReached = state.maxDepthReached.coerceAtLeast(currentDepthMeter)
            )
            repository.saveGameState(finalState)
            
            endReportCoinsEarned = 0 // Game over, no profit from this dive!
            endReportDepthReached = currentDepthMeter

            if (currentDepthMeter > 0) {
                repository.insertHighScore(HighScore(depth = currentDepthMeter, coinsEarned = 0))
            }
        }
    }
}
