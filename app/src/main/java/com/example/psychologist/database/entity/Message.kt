package com.example.psychologist.database.entity

import androidx.room.*

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = Conversation::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("conversationId"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["conversationId"]),  // 为外键列创建索引
        Index(value = ["timestamp"]),  // 为时间戳创建索引，便于按时间排序
        Index(value = ["sender", "timestamp"])  // 复合索引，便于按发送者和时间查询
    ]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "conversationId") val conversationId: Long,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "sender") val sender: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "status") val status: String = "sent",
    @ColumnInfo(name = "isStreaming") val isStreaming: Boolean = false,
    @ColumnInfo(name = "isEdited") val isEdited: Boolean = false
)