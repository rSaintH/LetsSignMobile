package com.letssign.mobile.ui.admin.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.letssign.mobile.data.local.entity.DocumentEntity
import com.letssign.mobile.di.AppContainer
import com.letssign.mobile.ui.common.EmptyState
import com.letssign.mobile.ui.common.StatusChip
import com.letssign.mobile.ui.common.documentStatusColor
import com.letssign.mobile.ui.common.documentStatusLabel
import com.letssign.mobile.util.DateFormat

@Composable
fun DocumentsListScreen(
    container: AppContainer,
    adminId: Long,
    onNewDocument: () -> Unit,
    onOpenDocument: (Long) -> Unit
) {
    val viewModel: DocumentsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DocumentsViewModel(container.documentRepository, adminId) }
        }
    )
    val documents by viewModel.documents.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewDocument) {
                Icon(Icons.Filled.Add, contentDescription = "Novo documento")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (documents.isEmpty()) {
                EmptyState("Nenhum documento cadastrado.\nToque em + para enviar um PDF para assinatura.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(documents, key = { it.id }) { document ->
                        DocumentRow(document, onClick = { onOpenDocument(document.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(document: DocumentEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(document.title, style = MaterialTheme.typography.titleMedium)
                    Text("v${document.version} • criado em ${DateFormat.format(document.createdAt)}", style = MaterialTheme.typography.bodySmall)
                }
            }
            StatusChip(documentStatusLabel(document.status), documentStatusColor(document.status))
        }
    }
}
