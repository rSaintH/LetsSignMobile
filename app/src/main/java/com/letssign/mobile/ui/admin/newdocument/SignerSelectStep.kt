package com.letssign.mobile.ui.admin.newdocument

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letssign.mobile.data.local.entity.SignerClassification
import com.letssign.mobile.data.local.entity.UserEntity
import com.letssign.mobile.ui.common.EmptyState
import com.letssign.mobile.ui.common.PrimaryButton

private fun classificationLabel(classification: SignerClassification): String =
    classificationLabelPublic(classification)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignerSelectStep(
    viewModel: NewDocumentViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val availableSigners by viewModel.availableSigners.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Signatários (2/4)") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (availableSigners.isEmpty()) {
                EmptyState("Nenhum signatário cadastrado.\nCadastre signatários antes de criar o documento.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(availableSigners, key = { it.id }) { signer ->
                        val selected = state.selectedSigners.firstOrNull { it.signer.id == signer.id }
                        SignerSelectRow(
                            signer = signer,
                            classification = selected?.classification,
                            onToggle = { checked -> viewModel.toggleSigner(signer, checked) },
                            onClassificationChange = { viewModel.changeClassification(signer.id, it) }
                        )
                    }
                }
            }

            state.errorMessage?.let {
                Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                "Selecionados: ${state.selectedSigners.size} (mínimo 1 obrigatório)",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(text = "Continuar", isLoading = state.isSavingStep, onClick = { viewModel.confirmSigners(onNext) })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignerSelectRow(
    signer: UserEntity,
    classification: SignerClassification?,
    onToggle: (Boolean) -> Unit,
    onClassificationChange: (SignerClassification) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isChecked = classification != null

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = isChecked, onCheckedChange = onToggle)
                Column(modifier = Modifier.weight(1f)) {
                    Text(signer.fullName, style = MaterialTheme.typography.titleMedium)
                    Text(signer.email, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (isChecked) {
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = classificationLabel(classification!!),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Classificação") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        SignerClassification.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(classificationLabel(option)) },
                                onClick = {
                                    onClassificationChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
