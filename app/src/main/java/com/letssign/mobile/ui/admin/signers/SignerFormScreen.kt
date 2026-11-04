package com.letssign.mobile.ui.admin.signers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.letssign.mobile.di.AppContainer
import com.letssign.mobile.ui.common.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignerFormScreen(
    container: AppContainer,
    adminId: Long,
    signerId: Long?,
    onBack: () -> Unit
) {
    val viewModel: SignersViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SignersViewModel(container.userRepository, adminId) }
        }
    )
    val signers by viewModel.signers.collectAsState()
    val formState by viewModel.formState.collectAsState()
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(signerId, signers) {
        if (initialized && (signerId == null || signerId == -1L)) return@LaunchedEffect
        if (signerId != null && signerId != -1L) {
            signers.firstOrNull { it.id == signerId }?.let {
                viewModel.loadForEdit(it)
                initialized = true
            }
        } else {
            viewModel.startNew()
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (formState.isEditing) "Editar signatário" else "Cadastrar signatário") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = formState.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = { Text("Nome completo *") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("E-mail (login) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Telefone *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(if (formState.isEditing) "Nova senha (opcional)" else "Senha *") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            formState.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(
                text = if (formState.isEditing) "Salvar alterações" else "Cadastrar signatário",
                isLoading = formState.isSaving,
                onClick = { viewModel.save(onSuccess = onBack) }
            )
        }
    }
}
