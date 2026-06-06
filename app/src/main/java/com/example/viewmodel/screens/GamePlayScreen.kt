package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.BlockType
import com.example.ui.theme.*
import kotlin.math.sin

@Composable
fun GamePlayScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    val state by viewModel.gameState.collectAsState()
    
    // Animation transitions for dynamic features
    val infiniteTransition = rememberInfiniteTransition("game_visuals")
    val drillAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drill_spin"
    )

    val flamePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CaveDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // HUD Top Panel
            GamePlayHud(viewModel = viewModel)

            // Game Playfield Rendering Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                GamePlayCanvas(
                    viewModel = viewModel,
                    drillAngle = drillAngle,
                    flamePulse = flamePulse
                )

                // Docked status overlay
                if (viewModel.isDocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .background(OreEmerald.copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = viewModel.dockingMessage,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Low Fuel alert
                val fuelRatio = viewModel.currentEnergy / viewModel.maxEnergy
                if (fuelRatio < 0.25f && !viewModel.isGameOver) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 50.dp)
                            .background(EnergyRed.copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚠ BATTERY LOW: Refuel at Surface Port!",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // On-screen control pad buttons
            GameControlsPad(viewModel = viewModel)
        }

        // Pause Menu Dialog Overlay
        if (viewModel.isPaused) {
            GamePauseDialog(viewModel = viewModel, onExit = onExit)
        }

        // Game Over Dialog Overlay
        if (viewModel.isGameOver) {
            GameOverDialog(viewModel = viewModel)
        }
    }
}

@Composable
fun GamePlayHud(viewModel: GameViewModel) {
    val state by viewModel.gameState.collectAsState()
    val fuelRatio = (viewModel.currentEnergy / viewModel.maxEnergy).coerceIn(0f, 1f)
    val maxCargo = state?.let { viewModel.getCargoMaxForLevel(it.cargoLevel) } ?: 15
    val currentCargo = viewModel.getCargoWeight()
    
    val cargoRatio = (currentCargo.toFloat() / maxCargo.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaveGrey)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Battery/Fuel indicator
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (fuelRatio < 0.25f) EnergyRed else EnergyGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RESERVE POWER: ${(fuelRatio * 100).toInt()}%",
                    color = if (fuelRatio < 0.25f) EnergyRed else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { fuelRatio },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    fuelRatio < 0.25f -> EnergyRed
                    fuelRatio < 0.5f -> EnergyYellow
                    else -> EnergyGreen
                },
                trackColor = CaveDark
            )
        }

        // Depth and Wealth Stats
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "DEPTH",
                fontSize = 9.sp,
                color = SteelGray,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${viewModel.currentDepthMeter}m",
                color = OreDiamond,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp
            )
        }

        // Cargo hold status
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CARGO: $currentCargo/$maxCargo",
                    fontSize = 10.sp,
                    color = if (currentCargo >= maxCargo) EnergyRed else Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = if (currentCargo >= maxCargo) EnergyRed else OreDiamond,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { cargoRatio },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (currentCargo >= maxCargo) EnergyRed else OreDiamond,
                trackColor = CaveDark
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Pause trigger
        IconButton(
            onClick = { viewModel.pauseGame() },
            modifier = Modifier
                .size(36.dp)
                .testTag("pause_trigger_button")
        ) {
            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
        }
    }
}

@Composable
fun GamePlayCanvas(
    viewModel: GameViewModel,
    drillAngle: Float,
    flamePulse: Float
) {
    val state = viewModel.gameState.collectAsState().value ?: return
    val grid = viewModel.gameGrid
    if (grid.isEmpty()) return

    val detectorLevel = state.detectorLevel

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Allows tapping on blocks if desired
            }
    ) {
        val viewWidth = size.width
        val viewHeight = size.height

        // Calculate dynamic block sizes. Fit 8-10 blocks on screen based on landscape/portrait layout size
        val colsOnScreen = 8.5f
        val blockSize = viewWidth / colsOnScreen
        
        // Compute active camera positions. We center the player on the Canvas.
        val camX = (viewModel.playerX * blockSize) - (viewWidth / 2f)
        val camY = (viewModel.playerY * blockSize) - (viewHeight / 2f)

        // Draw Sky/Cloud gradients above depth Y = 5
        // Sky depth goes from Y=0 to 5
        val skyBottomY = 5 * blockSize - camY
        if (skyBottomY > 0f) {
            drawRect(
                color = Color(0xFF1B2A4A),
                topLeft = Offset(0f, 0f),
                size = Size(viewWidth, skyBottomY.coerceAtMost(viewHeight))
            )
            // Solar Port hangar layout background lines
            drawRect(
                color = Color(0xFF101622),
                topLeft = Offset(0f, skyBottomY - 14f),
                size = Size(viewWidth, 14f)
            )
        }

        // Draw grid blocks within camera viewport
        val startCol = (camX / blockSize).toInt().coerceIn(0, com.example.viewmodel.MAP_WIDTH - 1)
        val endCol = ((camX + viewWidth) / blockSize).toInt().coerceIn(0, com.example.viewmodel.MAP_WIDTH - 1)
        val startRow = (camY / blockSize).toInt().coerceIn(0, com.example.viewmodel.MAP_HEIGHT - 1)
        val endRow = ((camY + viewHeight) / blockSize).toInt().coerceIn(0, com.example.viewmodel.MAP_HEIGHT - 1)

        for (r in startRow..endRow) {
            for (c in startCol..endCol) {
                val block = grid[r][c]
                if (block.type == BlockType.AIR) continue

                val bx = c * blockSize - camX
                val by = r * blockSize - camY

                when (block.type) {
                    BlockType.SURFACE_STATION -> {
                        // Drawing hangar terminal
                        drawRect(
                            color = CaveGrey,
                            topLeft = Offset(bx, by),
                            size = Size(blockSize, blockSize)
                        )
                        drawRect(
                            color = OreGold,
                            topLeft = Offset(bx + 4f, by + 4f),
                            size = Size(blockSize - 8f, blockSize - 8f),
                            style = Stroke(width = 3f)
                        )
                        // Refuel connection cable marker icon
                        drawLine(
                            color = OreGold,
                            start = Offset(bx + blockSize / 2f, by),
                            end = Offset(bx + blockSize / 2f, by + blockSize),
                            strokeWidth = 2f
                        )
                    }
                    BlockType.DIRT -> {
                        drawRect(
                            color = DirtBrown,
                            topLeft = Offset(bx + 1f, by + 1f),
                            size = Size(blockSize - 2f, blockSize - 2f)
                        )
                        // Add some dirt grains textures
                        drawCircle(color = Color(0xFF532E14), radius = blockSize * 0.08f, center = Offset(bx + blockSize * 0.3f, by + blockSize * 0.4f))
                        drawCircle(color = Color(0xFF532E14), radius = blockSize * 0.05f, center = Offset(bx + blockSize * 0.7f, by + blockSize * 0.6f))
                    }
                    BlockType.STONE -> {
                        drawRect(
                            color = StoneGrey,
                            topLeft = Offset(bx + 1f, by + 1f),
                            size = Size(blockSize - 2f, blockSize - 2f)
                        )
                        // Granite veins textures
                        drawLine(color = Color(0xFF43454B), start = Offset(bx + 4f, by + 4f), end = Offset(bx + blockSize - 4f, by + blockSize - 4f), strokeWidth = 3f)
                        drawLine(color = Color(0xFF43454B), start = Offset(bx + blockSize - 4f, by + 4f), end = Offset(bx + 4f, by + blockSize - 4f), strokeWidth = 2f)
                    }
                    BlockType.GOLD -> {
                        drawRect(color = DirtBrown, topLeft = Offset(bx + 1f, by + 1f), size = Size(blockSize - 2f, blockSize - 2f))
                        // Sparkling nuggets
                        drawCircle(color = OreGold, radius = blockSize * 0.2f, center = Offset(bx + blockSize * 0.5f, by + blockSize * 0.5f))
                        drawCircle(color = OreGold, radius = blockSize * 0.1f, center = Offset(bx + blockSize * 0.25f, by + blockSize * 0.3f))
                        drawCircle(color = OreGold, radius = blockSize * 0.12f, center = Offset(bx + blockSize * 0.75f, by + blockSize * 0.7f))
                    }
                    BlockType.RUBY -> {
                        drawRect(color = StoneGrey, topLeft = Offset(bx + 1f, by + 1f), size = Size(blockSize - 2f, blockSize - 2f))
                        // Emerald shapes
                        val path = Path().apply {
                            moveTo(bx + blockSize * 0.5f, by + blockSize * 0.2f)
                            lineTo(bx + blockSize * 0.8f, by + blockSize * 0.5f)
                            lineTo(bx + blockSize * 0.5f, by + blockSize * 0.8f)
                            lineTo(bx + blockSize * 0.2f, by + blockSize * 0.5f)
                            close()
                        }
                        drawPath(path = path, color = OreRuby)
                    }
                    BlockType.EMERALD -> {
                        drawRect(color = StoneGrey, topLeft = Offset(bx + 1f, by + 1f), size = Size(blockSize - 2f, blockSize - 2f))
                        // Hexagon structure
                        val path = Path().apply {
                            moveTo(bx + blockSize * 0.5f, by + blockSize * 0.15f)
                            lineTo(bx + blockSize * 0.85f, by + blockSize * 0.35f)
                            lineTo(bx + blockSize * 0.85f, by + blockSize * 0.75f)
                            lineTo(bx + blockSize * 0.5f, by + blockSize * 0.9f)
                            lineTo(bx + blockSize * 0.15f, by + blockSize * 0.75f)
                            lineTo(bx + blockSize * 0.15f, by + blockSize * 0.35f)
                            close()
                        }
                        drawPath(path = path, color = OreEmerald)
                    }
                    BlockType.DIAMOND -> {
                        drawRect(color = StoneGrey, topLeft = Offset(bx + 1f, by + 1f), size = Size(blockSize - 2f, blockSize - 2f))
                        // Diamond structures
                        val path = Path().apply {
                            moveTo(bx + blockSize * 0.5f, by + blockSize * 0.15f)
                            lineTo(bx + blockSize * 0.85f, by + blockSize * 0.45f)
                            lineTo(bx + blockSize * 0.5f, by + blockSize * 0.85f)
                            lineTo(bx + blockSize * 0.15f, by + blockSize * 0.45f)
                            close()
                        }
                        drawPath(path = path, color = OreDiamond)
                    }
                    BlockType.TREASURE -> {
                        // Relic Gilded box design
                        drawRect(color = Color(0xFF1E0E35), topLeft = Offset(bx + 1f, by + 1f), size = Size(blockSize - 2f, blockSize - 2f))
                        drawRect(color = OreTreasure, topLeft = Offset(bx + blockSize * 0.2f, by + blockSize * 0.3f), size = Size(blockSize * 0.6f, blockSize * 0.5f))
                        drawRect(color = OreGold, topLeft = Offset(bx + blockSize * 0.43f, by + blockSize * 0.45f), size = Size(blockSize * 0.14f, blockSize * 0.2f))
                    }
                    BlockType.BEDROCK -> {
                        drawRect(
                            color = BedrockSlate,
                            topLeft = Offset(bx, by),
                            size = Size(blockSize, blockSize)
                        )
                        // Warning diagonal rivets
                        drawLine(color = Color.Black, start = Offset(bx, by), end = Offset(bx + blockSize, by + blockSize), strokeWidth = 3f)
                        drawLine(color = Color.Black, start = Offset(bx + blockSize, by), end = Offset(bx, by + blockSize), strokeWidth = 3f)
                    }
                    else -> {}
                }

                // Treasure Sparkle Blinking logic (based on Detector Level vs Ores)
                var shouldBeacon = false
                val blockDepth = r - 5
                
                if (detectorLevel >= 2 && block.type == BlockType.GOLD) shouldBeacon = true
                if (detectorLevel >= 3 && block.type == BlockType.RUBY) shouldBeacon = true
                if (detectorLevel >= 4 && block.type == BlockType.EMERALD) shouldBeacon = true
                if (detectorLevel >= 5 && (block.type == BlockType.DIAMOND || block.type == BlockType.TREASURE)) shouldBeacon = true

                if (shouldBeacon) {
                    val cycle = sin(System.currentTimeMillis() / 200.0)
                    if (cycle > 0.1) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f),
                            radius = blockSize * 0.08f,
                            center = Offset(bx + blockSize * 0.85f, by + blockSize * 0.15f)
                        )
                    }
                }

                // Active drilling damage cracks representation
                if (viewModel.drillingCol == c && viewModel.drillingRow == r) {
                    val crackRatio = (block.maxHealth - block.currentHealth) / block.maxHealth
                    if (crackRatio > 0.1f) {
                        // Draw crack overlay lines
                        val center = Offset(bx + blockSize / 2f, by + blockSize / 2f)
                        val pointsCount = 6
                        for (i in 0 until pointsCount) {
                            val angle = i * (2 * Math.PI / pointsCount)
                            val len = (blockSize / 2f) * crackRatio
                            val px = center.x + Math.cos(angle).toFloat() * len
                            val py = center.y + Math.sin(angle).toFloat() * len
                            drawLine(
                                color = Color.Black.copy(alpha = 0.8f),
                                start = center,
                                end = Offset(px, py),
                                strokeWidth = 4f
                            )
                        }
                    }
                }
            }
        }

        // Draw Refueling / Parking hangar guides at surface
        // Grid col index 9..12, row index 3
        val terminalX = 9.5f * blockSize - camX
        val terminalY = 3.5f * blockSize - camY
        if (terminalX + 3 * blockSize > 0f && terminalY + blockSize > 0f) {
            drawRoundRect(
                color = OreEmerald.copy(alpha = 0.15f),
                topLeft = Offset(terminalX, terminalY),
                size = Size(blockSize * 3f, blockSize * 1.5f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = OreEmerald.copy(alpha = 0.3f),
                topLeft = Offset(terminalX, terminalY),
                size = Size(blockSize * 3f, blockSize * 1.5f),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 4f)
            )
            // Solar hose pad logo
            drawCircle(
                color = OreEmerald,
                radius = 12f,
                center = Offset(terminalX + blockSize * 1.5f, terminalY + blockSize * 0.65f)
            )
        }

        // Render drilling pod vehicle (Player)
        val px = viewModel.playerX * blockSize - camX
        val py = viewModel.playerY * blockSize - camY
        val pSize = blockSize * 0.72f // slightly smaller than block, matches collision bounding box

        drawPlayerBot(
            x = px,
            y = py,
            size = pSize,
            drillAngle = drillAngle,
            flamePulse = flamePulse,
            boosterActive = viewModel.btnUpActive,
            scope = this
        )
    }
}

private fun drawPlayerBot(
    x: Float,
    y: Float,
    size: Float,
    drillAngle: Float,
    flamePulse: Float,
    boosterActive: Boolean,
    scope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    scope.apply {
        // Compute layout center
        val cx = x
        val cy = y

        // Dynamic auxiliary thruster flame when moving up (counteracting gravity)
        if (boosterActive) {
            val flameWidth = size * 0.4f
            val flameHeight = size * 0.7f * flamePulse
            val fPath = Path().apply {
                moveTo(cx - flameWidth / 2f, cy + size * 0.4f)
                lineTo(cx, cy + size * 0.4f + flameHeight)
                lineTo(cx + flameWidth / 2f, cy + size * 0.4f)
                close()
            }
            drawPath(
                path = fPath,
                color = WarningOrange
            )
            // Inner core flame
            val innerFPath = Path().apply {
                moveTo(cx - flameWidth * 0.25f, cy + size * 0.4f)
                lineTo(cx, cy + size * 0.4f + flameHeight * 0.6f)
                lineTo(cx + flameWidth * 0.25f, cy + size * 0.4f)
                close()
            }
            drawPath(
                path = innerFPath,
                color = OreGold
            )
        }

        // 1. Pod Main Fuselage Chassis (Rugged Industrial Oval)
        drawCircle(
            color = CaveGrey,
            radius = size * 0.5f,
            center = Offset(cx, cy)
        )
        // Outer metallic heavy rim
        drawCircle(
            color = OreGold,
            radius = size * 0.5f,
            center = Offset(cx, cy),
            style = Stroke(width = size * 0.08f)
        )

        // 2. Cockpit Dome window (Cyan glowing visor)
        drawCircle(
            color = OreDiamond.copy(alpha = 0.85f),
            radius = size * 0.25f,
            center = Offset(cx, cy - size * 0.05f)
        )
        // Visor reflection gloss line
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = size * 0.25f,
            center = Offset(cx + size * 0.03f, cy - size * 0.08f),
            style = Stroke(width = size * 0.03f)
        )

        // 3. Side Thruster Exhaust Vent Wings
        drawRoundRect(
            color = SteelGray,
            topLeft = Offset(cx - size * 0.62f, cy - size * 0.1f),
            size = Size(size * 0.15f, size * 0.25f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = SteelGray,
            topLeft = Offset(cx + size * 0.47f, cy - size * 0.1f),
            size = Size(size * 0.15f, size * 0.25f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // 4. Heavy drill bit spinner (Rotates at bottom)
        withTransform({
            translate(cx, cy + size * 0.48f)
            rotate(drillAngle)
        }) {
            val dWidth = size * 0.65f
            val dHeight = size * 0.5f
            val drillPath = Path().apply {
                moveTo(-dWidth / 2f, 0f)
                lineTo(0f, dHeight)
                lineTo(dWidth / 2f, 0f)
                close()
            }
            drawPath(
                path = drillPath,
                color = SteelGray
            )
            // Heavy helical drill groves
            drawLine(
                color = Color.Black,
                start = Offset(-dWidth / 3f, dHeight * 0.25f),
                end = Offset(dWidth / 3f, dHeight * 0.25f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.Black,
                start = Offset(-dWidth / 5f, dHeight * 0.6f),
                end = Offset(dWidth / 5f, dHeight * 0.6f),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun GameControlsPad(viewModel: GameViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaveGrey)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Horizontal Movement arrows pad (LEFT & RIGHT)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CaveDark)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.btnLeftActive = true
                                tryAwaitRelease()
                                viewModel.btnLeftActive = false
                            }
                        )
                    }
                    .testTag("control_left_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Move Left",
                    tint = OreGold,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Right Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CaveDark)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.btnRightActive = true
                                tryAwaitRelease()
                                viewModel.btnRightActive = false
                            }
                        )
                    }
                    .testTag("control_right_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Move Right",
                    tint = OreGold,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // Vertical Gravity counter-thruster booster holds (UP arrow) & descend (DOWN arrow)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Down/Descend button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CaveDark)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.btnDownActive = true
                                tryAwaitRelease()
                                viewModel.btnDownActive = false
                            }
                        )
                    }
                    .testTag("control_down_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Descend Down",
                    tint = OreGold,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Thrust booster button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(brush = androidx.compose.ui.graphics.Brush.radialGradient(listOf(WarningOrange.copy(alpha = 0.5f), CaveDark), radius = 100f))
                    .border(2.dp, WarningOrange, shape = CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.btnUpActive = true
                                tryAwaitRelease()
                                viewModel.btnUpActive = false
                            }
                        )
                    }
                    .testTag("control_thrust_btn"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Fly Boost",
                        tint = WarningOrange,
                        modifier = Modifier.size(34.dp)
                    )
                    Text("THRUST", color = WarningOrange, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GamePauseDialog(
    viewModel: GameViewModel,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { viewModel.resumeGame() },
        containerColor = CaveGrey,
        confirmButton = {
            Button(
                onClick = { viewModel.resumeGame() },
                colors = ButtonDefaults.buttonColors(containerColor = OreGold)
            ) {
                Text("RESUME CORE", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.resumeGame()
                    viewModel.endDiveAndReturnToMenu()
                    onExit()
                }
            ) {
                Text("ABANDON & EXIT", color = EnergyRed)
            }
        },
        title = {
            Text(
                "SYSTEM PAUSED",
                color = OreGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Your drilling vessel is locked in stasis. Unclaimed cargo will be lost if you elect to abandon back to port.",
                color = Color.White,
                fontSize = 13.sp
            )
        }
    )
}

@Composable
fun GameOverDialog(viewModel: GameViewModel) {
    val depth = viewModel.endReportDepthReached
    val penaltyPercent = 15

    AlertDialog(
        onDismissRequest = {}, // Lock dialog
        containerColor = CaveGrey,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        confirmButton = {
            Button(
                onClick = {
                    viewModel.startNewDive()
                },
                colors = ButtonDefaults.buttonColors(containerColor = OreGold),
                modifier = Modifier.testTag("game_over_retry_button")
            ) {
                Text("PROVISION NEW RUN", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.endDiveAndReturnToMenu()
                },
                modifier = Modifier.testTag("game_over_exit_button")
            ) {
                Text("RETURN TO SURFACE PORT", color = OreDiamond)
            }
        },
        title = {
            Text(
                "⚡ DRILL VESSEL DESTROYED",
                color = EnergyRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "LITHIUM RESERVE EXHAUSTED Underground!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "A rescue probe recovered your chassis from a depth of ${depth}m. All cargo currently loaded has been scrapped! Wreckage recovery taxed $penaltyPercent% of your wallet value.",
                    color = SteelGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    )
}
