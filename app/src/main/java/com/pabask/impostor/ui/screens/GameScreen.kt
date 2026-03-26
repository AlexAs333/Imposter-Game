package com.pabask.impostor.ui.screens

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.pabask.impostor.utils.QrCodeUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// COLORES
private val DarkBackground = Color(0xFF0F111A)
private val CardBackground = Color(0xFF1E202B)
private val AccentGreen = Color(0xFF00C853)
private val AccentRed = Color(0xFFFF5252)
private val AccentBlue = Color(0xFF448AFF)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFAAAAAA)

// --- ¡CAMBIA ESTO POR TU URL DE GITHUB PAGES! ---
private const val WEB_URL_BASE = "https://pabask.github.io/impostor-web/"

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val players = uiState.players
    val startingPlayerName = uiState.startingPlayerName
    val showCategory = uiState.showCategory
    val isQrMode = uiState.isQrMode // <--- LEEMOS EL MODO

    var currentPlayerIndex by remember { mutableIntStateOf(0) }
    var showNextButton by remember { mutableStateOf(false) } // Para modo clásico

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        if (currentPlayerIndex >= players.size) {
            VotingPhase(players, startingPlayerName, viewModel) { viewModel.stopGame() }
        } else {
            val currentPlayer = players[currentPlayerIndex]

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // HEADER
                Text(
                    text = "TURNO ${currentPlayerIndex + 1}/${players.size}",
                    color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(36.dp))

                // --- DECISIÓN DE MODO ---
                if (isQrMode) {
                    // MODO QR
                    QrDisplayCard(
                        player = currentPlayer,
                        onNext = { currentPlayerIndex++ }
                    )
                } else {
                    // MODO CLÁSICO (Peek & Pass)
                    key(currentPlayerIndex) {
                        PeekCardContainer(
                            onPeekThresholdReached = { if (!showNextButton) showNextButton = true },
                            secretContent = {
                                SecretCardContent(currentPlayer, showCategory)
                            },
                            coverContent = {
                                CoverCardContent(currentPlayer.name)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón para modo clásico
                    Button(
                        onClick = { showNextButton = false; currentPlayerIndex++ },
                        modifier = Modifier.fillMaxWidth().height(56.dp).alpha(if (showNextButton) 1f else 0f),
                        enabled = showNextButton,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = TextWhite)
                    ) {
                        Text("TERMINAR TURNO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!showNextButton) {
                        Text("Desliza para mirar. Suelta para ocultar.", color = TextGray.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}

// --- COMPONENTE CARTA QR ---
@Composable
fun QrDisplayCard(player: com.pabask.impostor.model.Player, onNext: () -> Unit) {
    // 1. Generamos el QR (Igual que antes)
    val qrBitmap = remember(player) {
        val rawData = "${player.secretWord}|${player.role}|${player.category}"
        val encodedData = Base64.encodeToString(rawData.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
        val finalUrl = "$WEB_URL_BASE?d=$encodedData"
        QrCodeUtils.generateQrBitmap(finalUrl)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- TARJETA PRINCIPAL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            // Fondo oscuro para que pegue con la app
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            // Borde azul neón para rollo tecnológico
            border = BorderStroke(2.dp, AccentBlue),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 32.dp, horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título del Jugador
                Icon(Icons.Default.Person, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = player.name.uppercase(),
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ESCANEA PARA DESCODIFICAR",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- EL QR (ENMARCADO) ---
                // Usamos una caja blanca redondeada para que el QR se lea bien
                // pero no ocupe toda la pantalla
                Box(
                    modifier = Modifier
                        .size(260.dp) // Tamaño fijo cuadrado
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(12.dp), // Margen interno
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    } else {
                        // Por si falla, que no se vea vacío
                        CircularProgressIndicator(color = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- BOTÓN SIGUIENTE ---
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen, // Verde para indicar "Listo"
                contentColor = TextWhite
            ),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "SIGUIENTE JUGADOR",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// --- COMPONENTES VISUALES CARTA CLÁSICA ---
@Composable
fun SecretCardContent(player: com.pabask.impostor.model.Player, showCategory: Boolean) {
    val cardColor = if (player.role == Role.IMPOSTOR) AccentRed else AccentBlue
    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = cardColor, contentColor = TextWhite)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (showCategory && player.category.isNotBlank()) {
                Text(player.category.uppercase(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.background(Color.Black.copy(0.2f), RoundedCornerShape(8.dp)).padding(12.dp, 4.dp))
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text("TU PALABRA ES:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Text(player.secretWord.uppercase(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Text(if (player.role == Role.IMPOSTOR) "Eres el IMPOSTOR" else "Eres CIVIL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CoverCardContent(playerName: String) {
    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(10.dp), colors = CardDefaults.cardColors(containerColor = CardBackground, contentColor = TextWhite)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.VisibilityOff, null, modifier = Modifier.size(80.dp), tint = TextGray.copy(0.5f))
            Spacer(modifier = Modifier.height(32.dp))
            Text("Pásale el móvil a:", style = MaterialTheme.typography.titleMedium, color = TextGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(playerName, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(64.dp))
            PeekIndicator()
        }
    }
}

// --- LOGICA ANIMACIÓN PEEK ---
@Composable
fun PeekCardContainer(onPeekThresholdReached: () -> Unit, coverContent: @Composable () -> Unit, secretContent: @Composable () -> Unit) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val peekThresholdPx = with(density) { 180.dp.toPx() }
    var hapticTriggered by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.matchParentSize()) { secretContent() }
        Box(
            modifier = Modifier.matchParentSize().offset { IntOffset(0, offsetY.value.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { scope.launch { offsetY.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)); hapticTriggered = false } },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newOffset = (offsetY.value + dragAmount).coerceAtMost(0f)
                                offsetY.snapTo(newOffset)
                                if (newOffset < -peekThresholdPx && !hapticTriggered) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); hapticTriggered = true; onPeekThresholdReached() }
                            }
                        }
                    )
                }
        ) { coverContent() }
    }
}

@Composable
fun PeekIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "peek")
    val dy by infiniteTransition.animateFloat(0f, -15f, infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "peekMove")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { translationY = dy }) {
        Icon(Icons.Default.Visibility, null, tint = AccentGreen, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text("MIRAR", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// --- VOTACIÓN Y GAME OVER (IGUAL QUE ANTES) ---
// Mantén tus funciones VotingPhase y GameOverScreen aquí abajo.
// Si no las tienes a mano, dímelo y te las copio de nuevo, pero son idénticas a la versión anterior.
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