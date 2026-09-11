package com.angelina.daytask.data.model

enum class NoteCategory {
    PERSONAL, WORK, IDEAS, URGENT
}

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val date: String,
    val category: NoteCategory = NoteCategory.PERSONAL,
    val isPinned: Boolean = false
)
