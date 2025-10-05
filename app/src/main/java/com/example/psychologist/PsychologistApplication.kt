package com.example.psychologist

import android.app.Application
import com.example.psychologist.database.AppDatabase
import com.example.psychologist.util.ThemeUtils

class PsychologistApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // 应用保存的主题
        val savedTheme = ThemeUtils.getSavedTheme(this)
        ThemeUtils.applyTheme(savedTheme)
    }
}