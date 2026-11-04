package com.letssign.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.letssign.mobile.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Update
    suspend fun update(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun findById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE ownerAdminId = :adminId ORDER BY createdAt DESC")
    fun observeByAdmin(adminId: Long): Flow<List<DocumentEntity>>

    @Query("DELETE FROM documents WHERE id = :id AND status = 'DRAFT'")
    suspend fun deleteDraft(id: Long)
}
