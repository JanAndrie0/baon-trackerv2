package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "baon_prefs")

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AppPalette(val label: String) {
    MINT("Mint Green"),
    PINK_PASTEL("Rosewater")
}

data class UserPreferences(
    val studentName: String = "Student",
    val hasCompletedOnboarding: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val cachedWeeklyRecap: String? = null,
    val cachedWeeklyRecapMillis: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appPalette: AppPalette = AppPalette.MINT
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val CACHED_RECAP = stringPreferencesKey("cached_weekly_recap")
        val CACHED_RECAP_MILLIS = longPreferencesKey("cached_weekly_recap_millis")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_PALETTE = stringPreferencesKey("app_palette")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val modeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(modeName) }.getOrDefault(ThemeMode.SYSTEM)
        val paletteName = prefs[Keys.APP_PALETTE] ?: AppPalette.MINT.name
        val appPalette = runCatching { AppPalette.valueOf(paletteName) }.getOrDefault(AppPalette.MINT)
        UserPreferences(
            studentName = prefs[Keys.STUDENT_NAME] ?: "Student",
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            cachedWeeklyRecap = prefs[Keys.CACHED_RECAP],
            cachedWeeklyRecapMillis = prefs[Keys.CACHED_RECAP_MILLIS] ?: 0L,
            themeMode = themeMode,
            appPalette = appPalette
        )
    }

    suspend fun completeOnboarding(studentName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STUDENT_NAME] = studentName.trim().ifEmpty { "Student" }
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun updateStudentName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STUDENT_NAME] = name.trim().ifEmpty { "Student" }
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateAppPalette(palette: AppPalette) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_PALETTE] = palette.name
        }
    }

    suspend fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = enabled
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun saveCachedRecap(recap: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CACHED_RECAP] = recap
            prefs[Keys.CACHED_RECAP_MILLIS] = System.currentTimeMillis()
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
