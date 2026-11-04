package com.letssign.mobile.ui.signer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letssign.mobile.data.local.entity.DocumentEntity
import com.letssign.mobile.data.local.entity.DocumentSignerEntity
import com.letssign.mobile.data.repository.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SignerDocumentItem(val documentSigner: DocumentSignerEntity, val document: DocumentEntity?)

class SignerDocumentsViewModel(
    documentRepository: DocumentRepository,
    signerId: Long
) : ViewModel() {

    val items: StateFlow<List<SignerDocumentItem>> = documentRepository.observeDocumentSignersFor(signerId)
        .map { list -> list.map { ds -> SignerDocumentItem(ds, documentRepository.getDocument(ds.documentId)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
