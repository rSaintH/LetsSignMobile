package com.letssign.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DocumentStatus {
    DRAFT,              // em preparação, ainda não enviado
    SENDING_FAILED,     // tentativa de envio falhou, pode tentar novamente
    SENT,               // enviado, nenhum signatário assinou ainda
    PARTIALLY_SIGNED,   // ao menos 1 signatário assinou, mas não todos
    COMPLETED           // todos os signatários obrigatórios assinaram
}

/**
 * Documento PDF cadastrado pelo Admin para coleta de assinaturas.
 *
 * Persistência do PDF: arquivo salvo no sistema de arquivos do device
 * (ver PdfFileStorage); aqui guardamos apenas caminho, nome e metadados,
 * conforme a opção 1 de armazenamento descrita no desafio.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalFileName: String,
    val filePath: String,
    val pageCount: Int,
    val ownerAdminId: Long,
    val inviteMessage: String,
    val status: DocumentStatus = DocumentStatus.DRAFT,
    val version: Int = 1,
    val parentDocumentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sentAt: Long? = null,
    val completedAt: Long? = null
)
