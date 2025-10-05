package com.example.psychologist.database.repository

import com.example.psychologist.database.dao.ConversationDao
import com.example.psychologist.database.entity.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first // 添加这行导入

class ConversationRepository(private val conversationDao: ConversationDao) {
    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations()
    }

    suspend fun insertConversation(conversation: Conversation): Long {
        return conversationDao.insert(conversation)
    }

    suspend fun updateConversation(conversation: Conversation) {
        conversationDao.update(conversation)
    }

    suspend fun deleteConversation(conversation: Conversation) {
        conversationDao.delete(conversation)
    }

    suspend fun getConversationById(id: Long): Conversation? {
        return conversationDao.getConversationById(id)
    }

    // 添加获取所有对话的方法（非Flow版本）
    suspend fun getAllConversationsOnce(): List<Conversation> {
        return conversationDao.getAllConversations().first()
    }
}