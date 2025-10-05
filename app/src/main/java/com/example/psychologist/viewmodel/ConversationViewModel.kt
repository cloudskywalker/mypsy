package com.example.psychologist.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.psychologist.database.entity.Conversation
import com.example.psychologist.database.repository.ConversationRepository
import kotlinx.coroutines.launch

class ConversationViewModel(private val conversationRepository: ConversationRepository) : ViewModel() {

    // 使用 asLiveData() 将 Flow 转换为 LiveData
    val conversations: LiveData<List<Conversation>> = conversationRepository.getAllConversations().asLiveData()

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversation)
        }
    }
}
class ConversationViewModelFactory(
    private val conversationRepository: ConversationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConversationViewModel(conversationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}