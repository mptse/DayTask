package com.angelina.daytask.data.model

data class Task(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val completed: Boolean = false,
    val xp: Int = 10,
    val day: String,
    val time: String
)
