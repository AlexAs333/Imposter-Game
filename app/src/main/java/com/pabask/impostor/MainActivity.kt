package com.pabask.impostor

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pabask.impostor.ui.GameViewModel
import com.pabask.impostor.ui.screens.GameScreen
import com.pabask.impostor.ui.screens.SetupScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // --- CONFIGURACIÓN VISUAL BARRA DE ESTADO ---
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    val darkBackground = Color(0xFF0F111A).toArgb()
                    window.statusBarColor = darkBackground
                    window.navigationBarColor = darkBackground
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = false
                    insetsController.isAppearanceLightNavigationBars = false
                }
            }
            // --------------------------------------------

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: GameViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    if (uiState.isGameStarted) {
                        GameScreen(viewModel = viewModel)
                    } else {
                        SetupScreen(
                            availablePacks = viewModel.getAvailablePacks(),
                            initialPlayers = viewModel.getSavedPlayers(),
                            initialPackId = viewModel.getSavedPackId(),
                            initialShowCategory = viewModel.getSavedShowCategory(),

                            // 1. INICIAR JUEGO
                            onStartGame = { names, impostors, packs, showCat ->
                                viewModel.startGame(names, impostors, packs, showCat)
                            },

                            // 2. CREAR Y AÑADIR
                            onCreatePack = { name ->
                                viewModel.createNewPack(name)
                            },
                            onAddWord = { pack, word, cat ->
                                viewModel.addWordToPack(pack, word, cat)
                            },

                            // 3. BORRAR (¡ESTO ES LO QUE FALTABA!)
                            onDeletePack = { pack ->
                                viewModel.deletePack(pack)
                            },
                            onDeleteWord = { pack, word ->
                                viewModel.deleteWordFromPack(pack, word)
                            }
                        )
                    }
                }
            }
        }
    }
}