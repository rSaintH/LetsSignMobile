package com.letssign.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.letssign.mobile.data.local.entity.DocumentSignerEntity
import com.letssign.mobile.data.local.entity.SignatureStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentSignerDao {
    @Insert
    suspend fun insert(documentSigner: DocumentSignerEntity): Long

    @Insert
    suspend fun insertAll(items: List<DocumentSignerEntity>)

    @Update
    suspend fun update(documentSigner: DocumentSignerEntity)

    @Query("SELECT * FROM document_signers WHERE documentId = :documentId")
    fun observeByDocument(documentId: Long): Flow<List<DocumentSignerEntity>>

    @Query("SELECT * FROM document_signers WHERE documentId = :documentId")
    suspend fun getByDocument(documentId: Long): List<DocumentSignerEntity>

    @Query("DELETE FROM document_signers WHERE documentId = :documentId")
    suspend fun deleteAllOfDocument(documentId: Long)

    @Query("SELECT COUNT(*) FROM document_signers WHERE documentId = :documentId AND status != :signed")
    suspend fun countPending(documentId: Long, signed: SignatureStatus): Int

    @Query(
        """
        SELECT ds.* FROM document_signers ds
        INNER JOIN documents d ON d.id = ds.documentId
        WHERE ds.signerId = :signerId AND d.status != 'DRAFT'
        """
    )
    fun observePendingAndSignedForSigner(signerId: Long): Flow<List<DocumentSignerEntity>>

    @Query("SELECT * FROM document_signers WHERE id = :id")
    suspend fun findById(id: Long): DocumentSignerEntity?
}
