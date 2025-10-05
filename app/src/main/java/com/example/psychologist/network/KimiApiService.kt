package com.example.psychologist.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

interface KimiApiService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun sendMessage(@Body request: KimiRequest): Response<KimiResponse>

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    @Streaming
    suspend fun sendMessageStream(@Body request: KimiRequest): ResponseBody
}

data class KimiRequest(
    val model: String = "kimi-k2-0905-preview",
    val messages: List<KimiMessage>,
    val stream: Boolean = false
)

data class KimiMessage(
    val role: String,
    val content: String,
    val name: String? = null // 添加可选的 name 字段
)

data class KimiResponse(
    val id: String,
    val choices: List<KimiChoice>
)

data class KimiChoice(
    val message: KimiMessage,
    val finish_reason: String
)

data class StreamResponse(
    val choices: List<StreamChoice>
)

data class StreamChoice(
    val delta: StreamDelta,
    val finish_reason: String?
)

data class StreamDelta(
    val content: String?
)