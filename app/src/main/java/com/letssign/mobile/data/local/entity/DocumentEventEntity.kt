package com.letssign.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class DocumentEventType {
    CREATED, SENT, SEND_FAILED, SIGNER_SIGNED, COMPLETED, NEW_VERSION
}

/**
 * Histórico de eventos do documento, usado na tela de acompanhamento
 * (datas/horários dos principais eventos: criação, envio, assinatura...).
 */
@Entity(
    tableName = "document_events",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DocumentEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val type: DocumentEventType,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
