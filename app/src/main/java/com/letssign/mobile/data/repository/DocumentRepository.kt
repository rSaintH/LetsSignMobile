package com.letssign.mobile.data.repository

import android.net.Uri
import com.letssign.mobile.data.local.entity.DocumentEntity
import com.letssign.mobile.data.local.entity.DocumentEventEntity
import com.letssign.mobile.data.local.entity.DocumentSignerEntity
import com.letssign.mobile.data.local.entity.SignatureFieldEntity
import com.letssign.mobile.data.local.entity.SignerClassification
import kotlinx.coroutines.flow.Flow

/** DTO usado ao montar a lista de signatários selecionados no wizard de criação. */
data class PendingSignerSelection(
    val signerId: Long,
    val classification: SignerClassification
)

/** DTO usado ao montar/editar um campo de assinatura na tela de posicionamento. */
data class PendingSignatureField(
    val id: Long? = null,
    val signerId: Long,
    val page: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float
)

/**
 * Contrato de acesso a documentos. Implementado hoje 100% com Room
 * (DocumentRepositoryImpl); estruturado para que, na Sprint 2, uma
 * implementação que fale com a API REST do backend possa substituí-lo
 * sem qualquer alteração nas telas.
 */
interface DocumentRepository {

    suspend fun importPdf(uri: Uri, suggestedFileName: String): Result<PdfImportResult>

    suspend fun createDraft(
        title: String,
        importedFilePath: String,
        originalFileName: String,
        pageCount: Int,
        ownerAdminId: Long,
        inviteMessage: String
    ): Long

    suspend fun saveSigners(documentId: Long, selections: List<PendingSignerSelection>)

    suspend fun saveFields(documentId: Long, fields: List<PendingSignatureField>)

    suspend fun updateInviteMessage(documentId: Long, message: String)

    suspend fun send(documentId: Long): Result<Unit>

    suspend fun createNewVersion(documentId: Long): Result<Long>

    fun observeDocumentsOfAdmin(adminId: Long): Flow<List<DocumentEntity>>

    fun observeDocument(documentId: Long): Flow<DocumentEntity?>

    fun observeSigners(documentId: Long): Flow<List<DocumentSignerEntity>>

    fun observeFields(documentId: Long): Flow<List<SignatureFieldEntity>>

    fun observeEvents(documentId: Long): Flow<List<DocumentEventEntity>>

    suspend fun getDocument(documentId: Long): DocumentEntity?

    suspend fun getFields(documentId: Long): List<SignatureFieldEntity>

    suspend fun getSigners(documentId: Long): List<DocumentSignerEntity>

    suspend fun getFieldsForDocumentSigner(documentSignerId: Long): List<SignatureFieldEntity>

    /** Pendências de assinatura e documentos já assinados para um Signatário. */
    fun observeDocumentSignersFor(signerId: Long): Flow<List<DocumentSignerEntity>>

    /**
     * Grava a imagem da assinatura manuscrita em cada campo do signatário,
     * carimba no PDF de trabalho do documento e atualiza os status
     * (signatário -> SIGNED; documento -> PARTIALLY_SIGNED/COMPLETED).
     */
    suspend fun submitSignature(
        documentId: Long,
        documentSignerId: Long,
        signatureImagesByFieldId: Map<Long, String>
    ): Result<Unit>

    data class PdfImportResult(val filePath: String, val pageCount: Int, val originalFileName: String)
}
