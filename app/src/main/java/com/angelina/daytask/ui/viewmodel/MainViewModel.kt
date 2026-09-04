package com.angelina.daytask.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.angelina.daytask.data.NoteDao
import com.angelina.daytask.data.NoteEntity
import com.angelina.daytask.data.TaskDao
import com.angelina.daytask.data.TaskEntity
import com.angelina.daytask.data.model.Note
import com.angelina.daytask.data.model.Task
import com.angelina.daytask.data.model.UserSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val taskDao: TaskDao, private val noteDao: NoteDao) : ViewModel() {

    val tasks: StateFlow<List<Task>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toModel() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = noteDao.getAllNotes().map { entities ->
        entities.map { it.toModel() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var userSettings by mutableStateOf(UserSettings())
        private set

    fun updateSettings(settings: UserSettings) {
        userSettings = settings
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            taskDao.insertTask(task.toEntity())
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task.toEntity())
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task.toEntity())
        }
    }

    fun addNote(note: Note) {
        viewModelScope.launch {
            noteDao.insertNote(note.toEntity())
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note.toEntity())
        }
    }

    class Factory(private val taskDao: TaskDao, private val noteDao: NoteDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(taskDao, noteDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// Mapper extensions
fun TaskEntity.toModel() = Task(id, name, emoji, completed, xp, day, time)
fun Task.toEntity() = TaskEntity(id, name, emoji, completed, xp, day, time)

fun NoteEntity.toModel() = Note(id, title, content, date)
fun Note.toEntity() = NoteEntity(id, title, content, date)
