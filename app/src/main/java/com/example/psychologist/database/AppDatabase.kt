package com.example.psychologist.database

import android.content.Context
import androidx.room.*
import com.example.psychologist.database.dao.ConversationDao
import com.example.psychologist.database.dao.MessageDao
import com.example.psychologist.database.entity.Message
import com.example.psychologist.database.entity.Conversation

@Database(
    entities = [Message::class, Conversation::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "psychologist_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}