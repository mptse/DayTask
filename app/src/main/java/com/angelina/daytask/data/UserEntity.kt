package com.angelina.daytask.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.angelina.daytask.data.model.LandscapeType
import com.angelina.daytask.data.model.Language

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val password: String,
    val name: String,
    val xp: Int = 0,
    val level: Int = 1,
    val streakCount: Int = 0,
    val lastCompletionDate: Long = 0,
    
    // Settings persisted per user
    val darkTheme: Boolean = false,
    val landscape: LandscapeType = LandscapeType.MOUNTAIN,
    val language: Language = Language.ES,
    val avatarEmoji: String = "🏃"
)
