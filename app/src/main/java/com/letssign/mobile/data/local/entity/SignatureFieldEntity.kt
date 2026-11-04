package com.letssign.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Campo de assinatura posicionado sobre uma página do PDF.
 *
 * Requisito técnico: page, x, y, width e height são salvos como RATIOS
 * (0f..1f) em relação ao tamanho da página do PDF, e não em pixels de tela.
 * Isso garante que a posição seja preservada ao abrir o documento em
 * qualquer outro dispositivo/tamanho de tela.
 */
@Entity(
    tableName = "signature_fields",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DocumentSignerEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentSignerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SignatureFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val documentSignerId: Long,
    val page: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float,
    val signedImagePath: String? = null
)
