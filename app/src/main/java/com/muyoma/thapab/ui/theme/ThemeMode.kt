package com.muyoma.thapab.ui.theme

enum class ThemeMode(val storageValue: String, val label: String) {
    SYSTEM("system", "Auto"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }

    companion object {
        fun fromStorage(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}
