package com.pabask.impostor.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pabask.impostor.model.Role
import com.pabask.impostor.ui.GameViewModel
import com.pabask.impostor.ui.GameWinner
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// --- PALETA DE COLORES ---
private val DarkBackground = Color(0xFF0F111A)
private val CardBackground = Color(0xFF1E202B)
private val AccentGreen = Color(0xFF00C853)
private val AccentRed = Color(0xFFFF5252)
private val AccentBlue = Color(0xFF448AFF)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFAAAAAA)

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val players = uiState.players
    val startingPlayerName = uiState.startingPlayerName
    val showCategory = uiState.showCategory

    var currentPlayerIndex by remember { mutableIntStateOf(0) }

    // Controla si el botón de siguiente debe aparecer
    var showNextButton by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (currentPlayerIndex >= players.size) {
            VotingPhase(
                players = players,
                startingPlayerName = startingPlayerName,
                viewModel = viewModel,
                onRestart = { viewModel.stopGame() }
            )
        } else {
            val currentPlayer = players[currentPlayerIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // HEADER
                Text(
                    text = "TURNO ${currentPlayerIndex + 1}/${players.size}",
                    color = TextGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Usamos 'key' para reiniciar el estado de la carta al cambiar de jugador
                key(currentPlayerIndex) {
                    PeekCardContainer(
                        onPeekThresholdReached = {
                            // Solo activamos el botón si no estaba activo ya
                            if (!showNextButton) showNextButton = true
                        },

                        // 1. CARTA DE FONDO (EL SECRETO)
                        secretContent = {
                            val cardColor = if (currentPlayer.role == Role.IMPOSTOR) AccentRed else AccentBlue

                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor, contentColor = TextWhite)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // CATEGORÍA (Si está activada)
                                    if (showCategory && currentPlayer.category.isNotBlank()) {
                                        Text(
                                            text = currentPlayer.category.uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextWhite.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    Text(
                                        "TU PALABRA ES:",
                                        style = MaterialTheme.typography.labelLarge,
                                        letterSpacing = 2.sp,
                                        color = TextWhite.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))

                                    // TEXTO GIGANTE
                                    Text(
                                        currentPlayer.secretWord.uppercase(),
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        color = TextWhite
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))
                                    val roleText = if (currentPlayer.role == Role.IMPOSTOR)
                                        "Eres el IMPOSTOR" else "Eres CIVIL"
                                    Text(
                                        roleText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        },

                        // 2. TAPA (ARRIBA)
                        coverContent = {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(10.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground, contentColor = TextWhite)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = TextGray.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Text(
                                        "Pásale el móvil a:",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextGray
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        currentPlayer.name,
                                        style = MaterialTheme.typography.displayLarge,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.height(64.dp))

                                    // Indicador "Levanta para ver"
                                    PeekIndicator()
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // BOTÓN DE SIGUIENTE (Solo aparece si ya has mirado la carta)
                Button(
                    onClick = {
                        showNextButton = false
                        currentPlayerIndex++
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .alpha(if (showNextButton) 1f else 0f), // Fade in/out
                    enabled = showNextButton,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = TextWhite
                    )
                ) {
                    Text(
                        text = "TERMINAR TURNO",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!showNextButton) {
                    Text(
                        "Mantén pulsado y desliza hacia arriba para mirar.",
                        color = TextGray.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

// --- LÓGICA DE GESTO "PEEK" ---
@Composable
fun PeekCardContainer(
    onPeekThresholdReached: () -> Unit,
    coverContent: @Composable () -> Unit,
    secretContent: @Composable () -> Unit
) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Umbral para considerar que la carta ha sido "leída"
    val peekThresholdPx = with(density) { 180.dp.toPx() }

    var hapticTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. FONDO
        Box(modifier = Modifier.matchParentSize()) {
            secretContent()
        }

        // 2. TAPA MÓVIL
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                // REBOTE MÁS LEVE (LowBouncy)
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                                hapticTriggered = false
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newOffset = (offsetY.value + dragAmount).coerceAtMost(0f)
                                offsetY.snapTo(newOffset)

                                if (newOffset < -peekThresholdPx && !hapticTriggered) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hapticTriggered = true
                                    onPeekThresholdReached()
                                }
                            }
                        }
                    )
                }
        ) {
            coverContent()
        }
    }
}

@Composable
fun PeekIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "peek")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "peekMove"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { translationY = dy }
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "MIRAR",
            color = AccentGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// --- FASES DE VOTACIÓN ---
@Composable
fun VotingPhase(
    players: List<com.pabask.impostor.model.Player>,
    startingPlayerName: String,
    viewModel: GameViewModel,
    onRestart: () -> Unit
) {
    var playerToVote by remember { mutableStateOf<com.pabask.impostor.model.Player?>(null) }
    var eliminationResult by remember { mutableStateOf<Pair<com.pabask.impostor.model.Player, GameWinner>?>(null) }
    var gameWinner by remember { mutableStateOf(GameWinner.NONE) }

    if (gameWinner != GameWinner.NONE) {
        GameOverScreen(winner = gameWinner, onRestart = onRestart)
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("EMPIEZA EL DEBATE:", color = TextGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    startingPlayerName.uppercase(),
                    color = Color.Yellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
            }
        }

        Text("VOTACIÓN", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AccentRed, letterSpacing = 2.sp)
        Text("Selecciona a quién eliminar", color = TextGray)
        Spacer(modifier = Modifier.height(16.dp))

        val alivePlayers = players.filter { it.isAlive }
        alivePlayers.forEach { player ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { playerToVote = player },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(player.name, style = MaterialTheme.typography.titleLarge, color = TextWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (playerToVote != null) {
        AlertDialog(
            onDismissRequest = { playerToVote = null },
            containerColor = DarkBackground,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = { Text("¿Eliminar a ${playerToVote?.name}?") },
            text = { Text("Si fallas, los Impostores tendrán ventaja.") },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = AccentRed), onClick = {
                    val p = playerToVote!!
                    val result = viewModel.eliminatePlayer(p)
                    eliminationResult = p to result
                    playerToVote = null
                }) { Text("Eliminar", color = TextWhite, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { playerToVote = null }) { Text("Cancelar", color = TextGray) }
            }
        )
    }

    if (eliminationResult != null) {
        val (eliminatedPlayer, winnerState) = eliminationResult!!
        val isImpostor = eliminatedPlayer.role == Role.IMPOSTOR
        AlertDialog(
            onDismissRequest = { },
            containerColor = if (isImpostor) AccentBlue else AccentRed,
            titleContentColor = TextWhite,
            textContentColor = TextWhite,
            title = { Text(text = if (isImpostor) "¡ERA EL IMPOSTOR!" else "¡ERROR!", fontWeight = FontWeight.Black, fontSize = 24.sp) },
            text = {
                Column {
                    Text(text = "${eliminatedPlayer.name} era...", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = if (isImpostor) "EL IMPOSTOR" else "UN CIVIL", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = TextWhite, contentColor = Color.Black),
                    onClick = {
                        eliminationResult = null
                        if (winnerState != GameWinner.NONE) gameWinner = winnerState
                    }
                ) { Text(if (winnerState != GameWinner.NONE) "Ver Resultado" else "Seguir Jugando", fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
fun GameOverScreen(winner: GameWinner, onRestart: () -> Unit) {
    val isCivilianWin = winner == GameWinner.CIVILIANS
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isCivilianWin) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isCivilianWin) AccentGreen else AccentRed,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = if (isCivilianWin) "¡GANAN LOS CIVILES!" else "¡GANAN LOS IMPOSTORES!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = TextWhite, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = if (isCivilianWin) "Todos los infiltrados han sido eliminados." else "Los impostores han dominado la partida.", textAlign = TextAlign.Center, color = TextGray, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isCivilianWin) AccentGreen else AccentRed)
        ) {
            Icon(Icons.Default.Refresh, null, tint = TextWhite)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Volver al Menú", fontSize = 18.sp, color = TextWhite, fontWeight = FontWeight.Bold)
        }
    }
}