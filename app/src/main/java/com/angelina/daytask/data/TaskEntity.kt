package com.angelina.daytask.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val completed: Boolean,
    val xp: Int,
    val day: String,   // Format: "dd MMM, yyyy" or similar
    val time: String   // Format: "HH:mm"
)
