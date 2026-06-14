package com.example.data.repository

import com.example.data.local.DocumentDao
import com.example.data.model.Document
import com.example.data.model.DocumentVersion
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDao) {
    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()

    fun getDocumentsByFolder(folder: String): Flow<List<Document>> =
        documentDao.getDocumentsByFolder(folder)

    fun searchDocuments(query: String): Flow<List<Document>> =
        documentDao.searchDocuments(query)

    suspend fun getDocumentById(id: Long): Document? =
        documentDao.getDocumentById(id)

    suspend fun insertDocument(document: Document): Long =
        documentDao.insertDocument(document)

    suspend fun updateDocument(document: Document) =
        documentDao.updateDocument(document)

    suspend fun deleteDocument(document: Document) =
        documentDao.deleteDocument(document)

    fun getVersionsForDocument(documentId: Long): Flow<List<DocumentVersion>> =
        documentDao.getVersionsForDocument(documentId)

    suspend fun insertVersion(version: DocumentVersion) =
        documentDao.insertVersion(version)

    suspend fun deleteVersionsForDocument(documentId: Long) =
        documentDao.deleteVersionsForDocument(documentId)
}
