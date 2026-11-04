package com.letssign.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.letssign.mobile.data.local.entity.SignatureFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SignatureFieldDao {
    @Insert
    suspend fun insert(field: SignatureFieldEntity): Long

    @Update
    suspend fun update(field: SignatureFieldEntity)

    @Delete
    suspend fun delete(field: SignatureFieldEntity)

    @Query("SELECT * FROM signature_fields WHERE documentId = :documentId")
    fun observeByDocument(documentId: Long): Flow<List<SignatureFieldEntity>>

    @Query("SELECT * FROM signature_fields WHERE documentId = :documentId")
    suspend fun getByDocument(documentId: Long): List<SignatureFieldEntity>

    @Query("SELECT * FROM signature_fields WHERE documentSignerId = :documentSignerId")
    suspend fun getByDocumentSigner(documentSignerId: Long): List<SignatureFieldEntity>

    @Query("DELETE FROM signature_fields WHERE id = :id")
    suspend fun deleteById(id: Long)
}
