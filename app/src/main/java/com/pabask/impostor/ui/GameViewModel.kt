package com.pabask.impostor.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.pabask.impostor.data.WordRepository
import com.pabask.impostor.model.Player
import com.pabask.impostor.model.Role
import com.pabask.impostor.model.WordPack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
    val isGameStarted: Boolean = false,
    val players: List<Player> = emptyList(),
    val currentPack: WordPack? = null,
    val startingPlayerName: String = "",
    val showCategory: Boolean = false,
    val isQrMode: Boolean = false
)

enum class GameWinner { NONE, CIVILIANS, IMPOSTORS }

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WordRepository(application)
    private val uiPrefs = application.getSharedPreferences("impostor_ui_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    // --- NUEVAS FUNCIONES PARA GESTIÓN DE PACKS ---

    // Obtener la lista de packs (incluyendo los creados por el usuario)
    fun getAvailablePacks(): List<WordPack> {
        return repository.getPacks()
    }

    // Crear un pack nuevo
    fun createNewPack(name: String) {
        repository.createPack(name)
        // Forzamos una actualización "falsa" del estado para que la UI sepa que algo cambió si es necesario
        _uiState.update { it.copy() }
    }

    // Añadir palabra a un pack existente
    fun addWordToPack(pack: WordPack, word: String, category: String) {
        repository.addWordToPack(pack.id, word, category)
    }

    // ----------------------------------------------

    fun startGame(playerNames: List<String>, impostorCount: Int, packs: List<WordPack>, showCategory: Boolean, isQrMode: Boolean){
        // Obtenemos los IDs
        val packIds = packs.map { it.id }

        // Guardamos la configuración (IDs separados por coma)
        saveLastConfig(playerNames, packIds.joinToString(","), showCategory)

        // Pedimos palabra al repositorio usando la nueva función
        val wordInfo = repository.getSecretWordFromPacks(packIds) ?: ("Pack Vacío" to "Sin Categoría")

        val secretWord = wordInfo.first
        val category = wordInfo.second
        val starterName = playerNames.random()

        val roles = mutableListOf<Role>()
        repeat(impostorCount) { roles.add(Role.IMPOSTOR) }
        val civilianCount = playerNames.size - impostorCount
        repeat(civilianCount) { roles.add(Role.CIVILIAN) }
        roles.shuffle()

        val newPlayers = playerNames.mapIndexed { index, name ->
            val assignedRole = roles[index]
            val wordForPlayer = if (assignedRole == Role.CIVILIAN) secretWord else "ERES EL IMPOSTOR"

            Player(
                name = name,
                role = assignedRole,
                secretWord = wordForPlayer,
                category = category,
                isAlive = true
            )
        }

        // En el estado, currentPack ahora es solo visual, podemos coger el primero o crear uno "falso" que se llame "Mix"
        // Para simplificar, mostramos el nombre del primero + "y otros" o "Varios"
        val displayPack = if (packs.size == 1) packs.first() else WordPack("mix_temp", "Varios (${packs.size})", mutableListOf())

        _uiState.update {
            it.copy(
                isGameStarted = true,
                players = newPlayers,
                currentPack = displayPack,
                startingPlayerName = starterName,
                showCategory = showCategory,
                isQrMode = isQrMode
            )
        }
    }

    fun stopGame() {
        _uiState.update { it.copy(isGameStarted = false, players = emptyList()) }
    }

    fun eliminatePlayer(player: Player): GameWinner {
        val updatedPlayers = _uiState.value.players.map {
            if (it.name == player.name) it.copy(isAlive = false) else it
        }
        _uiState.update { it.copy(players = updatedPlayers) }

        val impostorsAlive = updatedPlayers.count { it.role == Role.IMPOSTOR && it.isAlive }
        val civiliansAlive = updatedPlayers.count { it.role == Role.CIVILIAN && it.isAlive }

        return when {
            impostorsAlive == 0 -> GameWinner.CIVILIANS
            impostorsAlive >= civiliansAlive -> GameWinner.IMPOSTORS
            else -> GameWinner.NONE
        }
    }

    // --- PERSISTENCIA ---

    fun getSavedPlayers(): List<String> {
        val savedString = uiPrefs.getString("last_players", "") ?: ""
        if (savedString.isBlank()) return emptyList()
        return savedString.split(",").filter { it.isNotBlank() }
    }

    fun getSavedPackId(): String? {
        return uiPrefs.getString("last_pack_id", null)
    }

    fun getSavedShowCategory(): Boolean {
        return uiPrefs.getBoolean("last_show_category", true)
    }

    private fun saveLastConfig(players: List<String>, packId: String, showCategory: Boolean) {
        uiPrefs.edit()
            .putString("last_players", players.joinToString(","))
            .putString("last_pack_id", packId)
            .putBoolean("last_show_category", showCategory)
            .apply()
    }

    fun deletePack(pack: WordPack) {
        repository.deletePack(pack.id)
        _uiState.update { it.copy() } // Forzar refresco UI
    }

    fun deleteWordFromPack(pack: WordPack, word: String) {
        repository.deleteWordFromPack(pack.id, word)
        _uiState.update { it.copy() } // Forzar refresco UI
    }
}