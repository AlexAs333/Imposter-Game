package com.pabask.impostor.data

import android.content.Context

class PlayerRepository(context: Context) {
    private val prefs = context.getSharedPreferences("impostor_player_prefs", Context.MODE_PRIVATE)
    private val KEY_PLAYERS = "saved_players_list"
    private val KEY_LAST_PACK = "last_selected_pack_id"

    fun savePlayers(names: List<String>) {
        // Convertimos la lista ["Ana", "Juan"] a un string "Ana,Juan"
        val playersString = names.joinToString(",")
        prefs.edit().putString(KEY_PLAYERS, playersString).apply()
    }

    fun getLastPlayers(): List<String> {
        val savedString = prefs.getString(KEY_PLAYERS, "") ?: ""
        if (savedString.isBlank()) return emptyList()

        // Convertimos "Ana,Juan" de vuelta a lista
        return savedString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    fun saveLastPack(packId: String) {
        prefs.edit().putString(KEY_LAST_PACK, packId).apply()
    }

    fun getLastPackId(): String? {
        return prefs.getString(KEY_LAST_PACK, null)
    }
}