package com.letssign.mobile.ui.admin.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letssign.mobile.data.local.entity.DocumentEntity
import com.letssign.mobile.data.repository.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DocumentsViewModel(
    documentRepository: DocumentRepository,
    adminId: Long
) : ViewModel() {

    val documents: StateFlow<List<DocumentEntity>> = documentRepository.observeDocumentsOfAdmin(adminId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
