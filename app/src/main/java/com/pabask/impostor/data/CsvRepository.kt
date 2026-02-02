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

    // --- GESTIÓN DE PALABRAS Y PACKS ---

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
     * Borra un pack completo
     */
    fun deletePack(packId: String) {
        cachedPacks.removeAll { it.id == packId }
        savePacksToInternalStorage()
        // Opcional: Borrar también su historial de SharedPreferences para no dejar basura
        clearHistory(packId)
    }

    /**
     * Borra una palabra específica de un pack
     */
    fun deleteWordFromPack(packId: String, wordToDelete: String) {
        val pack = cachedPacks.find { it.id == packId }
        pack?.let {
            it.words.removeAll { item -> item.word == wordToDelete }
            savePacksToInternalStorage()
        }
    }

    // --- OBTENCIÓN DE PALABRA SECRETA ---

    /**
     * MÉTODO CLAVE: Obtiene una palabra aleatoria de UNA LISTA de paquetes.
     * Gestiona el historial de cada paquete individualmente para que no se repitan.
     */
    fun getSecretWordFromPacks(packIds: List<String>): Pair<String, String>? {
        // 1. Recopilamos todas las palabras candidatas de los packs seleccionados
        // Triple(Palabra, Categoria, PackId_Original)
        val candidates = mutableListOf<Triple<String, String, String>>()
        val allWordsRef = mutableListOf<Triple<String, String, String>>() // Para reseteo si hace falta

        packIds.forEach { id ->
            val pack = cachedPacks.find { it.id == id }
            pack?.let { p ->
                val history = getHistory(id)
                p.words.forEach { wordItem ->
                    // Guardamos referencia de todo por si hay que resetear
                    allWordsRef.add(Triple(wordItem.word, wordItem.category, id))

                    // Si NO está en el historial, es candidata
                    if (!history.contains(wordItem.word)) {
                        candidates.add(Triple(wordItem.word, wordItem.category, id))
                    }
                }
            }
        }

        if (allWordsRef.isEmpty()) return null // No hay palabras en ningún pack seleccionado

        // 2. Si no quedan candidatas (hemos gastado todas las palabras de los packs seleccionados), reseteamos
        val finalCandidates = if (candidates.isEmpty()) {
            // Borramos historial SOLO de los packs elegidos
            packIds.forEach { clearHistory(it) }
            allWordsRef // Volvemos a tener todas disponibles
        } else {
            candidates
        }

        // 3. Elegimos al azar
        val selection = finalCandidates.random()
        val (word, category, originPackId) = selection

        // 4. Guardamos en el historial del pack original
        addToHistory(originPackId, word)

        return word to category
    }

    /**
     * Devuelve una palabra aleatoria de un solo pack (Método Legacy, por si acaso)
     */
    fun getSecretWord(packId: String): Pair<String, String>? {
        return getSecretWordFromPacks(listOf(packId))
    }

    // --- GESTIÓN DE ARCHIVOS ---

    private fun loadPacks() {
        val file = File(context.filesDir, fileName)

        if (file.exists()) {
            try {
                // 1. Cargamos lo que tiene el usuario guardado
                val jsonString = file.readText()
                val type = object : TypeToken<MutableList<WordPack>>() {}.type
                cachedPacks = gson.fromJson(jsonString, type) ?: mutableListOf()

                // 2. CHECK INTELIGENTE: ¿Falta algún pack oficial?
                // Definimos los oficiales aquí para comprobar
                val officialPacks = listOf(
                    Triple("pack_acon", "Pack Acon", "acon.csv"),
                    Triple("pack_sis", "Pack Sis", "sis.csv"),
                    Triple("pack_fans", "Pack Fans", "fans.csv"),
                    Triple("pack_mix", "Mix General", "mix.csv") // <--- El nuevo
                )

                var hasChanges = false
                officialPacks.forEach { (id, name, filename) ->
                    // Si el usuario NO tiene este pack en su JSON, lo importamos
                    if (cachedPacks.none { it.id == id }) {
                        val words = readCsvFromAssets(filename)
                        if (words.isNotEmpty()) {
                            cachedPacks.add(WordPack(id, name, words.toMutableList()))
                            hasChanges = true
                        }
                    }
                }

                // 3. Si hemos añadido algo nuevo, guardamos el JSON actualizado
                if (hasChanges) {
                    savePacksToInternalStorage()
                }

            } catch (e: Exception) {
                // Si falla el JSON, reiniciamos todo
                importFromAssets()
                savePacksToInternalStorage()
            }
        } else {
            // Primera instalación
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
            Triple("pack_fans", "Pack Fans", "fans.csv"),
            Triple("pack_mix", "Mix General", "mix.csv")
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

    // --- HISTORIAL (POR PREFS) ---

    private fun getHistory(packId: String): MutableList<String> {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val str = prefs.getString("$keyHistory$packId", "") ?: ""
        return if (str.isBlank()) mutableListOf() else str.split(",").toMutableList()
    }

    private fun addToHistory(packId: String, word: String) {
        val list = getHistory(packId)
        list.add(word)
        // Límite de memoria por pack (puedes subirlo si quieres)
        if (list.size > 50) list.removeAt(0)

        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putString("$keyHistory$packId", list.joinToString(",")).apply()
    }

    private fun clearHistory(packId: String) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().remove("$keyHistory$packId").apply()
    }
}