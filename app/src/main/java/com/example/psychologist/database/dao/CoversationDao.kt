package com.example.psychologist.database.dao

import androidx.room.*
import com.example.psychologist.database.entity.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: Conversation): Long

    @Update
    suspend fun update(conversation: Conversation)

    @Delete
    suspend fun delete(conversation: Conversation)

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): Conversation?

    // 添加获取所有对话的方法（非Flow版本）
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversationsOnce(): List<Conversation>

    // 添加删除所有对话的方法
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}