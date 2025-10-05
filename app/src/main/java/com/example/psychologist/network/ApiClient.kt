package com.example.psychologist.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.psychologist.BuildConfig
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://api.moonshot.cn/v1/"
    private const val API_KEY = BuildConfig.KIMI_API_KEY

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Authorization", "Bearer $API_KEY")
                .header("Content-Type", "application/json")
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // 改为BASIC减少日志开销
        })
        .connectTimeout(30, TimeUnit.SECONDS) // 设置连接超时
        .readTimeout(30, TimeUnit.SECONDS)    // 设置读取超时
        .writeTimeout(30, TimeUnit.SECONDS)   // 设置写入超时
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val kimiApiService: KimiApiService = retrofit.create(KimiApiService::class.java)
}