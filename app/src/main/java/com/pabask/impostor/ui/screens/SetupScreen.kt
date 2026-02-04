package com.pabask.impostor.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pabask.impostor.R
import com.pabask.impostor.model.WordPack
import kotlin.math.max

// --- PALETA DE COLORES ---
private val DarkBackground = Color(0xFF0F111A)
private val CardBackground = Color(0xFF1E202B)
private val AccentGreen = Color(0xFF00C853)
private val AccentRed = Color(0xFFFF5252)
private val AccentBlue = Color(0xFF448AFF)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFAAAAAA)

private const val MAX_PLAYERS = 20

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    availablePacks: List<WordPack>,
    initialPlayers: List<String>,
    initialPackId: String?,
    initialShowCategory: Boolean,
    onStartGame: (List<String>, Int, List<WordPack>, Boolean, Boolean) -> Unit,
    onCreatePack: (String) -> Unit,
    onAddWord: (WordPack, String, String) -> Unit,
    onDeletePack: (WordPack) -> Unit,
    onDeleteWord: (WordPack, String) -> Unit
) {
    // --- GESTIÓN DE SELECCIÓN (CORREGIDO) ---
    // 1. Usamos mutableStateListOf para que Compose detecte cambios internos (add/remove)
    val selectedPacks = remember { mutableStateListOf<WordPack>() }

    // 2. Inicializamos la lista UNA VEZ al entrar
    LaunchedEffect(Unit) {
        val savedIds = initialPackId?.split(",") ?: emptyList()
        if (savedIds.isNotEmpty()) {
            val packsToSelect = availablePacks.filter { savedIds.contains(it.id) }
            selectedPacks.addAll(packsToSelect)
        } else {
            // Si no hay nada guardado, seleccionamos el primero por defecto
            availablePacks.firstOrNull()?.let { selectedPacks.add(it) }
        }
    }

    // 3. Sincronizamos si availablePacks cambia (ej: borras un pack)
    LaunchedEffect(availablePacks) {
        val validPacks = selectedPacks.filter { p -> availablePacks.any { it.id == p.id } }

        // Solo actualizamos si hay discrepancia para evitar bucles
        if (validPacks.size != selectedPacks.size) {
            selectedPacks.clear()
            selectedPacks.addAll(validPacks)
        }

        if (selectedPacks.isEmpty() && availablePacks.isNotEmpty()) {
            selectedPacks.add(availablePacks.first())
        }
    }

    // --- ESTADOS LOCALES ---
    var showCategory by remember { mutableStateOf(initialShowCategory) }
    var useQrMode by remember { mutableStateOf(false)}
    var showPackDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    val playerNames = remember { mutableStateListOf<String>().apply { addAll(initialPlayers) } }

    val isDuplicate = remember(tempName, playerNames.toList()) {
        tempName.isNotBlank() && playerNames.any { it.equals(tempName.trim(), ignoreCase = true) }
    }
    val isFull = playerNames.size >= MAX_PLAYERS
    val canAddPlayer = !isFull && !isDuplicate && tempName.isNotBlank()

    var impostorCount by remember { mutableIntStateOf(1) }
    val maxImpostors = max(1, playerNames.size / 2)

    if (impostorCount > maxImpostors) impostorCount = maxImpostors
    if (impostorCount < 1) impostorCount = 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "IMPOSTOR",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentRed,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // --- JUGADORES ---
            SettingSectionCard(
                icon = rememberVectorPainter(Icons.Default.Group),
                iconTint = AccentBlue,
                title = "Jugadores",
                value = "${playerNames.size}/$MAX_PLAYERS"
            ) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    placeholder = {
                        Text(
                            text = if (isFull) "Sala llena" else "Añadir nombre...",
                            color = TextGray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isDuplicate,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = AccentBlue,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (canAddPlayer) {
                            playerNames.add(tempName.trim())
                            tempName = ""
                        }
                    }),
                    trailingIcon = {
                        IconButton(
                            enabled = canAddPlayer,
                            onClick = {
                                if (canAddPlayer) {
                                    playerNames.add(tempName.trim())
                                    tempName = ""
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (canAddPlayer) AccentBlue else TextGray
                            )
                        }
                    }
                )

                if (!isDuplicate) Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    playerNames.forEach { name ->
                        AssistChip(
                            onClick = { playerNames.remove(name) },
                            label = { Text(name, color = TextWhite) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = TextGray
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF2C2F3F)),
                            border = null
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // --- IMPOSTORES ---
            SettingSectionCard(
                icon = painterResource(id = R.drawable.impostor_icon),
                iconTint = Color.Unspecified,
                title = "Impostores",
                value = ""
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cantidad", color = TextGray, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularButton(
                            icon = Icons.Default.Remove,
                            color = AccentRed,
                            enabled = impostorCount > 1
                        ) { impostorCount-- }

                        Text(
                            text = "$impostorCount",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        CircularButton(
                            icon = Icons.Default.Add,
                            color = AccentRed,
                            enabled = impostorCount < maxImpostors && playerNames.size >= 3
                        ) { impostorCount++ }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // --- PAQUETES ---
            val packText = when (selectedPacks.size) {
                0 -> "Seleccionar"
                1 -> selectedPacks.first().displayName
                else -> "${selectedPacks.size} seleccionados"
            }
            SettingSectionCard(
                icon = painterResource(id = R.drawable.paquete_icon),
                iconTint = Color(0xFFFFC107),
                title = "Paquetes",
                value = packText,
                showArrow = true,
                onClick = { showPackDialog = true }
            ) { }
            Spacer(modifier = Modifier.height(12.dp))

            // --- OPCIONES ---
            SettingSectionCard(
                icon = painterResource(id = R.drawable.llave_inglesa_icon),
                iconTint = AccentGreen,
                title = "Opciones",
                value = ""
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mostrar Categoría", color = TextWhite, fontSize = 16.sp)
                    Switch(
                        checked = showCategory,
                        onCheckedChange = { showCategory = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = AccentGreen,
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = CardBackground
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // OPCIÓN 2: MODO QR (NUEVO)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Modo Escáner QR", color = TextWhite, fontSize = 16.sp)
                        Text("Usa tu propio móvil", color = TextGray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = useQrMode,
                        onCheckedChange = { useQrMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TextWhite, checkedTrackColor = AccentBlue) // Azul para diferenciar
                    )
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        // --- DIÁLOGOS Y BOTÓN FLOTANTE ---
        if (showPackDialog) {
            PackSelectionDialog(
                availablePacks = availablePacks,
                selectedPacks = selectedPacks,
                onSelectionChanged = { updatedList ->
                    // Al ser mutableStateListOf, limpiar y añadir dispara la animación del tick al instante
                    selectedPacks.clear()
                    selectedPacks.addAll(updatedList)
                },
                onDismiss = { showPackDialog = false },
                onCreatePack = onCreatePack,
                onAddWord = onAddWord,
                onDeletePack = onDeletePack,
                onDeleteWord = onDeleteWord
            )
        }

        Button(
            onClick = {
                if (selectedPacks.isNotEmpty()) {
                    onStartGame(playerNames, impostorCount, selectedPacks, showCategory, useQrMode)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            enabled = playerNames.size >= 3 && selectedPacks.isNotEmpty(),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
                disabledContainerColor = Color(0xFF2E323D)
            )
        ) {
            Text("Iniciar Juego", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
//                  COMPONENTES AUXILIARES Y DIÁLOGOS
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PackSelectionDialog(
    availablePacks: List<WordPack>,
    selectedPacks: List<WordPack>,
    onSelectionChanged: (List<WordPack>) -> Unit,
    onDismiss: () -> Unit,
    onCreatePack: (String) -> Unit,
    onDeletePack: (WordPack) -> Unit,
    onAddWord: (WordPack, String, String) -> Unit,
    onDeleteWord: (WordPack, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var packToEdit by remember { mutableStateOf<WordPack?>(null) }
    var packToDelete by remember { mutableStateOf<WordPack?>(null) }

    fun toggleSelection(pack: WordPack) {
        val newList = selectedPacks.toMutableList()
        if (newList.any { it.id == pack.id }) {
            newList.removeAll { it.id == pack.id }
        } else {
            newList.add(pack)
        }
        onSelectionChanged(newList)
    }

    if (showCreateDialog) {
        InputSimpleDialog(
            title = "Nuevo Paquete",
            label = "Nombre",
            onConfirm = { name -> onCreatePack(name); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (packToDelete != null) {
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            containerColor = CardBackground,
            title = { Text("¿Borrar ${packToDelete?.displayName}?", color = TextWhite) },
            text = { Text("Esta acción no se puede deshacer.", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = { onDeletePack(packToDelete!!); packToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Borrar", color = TextWhite) }
            },
            dismissButton = {
                TextButton(onClick = { packToDelete = null }) { Text("Cancelar", color = TextGray) }
            }
        )
    }

    if (packToEdit != null) {
        PackDetailsDialog(
            pack = packToEdit!!,
            onAddWord = { w, c -> onAddWord(packToEdit!!, w, c) },
            onDeleteWord = { w -> onDeleteWord(packToEdit!!, w) },
            onDismiss = { packToEdit = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PAQUETES", color = TextWhite, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, null, tint = AccentGreen)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo", color = AccentGreen, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(availablePacks) { pack ->
                    // IMPORTANTE: Al ser selectedPacks un StateList, esto se recompone al cambiar
                    val isSelected = selectedPacks.any { it.id == pack.id }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toggleSelection(pack) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF2C2F3F) else CardBackground
                        ),
                        border = if (isSelected) BorderStroke(2.dp, AccentGreen) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) AccentGreen else TextGray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pack.displayName,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${pack.words.size} palabras",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { packToEdit = pack }) {
                                Icon(Icons.Default.Edit, null, tint = AccentBlue)
                            }
                            IconButton(onClick = { packToDelete = pack }) {
                                Icon(Icons.Default.Delete, null, tint = AccentRed)
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PackDetailsDialog(
    pack: WordPack,
    onAddWord: (String, String) -> Unit,
    onDeleteWord: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddWordUI by remember { mutableStateOf(false) }
    var newWord by remember { mutableStateOf("") }
    val categories = listOf("Objeto", "Individuo", "Lugar", "Organización", "Otro")
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        title = { Text(pack.displayName, color = TextWhite) },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                if (!showAddWordUI) {
                    Button(
                        onClick = { showAddWordUI = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir Palabra")
                    }
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = newWord,
                                onValueChange = { newWord = it },
                                label = { Text("Nueva palabra") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = AccentGreen,
                                    unfocusedBorderColor = TextGray
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = (cat == selectedCategory),
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AccentBlue,
                                            selectedLabelColor = TextWhite
                                        )
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddWordUI = false }) {
                                    Text("Cancelar", color = TextGray)
                                }
                                Button(
                                    onClick = {
                                        if (newWord.isNotBlank()) {
                                            onAddWord(newWord, selectedCategory)
                                            newWord = ""
                                            showAddWordUI = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                                ) {
                                    Text("Guardar")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                if (pack.words.isEmpty()) {
                    Text("Paquete vacío", color = TextGray, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(pack.words) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.word, color = TextWhite, fontWeight = FontWeight.Bold)
                                        Text(item.category, color = TextGray, fontSize = 10.sp)
                                    }
                                    IconButton(
                                        onClick = { onDeleteWord(item.word) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = AccentRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = AccentBlue)
            }
        }
    )
}

@Composable
fun InputSimpleDialog(
    title: String,
    label: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text(title, color = TextWhite) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = TextGray
                ),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("Crear", color = TextWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextGray)
            }
        }
    )
}

@Composable
fun SettingSectionCard(
    icon: Painter,
    iconTint: Color,
    title: String,
    value: String,
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (value.isNotEmpty()) {
                    Text(
                        text = value,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                if (showArrow) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(">", color = TextGray, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun CircularButton(
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (enabled) color else Color(0xFF2E323D)
    val iconColor = if (enabled) Color.Black else TextGray
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}