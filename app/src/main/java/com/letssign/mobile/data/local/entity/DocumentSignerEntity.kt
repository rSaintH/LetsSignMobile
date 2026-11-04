package com.letssign.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class SignerClassification {
    CONTRATANTE, CONTRATADO, TESTEMUNHA
}

enum class SignatureStatus {
    PENDING, SIGNED
}

/**
 * Associação N:N entre Document e User(role=SIGNER), com a classificação
 * (contratante/contratado/testemunha) e o status individual de assinatura.
 */
@Entity(
    tableName = "document_signers",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DocumentSignerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val signerId: Long,
    val classification: SignerClassification,
    val status: SignatureStatus = SignatureStatus.PENDING,
    val signedAt: Long? = null
)
