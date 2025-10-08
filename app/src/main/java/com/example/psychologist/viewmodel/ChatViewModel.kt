package com.example.psychologist.viewmodel

import androidx.lifecycle.*
import com.example.psychologist.adapter.ChatAdapter
import com.example.psychologist.database.entity.Message
import com.example.psychologist.database.entity.Conversation
import com.example.psychologist.database.repository.MessageRepository
import com.example.psychologist.database.repository.ConversationRepository
import com.example.psychologist.network.ApiClient
import com.example.psychologist.network.KimiRequest
import com.example.psychologist.network.KimiMessage
import com.example.psychologist.network.StreamResponse
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.ResponseBody

class ChatViewModel(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _currentConversationId = MutableLiveData<Long>()
    val currentConversationId: LiveData<Long> = _currentConversationId

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentStreamingMessage: Message? = null
    private var currentJob: Job? = null

    init {
        createNewConversation()
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val conversation = Conversation(title = "新对话")
            val conversationId = conversationRepository.insertConversation(conversation)
            _currentConversationId.value = conversationId

            // 添加默认欢迎消息
            val welcomeMessage = Message(
                conversationId = conversationId,
                content = "我是你的私人心理医生，需要帮助随时找我。",
                sender = "assistant",
                status = "sent"
            )
            messageRepository.insertMessage(welcomeMessage)
            loadMessages(conversationId)
        }
    }

    // 添加 loadMessages 方法
    private suspend fun loadMessages(conversationId: Long) {
        messageRepository.getMessagesByConversationId(conversationId).collect { messages ->
            _messages.postValue(messages)
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val conversationId = _currentConversationId.value ?: return@launch

            // 保存用户消息
            val userMessage = Message(
                conversationId = conversationId,
                content = content,
                sender = "user",
                status = "sending"
            )
            val messageId = messageRepository.insertMessage(userMessage)
            // 不要尝试重新赋值 userMessage.id，因为它是 val

            // 更新对话标题（如果是第一条用户消息）
            updateConversationTitle(conversationId, content)

            // 创建AI消息占位符
            val aiMessage = Message(
                conversationId = conversationId,
                content = "",
                sender = "assistant",
                status = "sending",
                isStreaming = true
            )
            val aiMessageId = messageRepository.insertMessage(aiMessage)
            // 同样不要尝试重新赋值 aiMessage.id
            currentStreamingMessage = aiMessage.copy(id = aiMessageId)

            try {
                // 构建消息历史
                val messages = _messages.value ?: emptyList()

                // 创建多个系统消息
                val systemMessages = listOf(
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513euedfr4i11hwjmzi\",\"filename\":\"伯恩斯焦虑自助疗法.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513h9ndfr4i11hxxs31\",\"filename\":\"很多人（包括我自己），都没有意识到，'活在未来'是一种危害极大的'精神鸦片'.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513hfndfr4i11hy39ti\",\"filename\":\"焦虑你好.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513hkntr8zi11bik571\",\"filename\":\"焦虑心理学.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513jertp8s111cdp1di\",\"filename\":\"可塑的我：自我发展心理学的35堂必修课.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513jknrchri11c8qqe1\",\"filename\":\"课程一：我即基因.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513mcogirmi11ee6cci\",\"filename\":\"理解焦虑的大脑.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513mredfr4i11a1jomi\",\"filename\":\"内在成长：心智成熟的四个思维习惯.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513muw7mhoi11a1wcr1\",\"filename\":\"请问怎样才算接纳自己呢？.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513n1u6jak111b9yk6i\",\"filename\":\"人格解体是一种怎么样的体验？.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513n6fw36r111g5bog1\",\"filename\":\"如何对抗小概率事件的焦虑和恐惧及其泛化？.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513naatp8s111cfc7c1\",\"filename\":\"森田疗法强调不去管它，该干嘛干嘛。可症状一出现，人本能就会去对抗，根本做不到顺其自然。那该怎么办？.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513ndbs5ac111gda7di\",\"filename\":\"停下吧，焦虑：摆脱七大负面思维模式.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513ngxy57ti11h36j3i\",\"filename\":\"为什么会时不时出现不安？明明没什么事发生感，却感觉惴惴不安.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513qrhujici11e61r5i\",\"filename\":\"为什么有时候明明知道「想太多」没用，却还是控制不住陷入焦虑的内耗中？.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513qwqujici11e6566i\",\"filename\":\"我发现，我一直依赖着让我痛苦的东西。.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513r33rob5i11equij1\",\"filename\":\"心理观察实验记录.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513r6khn9w111casrsi\",\"filename\":\"遇见自己.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513r9etr8zi11boiisi\",\"filename\":\"重建自我认知.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513rcbrob5i11er1w11\",\"filename\":\"自我的觉醒：拥抱真实自我的力量.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f513rfchn9w111cayyui\",\"filename\":\"总会变好的：如何靠自己摆脱强迫症.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f57qryukubai11fns8ri\",\"filename\":\"焦虑不是恶魔也不是敌人，它像是一个过度保护你的朋友.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f57qs3tri3g111fpe5m1\",\"filename\":\"所有心理问题的根源是什么？.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f57qs6joz5ni11es1x3i\",\"filename\":\"有什么可以保持每天好心情的方法？.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f57qs9qqtfu111ajfhm1\",\"filename\":\"直觉思维和分析思维：是否应相信自己的直觉.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f57x6nkkbtki11an78p1\",\"filename\":\"被讨厌的勇气.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f57x6qyaekxi11betjbi\",\"filename\":\"创伤与复原.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f57x6txt1xmi11aoadhi\",\"filename\":\"大脑的情绪生活.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ),
                    KimiMessage(
                        role = "system",
                        content = "[{\"id\":\"lambda-f57x6yzdqdq111bsszwi\",\"filename\":\"活出最乐观的自己.docx\",\"file_type\":\"application/docx\"}]",
                        name = "resource:file-info"
                    ), KimiMessage(
                        role = "system",
                        content = "以上这些文档是你作为心理医生这么多年以来总结的经验，现在你是专业的心理医生，只允许回答心理相关的问题。"
                    )
                )

                // 构建消息列表：系统消息 + 历史消息 + 当前用户消息
                val kimiMessages = systemMessages +
                        messages.map { KimiMessage(it.sender, it.content) } +
                        KimiMessage("user", content)

                // 发送请求到Kimi API
                val request = KimiRequest(
                    messages = kimiMessages,
                    stream = true
                )

                currentJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = ApiClient.kimiApiService.sendMessageStream(request)
                        processStreamResponse(response, aiMessageId)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            _error.value = "网络错误: ${e.message}"
                            updateMessageStatus(aiMessageId, "error")
                            updateMessageStatus(messageId, "sent")
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "发送消息失败: ${e.message}"
                updateMessageStatus(aiMessageId, "error")
                updateMessageStatus(messageId, "sent")
                _isLoading.value = false
            }
        }
    }

    // 添加一个变量来引用适配器
    private var chatAdapter: ChatAdapter? = null

    // 添加一个方法来设置适配器
    fun setAdapter(adapter: ChatAdapter) {
        this.chatAdapter = adapter
    }

    // 修改 processStreamResponse 方法中的更新逻辑
    private suspend fun processStreamResponse(
        response: ResponseBody,
        messageId: Long
    ) {
        val reader = response.byteStream().bufferedReader()
        var accumulatedContent = StringBuilder()
        var lastUpdateTime = System.currentTimeMillis()
        val updateThreshold = 100 // 毫秒

        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("data: ") && line != "data: [DONE]") {
                        val json = line.removePrefix("data: ")
                        try {
                            val streamResponse = Gson().fromJson(json, StreamResponse::class.java)
                            val content = streamResponse.choices.firstOrNull()?.delta?.content ?: ""

                            if (content.isNotBlank()) {
                                accumulatedContent.append(content)

                                // 控制更新频率
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastUpdateTime > updateThreshold) {
                                    // 直接更新UI，避免数据库操作
                                    withContext(Dispatchers.Main) {
                                        chatAdapter?.updateStreamingMessage(accumulatedContent.toString())
                                    }
                                    lastUpdateTime = currentTime
                                }
                            }

                            // 检查是否结束
                            val finishReason = streamResponse.choices.firstOrNull()?.finish_reason
                            if (finishReason != null) {
                                // 最终更新数据库和UI
                                updateMessageContent(messageId, accumulatedContent.toString())

                                withContext(Dispatchers.Main) {
                                    chatAdapter?.finishStreaming()
                                    finishStreamingMessage(messageId, accumulatedContent.toString())
                                    _isLoading.value = false
                                }
                                return@forEach
                            }
                        } catch (e: Exception) {
                            // 解析错误，继续处理下一行
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _error.value = "流式响应处理错误: ${e.message}"
                updateMessageStatus(messageId, "error")
                _isLoading.value = false
            }
        }
    }

    // 添加一个变量来跟踪上次更新的时间
    private var lastMessageUpdateTime = 0L
    private val messageUpdateThreshold = 150 // 毫秒

    private suspend fun updateMessageContent(messageId: Long, content: String) {
        val currentTime = System.currentTimeMillis()

        // 控制更新频率，避免过于频繁的数据库操作
        if (currentTime - lastMessageUpdateTime > messageUpdateThreshold) {
            val message = messageRepository.getMessageById(messageId)
            message?.let {
                val updatedMessage = it.copy(content = content)
                messageRepository.updateMessage(updatedMessage)
                lastMessageUpdateTime = currentTime
            }
        }
    }

    private suspend fun finishStreamingMessage(messageId: Long, content: String) {
        val message = messageRepository.getMessageById(messageId)
        message?.let {
            val updatedMessage = it.copy(
                content = content,
                isStreaming = false,
                status = "sent"
            )
            messageRepository.updateMessage(updatedMessage)
        }
        currentStreamingMessage = null
    }

    private suspend fun updateMessageStatus(messageId: Long, status: String) {
        val message = messageRepository.getMessageById(messageId)
        message?.let {
            val updatedMessage = it.copy(status = status)
            messageRepository.updateMessage(updatedMessage)
        }
    }

    private suspend fun updateConversationTitle(conversationId: Long, firstMessage: String) {
        val conversation = conversationRepository.getConversationById(conversationId)
        conversation?.let {
            val title = if (firstMessage.length > 20) {
                firstMessage.substring(0, 20) + "..."
            } else {
                firstMessage
            }
            val updatedConversation = it.copy(
                title = title,
                updatedAt = System.currentTimeMillis()
            )
            conversationRepository.updateConversation(updatedConversation)
        }
    }

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            _currentConversationId.value = conversationId
            loadMessages(conversationId)
        }
    }

    fun editMessage(messageId: Long, newContent: String) {
        viewModelScope.launch {
            val message = messageRepository.getMessageById(messageId)
            message?.let {
                val updatedMessage = it.copy(
                    content = newContent,
                    isEdited = true
                )
                messageRepository.updateMessage(updatedMessage)
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        cancelCurrentJob()
        viewModelScope.launch {
            val message = messageRepository.getMessageById(messageId)
            message?.let {
                messageRepository.deleteMessage(it)
            }
        }
    }

    private fun cancelCurrentJob() {
        currentJob?.cancel()
        currentJob = null
        currentStreamingMessage?.let {
            viewModelScope.launch {
                updateMessageStatus(it.id, "error")
            }
        }
        currentStreamingMessage = null
    }

    fun clearError() {
        _error.value = null
    }

    // 使用事件包装器
    private val _snackbarEvent = MutableLiveData<Event<String>>()
    val snackbarEvent: LiveData<Event<String>> = _snackbarEvent

    fun clearAllConversations() {
        viewModelScope.launch {
            try {
                // 获取所有对话
                val conversations = conversationRepository.getAllConversationsOnce()

                // 删除所有对话及其消息
                conversations.forEach { conversation ->
                    messageRepository.deleteMessagesByConversationId(conversation.id)
                    conversationRepository.deleteConversation(conversation)
                }

                // 创建新对话
                createNewConversation()

                // 发送事件到 UI 层
                _snackbarEvent.postValue(Event("聊天记录已清除"))
            } catch (e: Exception) {
                _snackbarEvent.postValue(Event("清除聊天记录失败: ${e.message}"))
            }

        }
    }
}

// 事件包装器，用于处理一次性事件
class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}

class ChatViewModelFactory(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(messageRepository, conversationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}