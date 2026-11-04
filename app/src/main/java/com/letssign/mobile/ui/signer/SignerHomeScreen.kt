package com.letssign.mobile.ui.signer

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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.letssign.mobile.data.local.entity.SignatureStatus
import com.letssign.mobile.di.AppContainer
import com.letssign.mobile.session.SessionUser
import com.letssign.mobile.ui.common.EmptyState
import com.letssign.mobile.ui.common.StatusChip
import com.letssign.mobile.ui.common.documentStatusColor
import com.letssign.mobile.ui.common.documentStatusLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignerHomeScreen(
    container: AppContainer,
    signer: SessionUser,
    onOpenDocument: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val viewModel: SignerDocumentsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SignerDocumentsViewModel(container.documentRepository, signer.id) }
        }
    )
    val items by viewModel.items.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    val pending = items.filter { it.documentSigner.status == SignatureStatus.PENDING }
    val signed = items.filter { it.documentSigner.status == SignatureStatus.SIGNED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Olá, ${signer.fullName.substringBefore(" ")}") },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, contentDescription = "Sair") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Pendentes (${pending.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Assinados (${signed.size})") })
            }

            val list = if (tab == 0) pending else signed
            Box(modifier = Modifier.fillMaxSize()) {
                if (list.isEmpty()) {
                    EmptyState(if (tab == 0) "Nenhum documento pendente de assinatura." else "Nenhum documento assinado ainda.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(list, key = { it.documentSigner.id }) { item ->
                            val document = item.document ?: return@items
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                onClick = { onOpenDocument(item.documentSigner.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(document.title, style = MaterialTheme.typography.titleMedium)
                                        Text("Classificação: " + item.documentSigner.classification.name, style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusChip(documentStatusLabel(document.status), documentStatusColor(document.status))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
