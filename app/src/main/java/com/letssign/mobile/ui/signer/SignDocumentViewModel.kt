package com.letssign.mobile.ui.signer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letssign.mobile.data.local.entity.DocumentEntity
import com.letssign.mobile.data.local.entity.DocumentSignerEntity
import com.letssign.mobile.data.local.entity.SignatureFieldEntity
import com.letssign.mobile.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SignDocumentUiState(
    val isLoading: Boolean = true,
    val document: DocumentEntity? = null,
    val documentSigner: DocumentSignerEntity? = null,
    val fields: List<SignatureFieldEntity> = emptyList(),
    val signedImagesByFieldId: Map<Long, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false
)

class SignDocumentViewModel(
    private val documentRepository: DocumentRepository,
    private val documentSignerId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignDocumentUiState())
    val uiState: StateFlow<SignDocumentUiState> = _uiState

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val fields = documentRepository.getFieldsForDocumentSigner(documentSignerId)
            val documentId = fields.firstOrNull()?.documentId
            val document = documentId?.let { documentRepository.getDocument(it) }
            val signers = documentId?.let { documentRepository.getSigners(it) }
            val documentSigner = signers?.firstOrNull { it.id == documentSignerId }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                document = document,
                documentSigner = documentSigner,
                fields = fields
            )
        }
    }

    fun onFieldSigned(fieldId: Long, imagePath: String) {
        _uiState.value = _uiState.value.copy(
            signedImagesByFieldId = _uiState.value.signedImagesByFieldId + (fieldId to imagePath)
        )
    }

    fun allFieldsSigned(): Boolean {
        val state = _uiState.value
        return state.fields.isNotEmpty() && state.fields.all { state.signedImagesByFieldId.containsKey(it.id) }
    }

    fun submit() {
        val state = _uiState.value
        val document = state.document ?: return
        if (!allFieldsSigned()) {
            _uiState.value = state.copy(errorMessage = "Assine todos os campos destacados antes de concluir.")
            return
        }
        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            documentRepository.submitSignature(document.id, documentSignerId, state.signedImagesByFieldId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, completed = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = error.message)
                }
        }
    }
}
