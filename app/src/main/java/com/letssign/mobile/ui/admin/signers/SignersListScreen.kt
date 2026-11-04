package com.letssign.mobile.ui.admin.signers

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.letssign.mobile.data.local.entity.UserEntity
import com.letssign.mobile.di.AppContainer
import com.letssign.mobile.ui.common.EmptyState

@Composable
fun SignersListScreen(
    container: AppContainer,
    adminId: Long,
    onAddSigner: () -> Unit,
    onEditSigner: (Long) -> Unit
) {
    val viewModel: SignersViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SignersViewModel(container.userRepository, adminId) }
        }
    )
    val signers by viewModel.signers.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSigner) {
                Icon(Icons.Filled.Add, contentDescription = "Cadastrar signatário")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (signers.isEmpty()) {
                EmptyState("Nenhum signatário cadastrado ainda.\nToque em + para cadastrar.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(signers, key = { it.id }) { signer ->
                        SignerRow(signer, onClick = { onEditSigner(signer.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SignerRow(signer: UserEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(signer.fullName, style = MaterialTheme.typography.titleMedium)
                Text(signer.email, style = MaterialTheme.typography.bodySmall)
                Text(signer.phone, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar")
            }
        }
    }
}
