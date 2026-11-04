package com.letssign.mobile.data.repository

import com.letssign.mobile.data.local.dao.UserDao
import com.letssign.mobile.data.local.entity.UserEntity
import com.letssign.mobile.data.local.entity.UserRole
import kotlinx.coroutines.flow.Flow
import org.mindrot.jbcrypt.BCrypt

class UserRepositoryImpl(private val userDao: UserDao) : UserRepository {

    override suspend fun login(email: String, password: String): Result<UserEntity> {
        val user = userDao.findByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("E-mail ou senha inválidos."))
        return if (BCrypt.checkpw(password, user.passwordHash)) {
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("E-mail ou senha inválidos."))
        }
    }

    override suspend fun registerSigner(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        createdByAdminId: Long
    ): Result<UserEntity> {
        val existing = userDao.findByEmail(email.trim())
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Já existe um usuário com este e-mail."))
        }
        val entity = UserEntity(
            fullName = fullName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
            role = UserRole.SIGNER,
            createdByAdminId = createdByAdminId
        )
        val id = userDao.insert(entity)
        return Result.success(entity.copy(id = id))
    }

    override suspend fun updateSigner(
        signerId: Long,
        fullName: String,
        email: String,
        phone: String,
        newPassword: String?
    ): Result<Unit> {
        val current = userDao.findById(signerId)
            ?: return Result.failure(IllegalArgumentException("Signatário não encontrado."))
        val conflict = userDao.findByEmailExcluding(email.trim(), signerId)
        if (conflict != null) {
            return Result.failure(IllegalArgumentException("Já existe outro usuário com este e-mail."))
        }
        val updated = current.copy(
            fullName = fullName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            passwordHash = if (!newPassword.isNullOrBlank()) BCrypt.hashpw(newPassword, BCrypt.gensalt()) else current.passwordHash,
            updatedAt = System.currentTimeMillis()
        )
        userDao.update(updated)
        return Result.success(Unit)
    }

    override fun observeSignersOfAdmin(adminId: Long): Flow<List<UserEntity>> =
        userDao.observeSignersOfAdmin(UserRole.SIGNER, adminId)

    override suspend fun findById(id: Long): UserEntity? = userDao.findById(id)

    override suspend fun findByIds(ids: List<Long>): List<UserEntity> = userDao.findByIds(ids)
}
