package com.letssign.mobile.ui.signer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.letssign.mobile.data.local.entity.SignatureFieldEntity
import com.letssign.mobile.di.AppContainer
import com.letssign.mobile.ui.common.PdfPageView
import com.letssign.mobile.ui.common.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignDocumentScreen(
    container: AppContainer,
    documentSignerId: Long,
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: SignDocumentViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SignDocumentViewModel(container.documentRepository, documentSignerId) }
        }
    )
    val state by viewModel.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(0) }
    var fieldBeingSigned by remember { mutableStateOf<SignatureFieldEntity?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(state.completed) {
        if (state.completed) onFinished()
    }

    LaunchedEffect(state.fields) {
        if (state.fields.isNotEmpty()) currentPage = state.fields.minOf { it.page }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Assinar documento") }) }) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val document = state.document
        val pdfPath = document?.filePath

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Text(document?.title ?: "", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Página anterior")
                }
                Text(
                    "Página ${currentPage + 1} de ${document?.pageCount ?: 0}",
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(
                    onClick = { if (currentPage < (document?.pageCount ?: 1) - 1) currentPage++ },
                    enabled = currentPage < (document?.pageCount ?: 1) - 1
                ) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Próxima página")
                }
            }

            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (pdfPath != null) {
                    PdfPageView(
                        filePath = pdfPath,
                        page = currentPage,
                        modifier = Modifier.fillMaxWidth()
                    ) { pageWidthDp, pageHeightDp ->
                        state.fields.filter { it.page == currentPage }.forEach { field ->
                            val isSigned = state.signedImagesByFieldId.containsKey(field.id)
                            val widthDp = pageWidthDp * field.widthRatio
                            val heightDp = pageHeightDp * field.heightRatio
                            val offsetXDp = pageWidthDp * field.xRatio
                            val offsetYDp = pageHeightDp * field.yRatio
                            Box(
                                modifier = Modifier
                                    .offset(x = offsetXDp, y = offsetYDp)
                                    .size(width = widthDp, height = heightDp)
                                    .border(
                                        2.dp,
                                        if (isSigned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .background(
                                        (if (isSigned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .then(
                                        if (!isSigned) Modifier.clickableNoRipple { fieldBeingSigned = field } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSigned) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Assinado", tint = Color(0xFF2E7D32))
                                } else {
                                    Text("Toque para assinar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let {
                Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Campos assinados: ${state.signedImagesByFieldId.size} de ${state.fields.size}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            PrimaryButton(
                text = "Concluir assinatura",
                isLoading = state.isSubmitting,
                enabled = viewModel.allFieldsSigned(),
                onClick = { viewModel.submit() }
            )
        }
    }

    fieldBeingSigned?.let { field ->
        SignaturePadDialog(
            fieldLabel = "Página ${field.page + 1}",
            cacheDir = context.cacheDir,
            onDismiss = { fieldBeingSigned = null },
            onConfirm = { path ->
                viewModel.onFieldSigned(field.id, path)
                fieldBeingSigned = null
            }
        )
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    androidx.compose.foundation.clickable(
        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
)
