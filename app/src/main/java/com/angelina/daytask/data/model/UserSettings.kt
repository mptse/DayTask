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
    val isBiometricEnabled: Boolean = false,
    val avatarEmoji: String = "🏃"
)
