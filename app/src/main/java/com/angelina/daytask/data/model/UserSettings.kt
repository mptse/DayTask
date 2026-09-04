package com.angelina.daytask.data.model

enum class LandscapeType {
    MOUNTAIN, FOREST, DESERT
}

enum class Language {
    ES, EN
}

data class UserSettings(
    val darkTheme: Boolean? = null, // null = system
    val landscape: LandscapeType = LandscapeType.MOUNTAIN,
    val language: Language = Language.ES,
    val isBiometricEnabled: Boolean = false
)
