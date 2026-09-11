package com.angelina.daytask.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.angelina.daytask.data.model.NoteCategory

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val date: String,
    val category: NoteCategory = NoteCategory.PERSONAL,
    val isPinned: Boolean = false
)
