package com.example.psychologist.database.entity

import androidx.room.Entity
import androidx.room.*

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["updatedAt"]),  // 为更新时间创建索引，便于排序
        Index(value = ["createdAt"])   // 为创建时间创建索引
    ]
)
data class Conversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis()
)