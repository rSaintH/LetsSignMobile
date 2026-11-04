package com.letssign.mobile.data.repository

import com.letssign.mobile.data.local.entity.UserEntity
import com.letssign.mobile.data.local.entity.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acesso a usuários (Admin/Signatário). A implementação atual
 * usa Room (ver UserRepositoryImpl); na Sprint 2 uma implementação baseada
 * em API REST poderá ser plugada sem alterar as telas/ViewModels.
 */
interface UserRepository {
    suspend fun login(email: String, password: String): Result<UserEntity>
    suspend fun registerSigner(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        createdByAdminId: Long
    ): Result<UserEntity>

    suspend fun updateSigner(
        signerId: Long,
        fullName: String,
        email: String,
        phone: String,
        newPassword: String?
    ): Result<Unit>

    fun observeSignersOfAdmin(adminId: Long): Flow<List<UserEntity>>
    suspend fun findById(id: Long): UserEntity?
    suspend fun findByIds(ids: List<Long>): List<UserEntity>
}
