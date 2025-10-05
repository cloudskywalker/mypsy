package com.example.psychologist.database.repository

import com.example.psychologist.database.dao.MessageDao
import com.example.psychologist.database.entity.Message
import kotlinx.coroutines.flow.Flow


class MessageRepository(private val messageDao: MessageDao) {
    fun getMessagesByConversationId(conversationId: Long): Flow<List<Message>> {
        return messageDao.getMessagesByConversationId(conversationId)
    }

    suspend fun insertMessage(message: Message): Long {
        return messageDao.insert(message)
    }

    suspend fun updateMessage(message: Message) {
        messageDao.update(message)
    }

    suspend fun deleteMessage(message: Message) {
        messageDao.delete(message)
    }

    suspend fun getMessageById(id: Long): Message? {
        return messageDao.getMessageById(id)
    }

    // 添加删除特定会话所有消息的方法
    suspend fun deleteMessagesByConversationId(conversationId: Long) {
        messageDao.deleteMessagesByConversationId(conversationId)
    }
}
