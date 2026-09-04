package com.angelina.daytask.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.angelina.daytask.data.*
import com.angelina.daytask.data.model.Note
import com.angelina.daytask.data.model.Task
import com.angelina.daytask.data.model.UserSettings
import com.angelina.daytask.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val userDao: UserDao
) : AndroidViewModel(application) {

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
            val id = taskDao.insertTask(task.toEntity())
            val newTask = task.copy(id = id)
            NotificationHelper.scheduleTaskNotification(getApplication(), newTask)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task.toEntity())
            if (!task.completed) {
                NotificationHelper.scheduleTaskNotification(getApplication(), task)
            } else {
                NotificationHelper.cancelTaskNotification(getApplication(), task)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task.toEntity())
            NotificationHelper.cancelTaskNotification(getApplication(), task)
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

    suspend fun registerUser(user: UserEntity): Boolean {
        return try {
            if (userDao.getUserByEmail(user.email) != null) return false
            userDao.registerUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun loginUser(email: String, pass: String): UserEntity? {
        return userDao.getUserByEmail(email)?.takeIf { it.password == pass }
    }

    class Factory(
        private val application: Application,
        private val taskDao: TaskDao,
        private val noteDao: NoteDao,
        private val userDao: UserDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, taskDao, noteDao, userDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// Mapper extensions
fun TaskEntity.toModel() = Task(id, name, emoji, completed, xp, day, time)
fun Task.toEntity() = TaskEntity(id, name, emoji, completed, xp, day, time)

fun NoteEntity.toModel() = Note(id, title, content, date, category)
fun Note.toEntity() = NoteEntity(id, title, content, date, category)
