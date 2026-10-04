package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val provider: String,
    val model: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensUsed: Int = 0
)

@Entity(tableName = "recent_projects")
data class RecentProjectEntity(
    @PrimaryKey val path: String,
    val name: String,
    val lastOpened: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val templateType: String = "Empty"
)

@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey val id: String,
    val role: String,
    val taskDescription: String,
    val status: String, // "PENDING", "APPROVED", "COMPLETED", "REJECTED"
    val plan: String,
    val diff: String,
    val timestamp: Long = System.currentTimeMillis()
)
