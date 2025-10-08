package com.example.psychologist.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {
    private const val THEME_PREF = "theme_pref"
    private const val THEME_MODE = "theme_mode"

    const val MODE_LIGHT = 0
    const val MODE_DARK = 1
    const val MODE_SYSTEM = 2

    fun applyTheme(themeMode: Int) {
        when (themeMode) {
            MODE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            MODE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            MODE_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun saveTheme(context: Context, themeMode: Int) {
        val prefs: SharedPreferences = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)
        prefs.edit().putInt(THEME_MODE, themeMode).apply()
    }

    fun getSavedTheme(context: Context): Int {
        val prefs: SharedPreferences = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)
        return prefs.getInt(THEME_MODE, MODE_SYSTEM)
    }
}