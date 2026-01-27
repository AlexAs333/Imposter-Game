package com.pabask.impostor.model

data class WordItem(
    val word: String,
    val category: String
)

data class WordPack(
    val id: String,
    val displayName: String,
    val words: MutableList<WordItem> = mutableListOf(), // Ahora la lista vive aquí
    val isCustom: Boolean = false
)