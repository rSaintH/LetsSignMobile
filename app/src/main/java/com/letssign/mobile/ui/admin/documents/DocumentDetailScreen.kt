package com.letssign.mobile.ui.admin.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.letssign.mobile.data.local.entity.DocumentEventType
import com.letssign.mobile.data.local.entity.DocumentStatus
import com.letssign.mobile.di.AppContainer
import com.letssign.mobile.ui.admin.newdocument.classificationLabelPublic
import com.letssign.mobile.ui.common.PrimaryButton
import com.letssign.mobile.ui.common.StatusChip
import com.letssign.mobile.ui.common.documentStatusColor
import com.letssign.mobile.ui.common.documentStatusLabel
import com.letssign.mobile.ui.common.signatureStatusLabel
import com.letssign.mobile.util.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    container: AppContainer,
    documentId: Long,
    onBack: () -> Unit,
    onNewVersionCreated: (Long) -> Unit
) {
    val viewModel: DocumentDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DocumentDetailViewModel(container.documentRepository, container.userRepository, documentId)
            }
        }
    )
    val state by viewModel.uiState.collectAsState()
    var retrying by remember { mutableStateOf(false) }
    var retryError by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Acompanhamento") }) }) { padding ->
        val document = state.document
        if (document == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(document.title, style = MaterialTheme.typography.titleLarge)
                    Text("Versão ${document.version} • ${document.originalFileName}", style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(documentStatusLabel(document.status), documentStatusColor(document.status))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Datas", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Criado em: ${DateFormat.format(document.createdAt)}")
                    Text("Enviado em: ${DateFormat.format(document.sentAt)}")
                    Text("Concluído em: ${DateFormat.format(document.completedAt)}")
                }
            }

            if (document.status == DocumentStatus.SENDING_FAILED) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("O envio deste documento falhou.", color = MaterialTheme.colorScheme.error)
                        retryError?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Spacer(modifier = Modifier.height(8.dp))
                        PrimaryButton(
                            text = "Tentar enviar novamente",
                            isLoading = retrying,
                            onClick = {
                                retrying = true
                                viewModel.retrySend { success ->
                                    retrying = false
                                    retryError = if (!success) "Falha ao reenviar. Verifique o documento." else null
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Signatários", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            state.signers.forEach { row ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(row.signer?.fullName ?: "Signatário #${row.documentSigner.signerId}")
                            Text(
                                classificationLabelPublic(row.documentSigner.classification),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Assinado em: ${DateFormat.format(row.documentSigner.signedAt)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            signatureStatusLabel(row.documentSigner.status),
                            color = if (row.documentSigner.status.name == "SIGNED") Color(0xFF2E7D32) else Color(0xFFE6A700)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Histórico de eventos", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            state.events.forEach { event ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(DateFormat.format(event.timestamp), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(140.dp))
                    Text(eventLabel(event.type) + " — " + event.description, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            if (document.status != DocumentStatus.DRAFT) {
                OutlinedButton(
                    onClick = { viewModel.createNewVersion(onNewVersionCreated) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Criar nova versão deste documento")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar")
            }
        }
    }
}

private fun eventLabel(type: DocumentEventType): String = when (type) {
    DocumentEventType.CREATED -> "Criação"
    DocumentEventType.SENT -> "Envio"
    DocumentEventType.SEND_FAILED -> "Falha no envio"
    DocumentEventType.SIGNER_SIGNED -> "Assinatura"
    DocumentEventType.COMPLETED -> "Conclusão"
    DocumentEventType.NEW_VERSION -> "Nova versão"
}
