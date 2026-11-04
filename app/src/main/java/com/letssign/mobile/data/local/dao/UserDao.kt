package com.letssign.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.letssign.mobile.data.local.entity.UserEntity
import com.letssign.mobile.data.local.entity.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role ORDER BY fullName ASC")
    fun observeByRole(role: UserRole): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role AND createdByAdminId = :adminId ORDER BY fullName ASC")
    fun observeSignersOfAdmin(role: UserRole, adminId: Long): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'")
    suspend fun countAdmins(): Int

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<Long>): List<UserEntity>

    @Query("SELECT * FROM users WHERE email = :email AND id != :excludingId COLLATE NOCASE LIMIT 1")
    suspend fun findByEmailExcluding(email: String, excludingId: Long): UserEntity?
}
