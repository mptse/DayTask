package com.angelina.daytask.ui.viewmodel

import android.app.Application
import android.content.Context
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
import java.util.Calendar

class MainViewModel(
    application: Application,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val userDao: UserDao
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("daytask_prefs", Context.MODE_PRIVATE)

    val tasks: StateFlow<List<Task>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toModel() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = noteDao.getAllNotes().map { entities ->
        entities.map { it.toModel() }
            .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var currentUser by mutableStateOf<UserEntity?>(null)
        private set

    var userSettings by mutableStateOf(UserSettings())
        private set

    fun updateSettings(settings: UserSettings) {
        userSettings = settings
        val user = currentUser ?: return
        val updatedUser = user.copy(
            darkTheme = settings.darkTheme,
            landscape = settings.landscape,
            language = settings.language,
            isAppLockEnabled = settings.isAppLockEnabled,
            secretGestureCode = settings.secretGestureCode,
            avatarEmoji = settings.avatarEmoji
        )
        currentUser = updatedUser
        viewModelScope.launch {
            userDao.updateUser(updatedUser)
        }
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
            val oldTask = tasks.value.find { it.id == task.id }
            
            if (task.completed && oldTask?.completed == false) {
                gainXP(task.xp)
                updateStreak()
            } else if (!task.completed && oldTask?.completed == true) {
                loseXP(task.xp)
            }
            
            taskDao.updateTask(task.toEntity())
            if (!task.completed) {
                NotificationHelper.scheduleTaskNotification(getApplication(), task)
            } else {
                NotificationHelper.cancelTaskNotification(getApplication(), task)
            }
        }
    }

    private fun gainXP(amount: Int) {
        val user = currentUser ?: return
        var newXP = user.xp + amount
        var newLevel = user.level
        
        while (newXP >= 100) {
            newXP -= 100
            newLevel++
        }
        
        val updatedUser = user.copy(xp = newXP, level = newLevel)
        currentUser = updatedUser
        viewModelScope.launch {
            userDao.updateUser(updatedUser)
        }
    }

    private fun loseXP(amount: Int) {
        val user = currentUser ?: return
        var newXP = user.xp - amount
        var newLevel = user.level
        
        if (newXP < 0) {
            if (newLevel > 1) {
                newLevel--
                newXP = 100 + newXP
            } else {
                newXP = 0
            }
        }
        
        val updatedUser = user.copy(xp = newXP, level = newLevel)
        currentUser = updatedUser
        viewModelScope.launch {
            userDao.updateUser(updatedUser)
        }
    }

    private fun updateStreak() {
        val user = currentUser ?: return
        val now = Calendar.getInstance()
        val last = Calendar.getInstance()
        last.timeInMillis = user.lastCompletionDate

        val isSameDay = last.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                last.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

        if (user.lastCompletionDate != 0L && isSameDay) return

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        val isConsecutive = last.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                last.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

        val newStreak = if (user.lastCompletionDate == 0L || isConsecutive) {
            user.streakCount + 1
        } else {
            1
        }

        val updatedUser = user.copy(streakCount = newStreak, lastCompletionDate = now.timeInMillis)
        currentUser = updatedUser
        viewModelScope.launch {
            userDao.updateUser(updatedUser)
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

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.toEntity())
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
        val user = userDao.getUserByEmail(email)?.takeIf { it.password == pass }
        if (user != null) {
            currentUser = user
            applyUserSettings(user)
            prefs.edit().putString("last_user_email", email).apply()
        }
        return user
    }

    suspend fun checkSavedSession(): UserEntity? {
        val email = prefs.getString("last_user_email", null) ?: return null
        val user = userDao.getUserByEmail(email)
        if (user != null) {
            currentUser = user
            applyUserSettings(user)
        }
        return user
    }

    fun logout() {
        currentUser = null
        userSettings = UserSettings()
        prefs.edit().remove("last_user_email").apply()
    }

    private fun applyUserSettings(user: UserEntity) {
        userSettings = UserSettings(
            darkTheme = user.darkTheme,
            landscape = user.landscape,
            language = user.language,
            isAppLockEnabled = user.isAppLockEnabled,
            secretGestureCode = user.secretGestureCode,
            avatarEmoji = user.avatarEmoji
        )
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

fun NoteEntity.toModel() = Note(id, title, content, date, category, isPinned)
fun Note.toEntity() = NoteEntity(id, title, content, date, category, isPinned)
