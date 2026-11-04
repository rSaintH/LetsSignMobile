package com.letssign.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN, SIGNER
}

/**
 * Representa tanto o Admin quanto o Signatário.
 * O Admin é "seedado" na criação do banco (ver AppDatabase.Callback).
 * Signatários são cadastrados pelo Admin (funcionalidade 2 do desafio).
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val role: UserRole,
    val createdByAdminId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
