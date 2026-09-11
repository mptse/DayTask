package com.angelina.daytask.data.model

enum class LandscapeType {
    MOUNTAIN, FOREST, DESERT, VOLCANO
}

enum class Language {
    ES, EN
}

data class UserSettings(
    val darkTheme: Boolean = false,
    val landscape: LandscapeType = LandscapeType.MOUNTAIN,
    val language: Language = Language.ES,
    val isAppLockEnabled: Boolean = false,
    val secretGestureCode: String = "", // Stores the sequence of dots (e.g., "012")
    val avatarEmoji: String = "🏃"
)
