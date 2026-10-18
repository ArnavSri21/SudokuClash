package com.example.ui.model

data class Cell(
    val row: Int,
    val col: Int,
    val value: Int = 0,
    val isGiven: Boolean = false,
    val notes: Set<Int> = emptySet(),
    val isError: Boolean = false,
    val isHint: Boolean = false
)
