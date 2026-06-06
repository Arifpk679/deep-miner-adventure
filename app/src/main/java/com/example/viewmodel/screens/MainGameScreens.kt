package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.GameViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppHeader(
    title: String,
    coins: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaveGrey)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("back_button")
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Go Back",
                tint = OreGold
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(CaveDark, shape = RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = "Coins",
                tint = OreGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = coins.toString(),
                color = OreGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    coins: Int,
    onNavigate: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CaveDark)
    ) {
        // Starry/Ore background pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rand = Random(42)
            for (i in 0 until 40) {
                val x = rand.nextFloat() * size.width
                val y = rand.nextFloat() * size.height
                val r = rand.nextFloat() * 4f + 1f
                drawCircle(
                    color = if (rand.nextBoolean()) OreDiamond.copy(alpha = 0.2f) else OreGold.copy(alpha = 0.2f),
                    radius = r,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Game Logo Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(pulseScale)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(OreGold, Color.Transparent),
                                radius = 160f
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = "Drill",
                        tint = OreGold,
                        modifier = Modifier
                            .size(76.dp)
                            .rotate(45f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DEEP MINER",
                    color = OreGold,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ADVENTURE",
                    color = OreDiamond,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Quick Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CaveGrey, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, SteelGray.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CURRENT WALLET", fontSize = 10.sp, color = SteelGray, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.MonetizationOn, "Coins", tint = OreGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(coins.toString(), fontSize = 18.sp, color = OreGold, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(SteelGray.copy(alpha = 0.3f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RECORD DEPTH", fontSize = 10.sp, color = SteelGray, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.TrendingDown, "Depth", tint = OreDiamond, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val depth = viewModel.gameState.collectAsState().value?.maxDepthReached ?: 0
                        Text("${depth}m", fontSize = 18.sp, color = OreDiamond, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play Button
                Button(
                    onClick = { viewModel.startNewDive() },
                    colors = ButtonDefaults.buttonColors(containerColor = OreGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("play_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EMBARK DIVE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                // Daily Reward Banner
                if (viewModel.dailyRewardAvailable) {
                    Button(
                        onClick = { viewModel.claimDailyReward() },
                        colors = ButtonDefaults.buttonColors(containerColor = OreEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("daily_reward_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CLAIM DAILY BONUS (+200)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigate("shop") },
                        colors = ButtonDefaults.buttonColors(containerColor = CaveGrey),
                        border = BorderStroke(1.dp, OreDiamond.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("shop_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = OreDiamond, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SHOP", color = OreDiamond, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onNavigate("achievements") },
                        colors = ButtonDefaults.buttonColors(containerColor = CaveGrey),
                        border = BorderStroke(1.dp, OreRuby.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("achievements_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = OreRuby, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ACHIEVE", color = OreRuby, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigate("scores") },
                        colors = ButtonDefaults.buttonColors(containerColor = CaveGrey),
                        border = BorderStroke(1.dp, SteelGray.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("scores_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Leaderboard, null, tint = SteelGray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SCORES", color = SteelGray, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onNavigate("settings") },
                        colors = ButtonDefaults.buttonColors(containerColor = CaveGrey),
                        border = BorderStroke(1.dp, SteelGray.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Settings, null, tint = SteelGray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SETTINGS", color = SteelGray, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "v1.0.0 • Offline Play Enabled",
                fontSize = 11.sp,
                color = SteelGray.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    coins: Int,
    onBack: () -> Unit
) {
    val state by viewModel.gameState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CaveDark)
    ) {
        AppHeader(title = "EQUIPMENT SHOP", coins = coins, onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "UPGRADE VEHICLE SYSTEMS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SteelGray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            state?.let { s ->
                item {
                    UpgradeItemCard(
                        title = "Diamond-Tipped Drill",
                        description = "Increases block drilling speed",
                        currentLevel = s.drillLevel,
                        cost = viewModel.getUpgradeCost(s.drillLevel),
                        icon = Icons.Default.Construction,
                        accentColor = OreGold,
                        onUpgrade = { viewModel.purchaseUpgrade("drill") }
                    )
                }

                item {
                    UpgradeItemCard(
                        title = "Auxiliary Fuel Tank",
                        description = "Boosts maximum energy storage reserve",
                        currentLevel = s.energyLevel,
                        cost = viewModel.getUpgradeCost(s.energyLevel),
                        icon = Icons.Default.EvStation,
                        accentColor = EnergyGreen,
                        onUpgrade = { viewModel.purchaseUpgrade("energy") }
                    )
                }

                item {
                    UpgradeItemCard(
                        title = "Expanded Cargo Hold",
                        description = "Holds more mineral ore pieces in inventory",
                        currentLevel = s.cargoLevel,
                        cost = viewModel.getUpgradeCost(s.cargoLevel),
                        icon = Icons.Default.LocalShipping,
                        accentColor = OreDiamond,
                        onUpgrade = { viewModel.purchaseUpgrade("cargo") }
                    )
                }

                item {
                    UpgradeItemCard(
                        title = "Ore Spectrum Detector",
                        description = "Allows deep minerals to blink nearby",
                        currentLevel = s.detectorLevel,
                        cost = viewModel.getUpgradeCost(s.detectorLevel),
                        icon = Icons.Default.Troubleshoot,
                        accentColor = OreRuby,
                        onUpgrade = { viewModel.purchaseUpgrade("detector") }
                    )
                }

                item {
                    UpgradeItemCard(
                        title = "Aux Combustion Engine",
                        description = "Increases thruster and descent mobility speed",
                        currentLevel = s.speedLevel,
                        cost = viewModel.getUpgradeCost(s.speedLevel),
                        icon = Icons.Default.Speed,
                        accentColor = WarningOrange,
                        onUpgrade = { viewModel.purchaseUpgrade("speed") }
                    )
                }
            }
        }
    }
}

@Composable
fun UpgradeItemCard(
    title: String,
    description: String,
    currentLevel: Int,
    cost: Int,
    icon: ImageVector,
    accentColor: Color,
    onUpgrade: () -> Unit
) {
    val maxLevel = 5
    val isMaxed = currentLevel >= maxLevel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaveGrey, shape = RoundedCornerShape(16.dp))
            .border(1.dp, SteelGray.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Text(
                    text = description,
                    color = SteelGray,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                // Level ticks
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..maxLevel) {
                        val active = i <= currentLevel
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (active) accentColor else SteelGray.copy(alpha = 0.2f))
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMaxed) "MAX" else "Lv.$currentLevel",
                        color = if (isMaxed) accentColor else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isMaxed) {
            Button(
                onClick = {},
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = CaveDark,
                    disabledContentColor = SteelGray
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("MAX", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        } else {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = OreGold),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("UPGRADE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = cost.toString(),
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsScreen(
    viewModel: GameViewModel,
    coins: Int,
    onBack: () -> Unit
) {
    val achievementList by viewModel.achievements.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CaveDark)
    ) {
        AppHeader(title = "ACHIEVEMENTS", coins = coins, onBack = onBack)

        if (achievementList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Achievements empty",
                        tint = SteelGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No records found", color = SteelGray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(achievementList) { ach ->
                    val progressRatio = if (ach.maxProgress > 1) {
                        ach.progress.toFloat() / ach.maxProgress.toFloat()
                    } else {
                        if (ach.isUnlocked) 1f else 0f
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (ach.isUnlocked) CaveGrey else CaveGrey.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                1.dp,
                                if (ach.isUnlocked) OreGold.copy(alpha = 0.4f) else SteelGray.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (ach.isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (ach.isUnlocked) OreGold else SteelGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ach.title,
                                color = if (ach.isUnlocked) Color.White else SteelGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = ach.description,
                                color = SteelGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            if (ach.maxProgress > 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LinearProgressIndicator(
                                        progress = { progressRatio },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (ach.isUnlocked) OreEmerald else OreDiamond,
                                        trackColor = CaveDark
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${ach.progress}/${ach.maxProgress}",
                                        color = SteelGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighScoresScreen(
    viewModel: GameViewModel,
    coins: Int,
    onBack: () -> Unit
) {
    val scores by viewModel.highScores.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CaveDark)
    ) {
        AppHeader(title = "TOP DIVE RECORDS", coins = coins, onBack = onBack)

        if (scores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Scores empty",
                        tint = SteelGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No dive logs saved. Complete a run first!", color = SteelGray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 30.dp))
                }
            }
        } else {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scores.associateWith { scores.indexOf(it) + 1 }.toList()) { (score, rank) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CaveGrey, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, SteelGray.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$rank",
                                color = when (rank) {
                                    1 -> OreGold
                                    2 -> OreDiamond
                                    3 -> OreRuby
                                    else -> SteelGray
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                modifier = Modifier.width(36.dp)
                            )

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingDown, "Depth", tint = OreDiamond, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Depth: ${score.depth}m", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(
                                    text = format.format(Date(score.timestamp)),
                                    color = SteelGray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, "Profit", tint = OreGold, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${score.coinsEarned}",
                                    color = OreGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text("coins", color = SteelGray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    coins: Int,
    onBack: () -> Unit
) {
    val state by viewModel.gameState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CaveDark)
    ) {
        AppHeader(title = "SETTINGS", coins = coins, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "AUDIO PREFERENCES",
                color = SteelGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CaveGrey, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Sound Synthesizer Effects", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Chiptune drilling feedback & alerts", color = SteelGray, fontSize = 11.sp)
                }

                Switch(
                    checked = state?.soundEnabled ?: true,
                    onCheckedChange = { viewModel.toggleSound() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OreGold,
                        checkedTrackColor = OreGold.copy(alpha = 0.5f),
                        uncheckedThumbColor = SteelGray,
                        uncheckedTrackColor = CaveDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "DIVE CONTROLLER INFORMATION",
                color = SteelGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = CaveGrey),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "How to play:",
                        color = OreDiamond,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "• Use the on-screen left and right control arrows to move horizontally.\n" +
                        "• Hold thrust booster (UP Arrow) to counteract downward gravity.\n" +
                        "• Push adjacent to solid blocks to automatically activate the dig drill.\n" +
                        "• Watch your energy level carefully! If it drains to empty under the surface, you crash and lose a portion of coins.\n" +
                        "• Return to the surface and dock inside the Solar Port Terminal (X: 10 central pad Y: 3) to offload cargo, collect coins, and refuel fully.",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
