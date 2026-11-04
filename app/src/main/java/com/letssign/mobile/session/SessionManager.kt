package com.letssign.mobile.session

import android.content.Context
import com.letssign.mobile.data.local.entity.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SessionUser(
    val id: Long,
    val fullName: String,
    val email: String,
    val role: UserRole
)

/**
 * Mantém o usuário autenticado em memória + persiste o id/role em
 * SharedPreferences para restaurar a sessão ao reabrir o app.
 */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("letssign_session", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(restore())
    val currentUser: StateFlow<SessionUser?> = _currentUser

    private fun restore(): SessionUser? {
        val id = prefs.getLong(KEY_ID, -1L)
        if (id == -1L) return null
        val roleName = prefs.getString(KEY_ROLE, null) ?: return null
        return SessionUser(
            id = id,
            fullName = prefs.getString(KEY_NAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            role = UserRole.valueOf(roleName)
        )
    }

    fun login(user: SessionUser) {
        prefs.edit()
            .putLong(KEY_ID, user.id)
            .putString(KEY_NAME, user.fullName)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_ROLE, user.role.name)
            .apply()
        _currentUser.value = user
    }

    fun logout() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE = "role"
    }
}
