package com.pabask.impostor.model

data class Player(
    val name: String,
    val role: Role,
    val secretWord: String,
    val category: String,
    val isAlive: Boolean = true
)

enum class Role { CIVILIAN, IMPOSTOR }