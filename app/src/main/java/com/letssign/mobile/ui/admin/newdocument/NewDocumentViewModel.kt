package com.letssign.mobile.ui.admin.newdocument

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letssign.mobile.data.local.entity.SignerClassification
import com.letssign.mobile.data.local.entity.UserEntity
import com.letssign.mobile.data.repository.DocumentRepository
import com.letssign.mobile.data.repository.PendingSignatureField
import com.letssign.mobile.data.repository.PendingSignerSelection
import com.letssign.mobile.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Representação em memória de um campo de assinatura durante a edição do wizard. */
data class FieldDraft(
    val key: String = UUID.randomUUID().toString(),
    val signerId: Long,
    val page: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float = DEFAULT_WIDTH_RATIO,
    val heightRatio: Float = DEFAULT_HEIGHT_RATIO
) {
    companion object {
        const val DEFAULT_WIDTH_RATIO = 0.42f
        const val DEFAULT_HEIGHT_RATIO = 0.08f
    }
}

data class SelectedSigner(
    val signer: UserEntity,
    val classification: SignerClassification
)

data class NewDocumentUiState(
    val title: String = "",
    val titleConfirmed: Boolean = false,
    val pdfFileName: String? = null,
    val pdfPath: String? = null,
    val pageCount: Int = 0,
    val documentId: Long? = null,
    val selectedSigners: List<SelectedSigner> = emptyList(),
    val fields: List<FieldDraft> = emptyList(),
    val inviteMessage: String = "Olá! Você recebeu um documento da LetsSign para assinatura. Por favor, revise e assine.",
    val isProcessingPdf: Boolean = false,
    val isSavingStep: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val sendSuccess: Boolean? = null
)

class NewDocumentViewModel(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val adminId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewDocumentUiState())
    val uiState: StateFlow<NewDocumentUiState> = _uiState

    val availableSigners: StateFlow<List<UserEntity>> = userRepository.observeSignersOfAdmin(adminId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value, errorMessage = null)
    }

    fun onInviteMessageChange(value: String) {
        _uiState.value = _uiState.value.copy(inviteMessage = value)
    }

    fun importPdf(uri: Uri, displayName: String) {
        _uiState.value = _uiState.value.copy(isProcessingPdf = true, errorMessage = null)
        viewModelScope.launch {
            documentRepository.importPdf(uri, displayName)
                .onSuccess { imported ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPdf = false,
                        pdfPath = imported.filePath,
                        pdfFileName = imported.originalFileName,
                        pageCount = imported.pageCount
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPdf = false,
                        pdfPath = null,
                        pdfFileName = null,
                        pageCount = 0,
                        errorMessage = error.message ?: "Arquivo inválido ou corrompido."
                    )
                }
        }
    }

    /** Valida título + PDF e cria (ou reaproveita) o documento em rascunho. */
    fun confirmTitleAndPdf(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Informe o título do documento."); return
        }
        if (state.pdfPath == null) {
            _uiState.value = state.copy(errorMessage = "Selecione um arquivo PDF válido."); return
        }
        if (state.documentId != null) {
            onDone(); return
        }
        _uiState.value = state.copy(isSavingStep = true, errorMessage = null)
        viewModelScope.launch {
            val id = documentRepository.createDraft(
                title = state.title,
                importedFilePath = state.pdfPath,
                originalFileName = state.pdfFileName ?: "documento.pdf",
                pageCount = state.pageCount,
                ownerAdminId = adminId,
                inviteMessage = state.inviteMessage
            )
            _uiState.value = _uiState.value.copy(isSavingStep = false, documentId = id, titleConfirmed = true)
            onDone()
        }
    }

    fun toggleSigner(signer: UserEntity, checked: Boolean) {
        val state = _uiState.value
        if (checked) {
            val updated = state.selectedSigners + SelectedSigner(signer, SignerClassification.CONTRATADO)
            _uiState.value = state.copy(selectedSigners = updated)
        } else {
            val remaining = state.selectedSigners.filterNot { it.signer.id == signer.id }
            _uiState.value = state.copy(
                selectedSigners = remaining,
                fields = state.fields.filterNot { it.signerId == signer.id }
            )
        }
    }

    fun changeClassification(signerId: Long, classification: SignerClassification) {
        _uiState.value = _uiState.value.copy(
            selectedSigners = _uiState.value.selectedSigners.map {
                if (it.signer.id == signerId) it.copy(classification = classification) else it
            }
        )
    }

    fun confirmSigners(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.selectedSigners.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Selecione ao menos 1 signatário.")
            return
        }
        val documentId = state.documentId ?: return
        _uiState.value = state.copy(isSavingStep = true, errorMessage = null)
        viewModelScope.launch {
            documentRepository.saveSigners(
                documentId,
                state.selectedSigners.map { PendingSignerSelection(it.signer.id, it.classification) }
            )
            _uiState.value = _uiState.value.copy(isSavingStep = false)
            onDone()
        }
    }

    fun addField(signerId: Long, page: Int, xRatio: Float, yRatio: Float) {
        val state = _uiState.value
        val clampedX = xRatio.coerceIn(0f, 1f - FieldDraft.DEFAULT_WIDTH_RATIO)
        val clampedY = yRatio.coerceIn(0f, 1f - FieldDraft.DEFAULT_HEIGHT_RATIO)
        _uiState.value = state.copy(
            fields = state.fields + FieldDraft(signerId = signerId, page = page, xRatio = clampedX, yRatio = clampedY)
        )
    }

    fun moveField(key: String, xRatio: Float, yRatio: Float) {
        _uiState.value = _uiState.value.copy(
            fields = _uiState.value.fields.map { field ->
                if (field.key == key) {
                    field.copy(
                        xRatio = xRatio.coerceIn(0f, 1f - field.widthRatio),
                        yRatio = yRatio.coerceIn(0f, 1f - field.heightRatio)
                    )
                } else field
            }
        )
    }

    fun deleteField(key: String) {
        _uiState.value = _uiState.value.copy(fields = _uiState.value.fields.filterNot { it.key == key })
    }

    fun confirmFields(onDone: () -> Unit) {
        val state = _uiState.value
        val documentId = state.documentId ?: return
        if (state.fields.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Posicione ao menos um campo de assinatura.")
            return
        }
        _uiState.value = state.copy(isSavingStep = true, errorMessage = null)
        viewModelScope.launch {
            documentRepository.saveFields(
                documentId,
                state.fields.map {
                    PendingSignatureField(
                        signerId = it.signerId,
                        page = it.page,
                        xRatio = it.xRatio,
                        yRatio = it.yRatio,
                        widthRatio = it.widthRatio,
                        heightRatio = it.heightRatio
                    )
                }
            )
            _uiState.value = _uiState.value.copy(isSavingStep = false)
            onDone()
        }
    }

    fun saveInviteMessage() {
        val documentId = _uiState.value.documentId ?: return
        viewModelScope.launch {
            documentRepository.updateInviteMessage(documentId, _uiState.value.inviteMessage)
        }
    }

    fun send() {
        val documentId = _uiState.value.documentId ?: return
        _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)
        viewModelScope.launch {
            documentRepository.updateInviteMessage(documentId, _uiState.value.inviteMessage)
            val result = documentRepository.send(documentId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSending = false, sendSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    sendSuccess = false,
                    errorMessage = error.message ?: "Falha ao enviar o documento."
                )
            }
        }
    }
}
