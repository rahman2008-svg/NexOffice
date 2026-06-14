package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "DOC", "SHEET", "PDF", "PRESENTATION"
    val content: String, // Rich string/JSON document state
    val createdAt: Long = System.currentTimeMillis(),
    val lastSaved: Long = System.currentTimeMillis(),
    val folder: String = "All", // Folder categorization
    val isFavorite: Boolean = false
) : Serializable

@Entity(tableName = "document_versions")
data class DocumentVersion(
    @PrimaryKey(autoGenerate = true) val versionId: Long = 0,
    val documentId: Long,
    val content: String,
    val savedAt: Long = System.currentTimeMillis(),
    val description: String = "Auto-save point"
) : Serializable
