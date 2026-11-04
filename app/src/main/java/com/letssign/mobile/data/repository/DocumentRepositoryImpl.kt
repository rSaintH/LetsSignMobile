package com.letssign.mobile.data.repository

import android.net.Uri
import com.letssign.mobile.data.local.dao.DocumentDao
import com.letssign.mobile.data.local.dao.DocumentEventDao
import com.letssign.mobile.data.local.dao.DocumentSignerDao
import com.letssign.mobile.data.local.dao.SignatureFieldDao
import com.letssign.mobile.data.local.entity.DocumentEntity
import com.letssign.mobile.data.local.entity.DocumentEventEntity
import com.letssign.mobile.data.local.entity.DocumentEventType
import com.letssign.mobile.data.local.entity.DocumentSignerEntity
import com.letssign.mobile.data.local.entity.DocumentStatus
import com.letssign.mobile.data.local.entity.SignatureFieldEntity
import com.letssign.mobile.data.local.entity.SignatureStatus
import com.letssign.mobile.data.storage.PdfFileStorage
import com.letssign.mobile.data.storage.PdfPageRenderer
import com.letssign.mobile.data.storage.PdfStampingUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    private val documentSignerDao: DocumentSignerDao,
    private val signatureFieldDao: SignatureFieldDao,
    private val documentEventDao: DocumentEventDao,
    private val pdfFileStorage: PdfFileStorage
) : DocumentRepository {

    override suspend fun importPdf(uri: Uri, suggestedFileName: String): Result<DocumentRepository.PdfImportResult> =
        withContext(Dispatchers.IO) {
            try {
                val imported = pdfFileStorage.importPdf(uri, suggestedFileName)
                val file = File(imported.path)
                if (!PdfPageRenderer.isValidPdf(file)) {
                    file.delete()
                    return@withContext Result.failure(
                        IllegalArgumentException("Arquivo inválido, corrompido ou não suportado. Selecione outro PDF.")
                    )
                }
                val pageCount = PdfPageRenderer(file).use { it.pageCount }
                Result.success(
                    DocumentRepository.PdfImportResult(
                        filePath = imported.path,
                        pageCount = pageCount,
                        originalFileName = imported.originalName
                    )
                )
            } catch (e: Exception) {
                Result.failure(IllegalArgumentException(e.message ?: "Não foi possível processar o PDF selecionado."))
            }
        }

    override suspend fun createDraft(
        title: String,
        importedFilePath: String,
        originalFileName: String,
        pageCount: Int,
        ownerAdminId: Long,
        inviteMessage: String
    ): Long = withContext(Dispatchers.IO) {
        val document = DocumentEntity(
            title = title.trim(),
            originalFileName = originalFileName,
            filePath = importedFilePath,
            pageCount = pageCount,
            ownerAdminId = ownerAdminId,
            inviteMessage = inviteMessage
        )
        val id = documentDao.insert(document)
        documentEventDao.insert(
            DocumentEventEntity(
                documentId = id,
                type = DocumentEventType.CREATED,
                description = "Documento criado como rascunho."
            )
        )
        id
    }

    override suspend fun saveSigners(documentId: Long, selections: List<PendingSignerSelection>) =
        withContext(Dispatchers.IO) {
            documentSignerDao.deleteAllOfDocument(documentId)
            documentSignerDao.insertAll(
                selections.map {
                    DocumentSignerEntity(
                        documentId = documentId,
                        signerId = it.signerId,
                        classification = it.classification
                    )
                }
            )
        }

    override suspend fun saveFields(documentId: Long, fields: List<PendingSignatureField>) =
        withContext(Dispatchers.IO) {
            // Estratégia simples: substitui todos os campos do documento a cada salvamento
            // da tela de posicionamento, garantindo que mover/excluir seja refletido.
            val signers = documentSignerDao.getByDocument(documentId)
            val existing = signatureFieldDao.getByDocument(documentId)
            existing.forEach { signatureFieldDao.delete(it) }

            fields.forEach { field ->
                val documentSigner = signers.firstOrNull { it.signerId == field.signerId } ?: return@forEach
                val clampedX = field.xRatio.coerceIn(0f, 1f - field.widthRatio.coerceAtMost(1f))
                val clampedY = field.yRatio.coerceIn(0f, 1f - field.heightRatio.coerceAtMost(1f))
                signatureFieldDao.insert(
                    SignatureFieldEntity(
                        documentId = documentId,
                        documentSignerId = documentSigner.id,
                        page = field.page,
                        xRatio = clampedX,
                        yRatio = clampedY,
                        widthRatio = field.widthRatio.coerceIn(0.05f, 1f),
                        heightRatio = field.heightRatio.coerceIn(0.02f, 1f)
                    )
                )
            }
        }

    override suspend fun updateInviteMessage(documentId: Long, message: String) = withContext(Dispatchers.IO) {
        val document = documentDao.findById(documentId) ?: return@withContext
        documentDao.update(document.copy(inviteMessage = message))
    }

    override suspend fun send(documentId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val document = documentDao.findById(documentId)
                ?: return@withContext Result.failure(IllegalStateException("Documento não encontrado."))

            val signers = documentSignerDao.getByDocument(documentId)
            if (signers.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("O documento precisa ter ao menos 1 signatário para ser enviado.")
                )
            }
            val fields = signatureFieldDao.getByDocument(documentId)
            if (fields.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Posicione ao menos um campo de assinatura antes de enviar.")
                )
            }
            if (!File(document.filePath).exists()) {
                return@withContext Result.failure(IllegalStateException("Arquivo PDF do documento não foi encontrado."))
            }

            // Envio idempotente: reenviar um documento já enviado apenas atualiza o
            // registro existente (sem duplicar solicitações/registros).
            documentDao.update(
                document.copy(
                    status = DocumentStatus.SENT,
                    sentAt = System.currentTimeMillis()
                )
            )
            documentEventDao.insert(
                DocumentEventEntity(
                    documentId = documentId,
                    type = DocumentEventType.SENT,
                    description = "Documento enviado para ${signers.size} signatário(s)."
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            documentDao.findById(documentId)?.let {
                documentDao.update(it.copy(status = DocumentStatus.SENDING_FAILED))
            }
            documentEventDao.insert(
                DocumentEventEntity(
                    documentId = documentId,
                    type = DocumentEventType.SEND_FAILED,
                    description = e.message ?: "Falha ao enviar o documento."
                )
            )
            Result.failure(e)
        }
    }

    override suspend fun createNewVersion(documentId: Long): Result<Long> = withContext(Dispatchers.IO) {
        val original = documentDao.findById(documentId)
            ?: return@withContext Result.failure(IllegalStateException("Documento original não encontrado."))

        val newFilePath = pdfFileStorage.duplicate(original.filePath)
        val newId = documentDao.insert(
            DocumentEntity(
                title = original.title,
                originalFileName = original.originalFileName,
                filePath = newFilePath,
                pageCount = original.pageCount,
                ownerAdminId = original.ownerAdminId,
                inviteMessage = original.inviteMessage,
                status = DocumentStatus.DRAFT,
                version = original.version + 1,
                parentDocumentId = original.id
            )
        )

        val oldSigners = documentSignerDao.getByDocument(documentId)
        val idMap = mutableMapOf<Long, Long>()
        oldSigners.forEach { signer ->
            val newSignerRowId = documentSignerDao.insert(
                DocumentSignerEntity(
                    documentId = newId,
                    signerId = signer.signerId,
                    classification = signer.classification
                )
            )
            idMap[signer.id] = newSignerRowId
        }

        val oldFields = signatureFieldDao.getByDocument(documentId)
        oldFields.forEach { field ->
            val mappedSignerId = idMap[field.documentSignerId] ?: return@forEach
            signatureFieldDao.insert(
                field.copy(id = 0, documentId = newId, documentSignerId = mappedSignerId, signedImagePath = null)
            )
        }

        documentEventDao.insert(
            DocumentEventEntity(
                documentId = newId,
                type = DocumentEventType.NEW_VERSION,
                description = "Nova versão (v${original.version + 1}) criada a partir do documento #${original.id}."
            )
        )
        Result.success(newId)
    }

    override fun observeDocumentsOfAdmin(adminId: Long): Flow<List<DocumentEntity>> =
        documentDao.observeByAdmin(adminId)

    override fun observeDocument(documentId: Long): Flow<DocumentEntity?> = documentDao.observeById(documentId)

    override fun observeSigners(documentId: Long): Flow<List<DocumentSignerEntity>> =
        documentSignerDao.observeByDocument(documentId)

    override fun observeFields(documentId: Long): Flow<List<SignatureFieldEntity>> =
        signatureFieldDao.observeByDocument(documentId)

    override fun observeEvents(documentId: Long): Flow<List<DocumentEventEntity>> =
        documentEventDao.observeByDocument(documentId)

    override suspend fun getDocument(documentId: Long): DocumentEntity? = documentDao.findById(documentId)

    override suspend fun getFields(documentId: Long): List<SignatureFieldEntity> =
        signatureFieldDao.getByDocument(documentId)

    override suspend fun getSigners(documentId: Long): List<DocumentSignerEntity> =
        documentSignerDao.getByDocument(documentId)

    override suspend fun getFieldsForDocumentSigner(documentSignerId: Long): List<SignatureFieldEntity> =
        signatureFieldDao.getByDocumentSigner(documentSignerId)

    override fun observeDocumentSignersFor(signerId: Long): Flow<List<DocumentSignerEntity>> =
        documentSignerDao.observePendingAndSignedForSigner(signerId)

    override suspend fun submitSignature(
        documentId: Long,
        documentSignerId: Long,
        signatureImagesByFieldId: Map<Long, String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val document = documentDao.findById(documentId)
                ?: return@withContext Result.failure(IllegalStateException("Documento não encontrado."))
            val documentSigner = documentSignerDao.findById(documentSignerId)
                ?: return@withContext Result.failure(IllegalStateException("Signatário não encontrado neste documento."))

            val fields = signatureFieldDao.getByDocumentSigner(documentSignerId)
            if (fields.any { !signatureImagesByFieldId.containsKey(it.id) }) {
                return@withContext Result.failure(IllegalStateException("Assine todos os campos destacados antes de concluir."))
            }

            // Persiste as imagens de assinatura nos campos.
            fields.forEach { field ->
                val imagePath = signatureImagesByFieldId[field.id] ?: return@forEach
                signatureFieldDao.update(field.copy(signedImagePath = imagePath))
            }

            // Carimba as assinaturas no PDF de trabalho do documento.
            val stampRequests = fields.map { field ->
                PdfStampingUtil.StampRequest(
                    page = field.page,
                    xRatio = field.xRatio,
                    yRatio = field.yRatio,
                    widthRatio = field.widthRatio,
                    heightRatio = field.heightRatio,
                    signatureImagePath = signatureImagesByFieldId.getValue(field.id)
                )
            }
            val outputPath = document.filePath
            PdfStampingUtil.stamp(document.filePath, outputPath, stampRequests)

            documentSignerDao.update(
                documentSigner.copy(status = SignatureStatus.SIGNED, signedAt = System.currentTimeMillis())
            )
            documentEventDao.insert(
                DocumentEventEntity(
                    documentId = documentId,
                    type = DocumentEventType.SIGNER_SIGNED,
                    description = "Signatário concluiu a assinatura."
                )
            )

            val pendingCount = documentSignerDao.countPending(documentId, SignatureStatus.SIGNED)
            val newStatus = if (pendingCount == 0) DocumentStatus.COMPLETED else DocumentStatus.PARTIALLY_SIGNED
            documentDao.update(
                document.copy(
                    status = newStatus,
                    completedAt = if (newStatus == DocumentStatus.COMPLETED) System.currentTimeMillis() else document.completedAt
                )
            )
            if (newStatus == DocumentStatus.COMPLETED) {
                documentEventDao.insert(
                    DocumentEventEntity(
                        documentId = documentId,
                        type = DocumentEventType.COMPLETED,
                        description = "Todos os signatários concluíram a assinatura. Documento concluído."
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
