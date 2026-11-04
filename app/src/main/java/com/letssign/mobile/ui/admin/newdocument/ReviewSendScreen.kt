package com.letssign.mobile.ui.admin.newdocument

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letssign.mobile.ui.admin.newdocument.classificationLabelPublic
import com.letssign.mobile.ui.common.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSendScreen(
    viewModel: NewDocumentViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.sendSuccess) {
        if (state.sendSuccess == true) {
            onFinished()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Revisão e envio (4/4)") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionCard(title = "Documento") {
                Text("Título: ${state.title}")
                Text("Arquivo: ${state.pdfFileName}")
                Text("Páginas: ${state.pageCount}")
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionCard(title = "Signatários (${state.selectedSigners.size})") {
                state.selectedSigners.forEach {
                    Text("• ${it.signer.fullName} — ${classificationLabelPublic(it.classification)} (${it.signer.email})")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionCard(title = "Campos de assinatura (${state.fields.size})") {
                state.fields
                    .groupBy { it.signerId }
                    .forEach { (signerId, fields) ->
                        val name = state.selectedSigners.firstOrNull { it.signer.id == signerId }?.signer?.fullName ?: "Signatário"
                        Text("• $name: ${fields.size} campo(s) em ${fields.map { it.page + 1 }.distinct().sorted()}")
                    }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Mensagem do convite", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.inviteMessage,
                onValueChange = viewModel::onInviteMessageChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            if (state.sendSuccess == false) {
                Spacer(modifier = Modifier.height(12.dp))
                Card {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Falha ao enviar", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                            Text(
                                state.errorMessage ?: "Tente novamente.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Você pode tentar novamente sem duplicar a solicitação.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = if (state.sendSuccess == false) "Tentar novamente" else "Confirmar e enviar",
                isLoading = state.isSending,
                onClick = { viewModel.send() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !state.isSending) {
                Text("Voltar")
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
