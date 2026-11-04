package com.letssign.mobile.ui.admin.signers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letssign.mobile.data.local.entity.UserEntity
import com.letssign.mobile.data.repository.UserRepository
import com.letssign.mobile.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SignersViewModel(
    private val userRepository: UserRepository,
    private val adminId: Long
) : ViewModel() {

    val signers: StateFlow<List<UserEntity>> = userRepository.observeSignersOfAdmin(adminId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(SignerFormState())
    val formState: StateFlow<SignerFormState> = _formState

    fun loadForEdit(signer: UserEntity) {
        _formState.value = SignerFormState(
            id = signer.id,
            fullName = signer.fullName,
            email = signer.email,
            phone = signer.phone,
            password = "",
            isEditing = true
        )
    }

    fun startNew() {
        _formState.value = SignerFormState()
    }

    fun onFullNameChange(v: String) { _formState.value = _formState.value.copy(fullName = v, errorMessage = null) }
    fun onEmailChange(v: String) { _formState.value = _formState.value.copy(email = v, errorMessage = null) }
    fun onPhoneChange(v: String) { _formState.value = _formState.value.copy(phone = v, errorMessage = null) }
    fun onPasswordChange(v: String) { _formState.value = _formState.value.copy(password = v, errorMessage = null) }

    fun save(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.fullName.isBlank()) {
            _formState.value = state.copy(errorMessage = "Informe o nome completo."); return
        }
        if (!Validators.isValidEmail(state.email)) {
            _formState.value = state.copy(errorMessage = "Informe um e-mail válido."); return
        }
        if (!Validators.isValidPhone(state.phone)) {
            _formState.value = state.copy(errorMessage = "Informe um telefone válido."); return
        }
        if (!state.isEditing && !Validators.isValidPassword(state.password)) {
            _formState.value = state.copy(errorMessage = "A senha deve ter ao menos 6 caracteres."); return
        }
        if (state.isEditing && state.password.isNotBlank() && !Validators.isValidPassword(state.password)) {
            _formState.value = state.copy(errorMessage = "A nova senha deve ter ao menos 6 caracteres."); return
        }

        _formState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val result = if (state.isEditing && state.id != null) {
                userRepository.updateSigner(
                    signerId = state.id,
                    fullName = state.fullName,
                    email = state.email,
                    phone = state.phone,
                    newPassword = state.password.ifBlank { null }
                )
            } else {
                userRepository.registerSigner(
                    fullName = state.fullName,
                    email = state.email,
                    phone = state.phone,
                    password = state.password,
                    createdByAdminId = adminId
                ).map { }
            }
            result.onSuccess {
                _formState.value = SignerFormState()
                onSuccess()
            }.onFailure {
                _formState.value = _formState.value.copy(isSaving = false, errorMessage = it.message)
            }
        }
    }
}

data class SignerFormState(
    val id: Long? = null,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
