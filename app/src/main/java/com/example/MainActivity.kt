package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.DeepMinerTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            DeepMinerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val currentScreen = viewModel.currentScreen
                    val state by viewModel.gameState.collectAsState()
                    val coins = state?.coins ?: 0

                    when (currentScreen) {
                        "main_menu" -> {
                            MainMenuScreen(
                                viewModel = viewModel,
                                coins = coins,
                                onNavigate = { screen ->
                                    viewModel.currentScreen = screen
                                }
                            )
                        }
                        "game_play" -> {
                            GamePlayScreen(
                                viewModel = viewModel,
                                onExit = {
                                    viewModel.currentScreen = "main_menu"
                                }
                            )
                        }
                        "shop" -> {
                            ShopScreen(
                                viewModel = viewModel,
                                coins = coins,
                                onBack = {
                                    viewModel.currentScreen = "main_menu"
                                }
                            )
                        }
                        "achievements" -> {
                            AchievementsScreen(
                                viewModel = viewModel,
                                coins = coins,
                                onBack = {
                                    viewModel.currentScreen = "main_menu"
                                }
                            )
                        }
                        "scores" -> {
                            HighScoresScreen(
                                viewModel = viewModel,
                                coins = coins,
                                onBack = {
                                    viewModel.currentScreen = "main_menu"
                                }
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                coins = coins,
                                onBack = {
                                    viewModel.currentScreen = "main_menu"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
