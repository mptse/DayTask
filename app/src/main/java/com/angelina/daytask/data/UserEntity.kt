package com.angelina.daytask.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val password: String,
    val name: String
)
