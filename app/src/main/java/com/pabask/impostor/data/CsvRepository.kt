package com.pabask.impostor.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pabask.impostor.model.WordItem
import com.pabask.impostor.model.WordPack
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class WordRepository(private val context: Context) {

    private val gson = Gson()
    private val fileName = "packs_data.json"
    private val prefsName = "impostor_game_prefs"
    private val keyHistory = "used_words_history"

    // Memoria caché de los packs
    private var cachedPacks: MutableList<WordPack> = mutableListOf()

    init {
        // Al iniciar, cargamos los datos
        loadPacks()
    }

    fun getPacks(): List<WordPack> {
        return cachedPacks
    }

    /**
     * Guarda una nueva palabra en un pack y actualiza el archivo JSON
     */
    fun addWordToPack(packId: String, word: String, category: String) {
        val pack = cachedPacks.find { it.id == packId }
        pack?.let {
            it.words.add(WordItem(word, category))
            savePacksToInternalStorage()
        }
    }

    /**
     * Crea un nuevo pack vacío
     */
    fun createPack(packName: String) {
        val newId = "custom_${System.currentTimeMillis()}"
        val newPack = WordPack(
            id = newId,
            displayName = packName,
            words = mutableListOf(),
            isCustom = true
        )
        cachedPacks.add(newPack)
        savePacksToInternalStorage()
    }

    /**
     * Devuelve una palabra aleatoria del pack seleccionado
     */
    fun getSecretWord(packId: String): Pair<String, String>? {
        val pack = cachedPacks.find { it.id == packId } ?: return null
        if (pack.words.isEmpty()) return null

        val history = getHistory(packId)
        // Filtramos las palabras que no estén en el historial
        val candidates = pack.words.filter { !history.contains(it.word) }

        val finalCandidates = if (candidates.isEmpty()) {
            clearHistory(packId)
            pack.words
        } else {
            candidates
        }

        val selection = finalCandidates.random()
        addToHistory(packId, selection.word)

        return selection.word to selection.category
    }

    // --- GESTIÓN DE ARCHIVOS ---

    private fun loadPacks() {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            // Leemos del archivo local (ediciones del usuario)
            val jsonString = file.readText()
            val type = object : TypeToken<MutableList<WordPack>>() {}.type
            cachedPacks = gson.fromJson(jsonString, type)
        } else {
            // Primera vez: Importamos de Assets
            importFromAssets()
            savePacksToInternalStorage()
        }
    }

    private fun savePacksToInternalStorage() {
        val jsonString = gson.toJson(cachedPacks)
        val file = File(context.filesDir, fileName)
        file.writeText(jsonString)
    }

    private fun importFromAssets() {
        // Aquí definimos tus packs iniciales
        val initialPacks = listOf(
            Triple("pack_acon", "Pack Acon", "acon.csv"),
            Triple("pack_sis", "Pack Sis", "sis.csv"),
            Triple("pack_fans", "Pack Fans", "fans.csv")
        )

        initialPacks.forEach { (id, name, file) ->
            val words = readCsvFromAssets(file)
            cachedPacks.add(WordPack(id, name, words.toMutableList()))
        }
    }

    private fun readCsvFromAssets(filename: String): List<WordItem> {
        val items = mutableListOf<WordItem>()
        try {
            val inputStream = context.assets.open(filename)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.forEachLine { line ->
                if (line.isNotBlank()) {
                    val parts = when {
                        line.contains(";") -> line.split(";")
                        line.contains(",") -> line.split(",")
                        else -> listOf(line, "Otro")
                    }
                    val w = parts[0].trim()
                    val c = if (parts.size > 1) parts[1].trim() else "Otro"
                    if (w.isNotEmpty()) items.add(WordItem(w, c))
                }
            }
            reader.close()
        } catch (e: Exception) { e.printStackTrace() }
        return items
    }

    fun deletePack(packId: String) {
        // Filtramos la lista para quitar el pack con ese ID
        cachedPacks.removeAll { it.id == packId }
        savePacksToInternalStorage()
    }

    fun deleteWordFromPack(packId: String, wordToDelete: String) {
        val pack = cachedPacks.find { it.id == packId }
        pack?.let {
            // Borramos la palabra que coincida
            it.words.removeAll { item -> item.word == wordToDelete }
            savePacksToInternalStorage()
        }
    }

    // --- HISTORIAL (POR PREFS) ---
    private fun getHistory(packId: String): MutableList<String> {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val str = prefs.getString("$keyHistory$packId", "") ?: ""
        return if (str.isBlank()) mutableListOf() else str.split(",").toMutableList()
    }

    private fun addToHistory(packId: String, word: String) {
        val list = getHistory(packId)
        list.add(word)
        if (list.size > 50) list.removeAt(0)
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putString("$keyHistory$packId", list.joinToString(",")).apply()
    }

    private fun clearHistory(packId: String) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().remove("$keyHistory$packId").apply()
    }
}