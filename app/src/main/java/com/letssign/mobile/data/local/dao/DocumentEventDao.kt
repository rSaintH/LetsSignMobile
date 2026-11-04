package com.letssign.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.letssign.mobile.data.local.entity.DocumentEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentEventDao {
    @Insert
    suspend fun insert(event: DocumentEventEntity): Long

    @Query("SELECT * FROM document_events WHERE documentId = :documentId ORDER BY timestamp ASC")
    fun observeByDocument(documentId: Long): Flow<List<DocumentEventEntity>>
}
