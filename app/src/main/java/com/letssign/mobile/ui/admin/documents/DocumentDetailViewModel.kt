package com.letssign.mobile.ui.admin.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letssign.mobile.data.local.entity.DocumentEntity
import com.letssign.mobile.data.local.entity.DocumentEventEntity
import com.letssign.mobile.data.local.entity.DocumentSignerEntity
import com.letssign.mobile.data.local.entity.SignatureFieldEntity
import com.letssign.mobile.data.local.entity.UserEntity
import com.letssign.mobile.data.repository.DocumentRepository
import com.letssign.mobile.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SignerTrackingRow(val documentSigner: DocumentSignerEntity, val signer: UserEntity?)

data class DocumentDetailUiState(
    val document: DocumentEntity? = null,
    val signers: List<SignerTrackingRow> = emptyList(),
    val fields: List<SignatureFieldEntity> = emptyList(),
    val events: List<DocumentEventEntity> = emptyList(),
    val isRetrying: Boolean = false,
    val retryError: String? = null
)

class DocumentDetailViewModel(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    private val documentId: Long
) : ViewModel() {

    val uiState: StateFlow<DocumentDetailUiState> = combine(
        documentRepository.observeDocument(documentId),
        documentRepository.observeSigners(documentId).map { signers ->
            signers.map { ds -> SignerTrackingRow(ds, userRepository.findById(ds.signerId)) }
        },
        documentRepository.observeFields(documentId),
        documentRepository.observeEvents(documentId)
    ) { document, signers, fields, events ->
        DocumentDetailUiState(document = document, signers = signers, fields = fields, events = events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DocumentDetailUiState())

    fun retrySend(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = documentRepository.send(documentId)
            onResult(result.isSuccess)
        }
    }

    fun createNewVersion(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            documentRepository.createNewVersion(documentId).onSuccess { onCreated(it) }
        }
    }
}
