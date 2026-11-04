package com.letssign.mobile.ui.admin.newdocument

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letssign.mobile.ui.common.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositioningScreen(
    viewModel: NewDocumentViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(0) }
    var activeSignerId by remember(state.selectedSigners) {
        mutableStateOf(state.selectedSigners.firstOrNull()?.signer?.id)
    }

    val pdfPath = state.pdfPath

    Scaffold(topBar = { TopAppBar(title = { Text("Posicionar assinaturas (3/4)") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Text("Toque na página para adicionar um campo para o signatário selecionado.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.selectedSigners) { selected ->
                    FilterChip(
                        selected = activeSignerId == selected.signer.id,
                        onClick = { activeSignerId = selected.signer.id },
                        label = { Text(selected.signer.fullName.substringBefore(" ")) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Página anterior")
                }
                Text(
                    "Página ${currentPage + 1} de ${state.pageCount}",
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(
                    onClick = { if (currentPage < state.pageCount - 1) currentPage++ },
                    enabled = currentPage < state.pageCount - 1
                ) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Próxima página")
                }
            }

            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (pdfPath != null) {
                    com.letssign.mobile.ui.common.PdfPageView(
                        filePath = pdfPath,
                        page = currentPage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(currentPage, activeSignerId) {
                                detectTapGestures { offset ->
                                    val signerId = activeSignerId ?: return@detectTapGestures
                                    val xRatio = offset.x / size.width
                                    val yRatio = offset.y / size.height
                                    viewModel.addField(signerId, currentPage, xRatio, yRatio)
                                }
                            }
                    ) { pageWidthDp, pageHeightDp ->
                        state.fields.filter { it.page == currentPage }.forEach { field ->
                            FieldOverlay(
                                field = field,
                                signerInitial = state.selectedSigners.firstOrNull { it.signer.id == field.signerId }
                                    ?.signer?.fullName?.firstOrNull()?.toString() ?: "?",
                                pageWidthDp = pageWidthDp,
                                pageHeightDp = pageHeightDp,
                                onMove = { nx, ny -> viewModel.moveField(field.key, nx, ny) },
                                onDelete = { viewModel.deleteField(field.key) }
                            )
                        }
                    }
                }
            }

            state.errorMessage?.let {
                Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Campos posicionados: ${state.fields.size}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            PrimaryButton(text = "Continuar", isLoading = state.isSavingStep, onClick = { viewModel.confirmFields(onNext) })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
        }
    }
}

@Composable
private fun FieldOverlay(
    field: FieldDraft,
    signerInitial: String,
    pageWidthDp: Dp,
    pageHeightDp: Dp,
    onMove: (Float, Float) -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val widthDp = pageWidthDp * field.widthRatio
    val heightDp = pageHeightDp * field.heightRatio
    val offsetXDp = pageWidthDp * field.xRatio
    val offsetYDp = pageHeightDp * field.yRatio

    Box(
        modifier = Modifier
            .offset(x = offsetXDp, y = offsetYDp)
            .size(width = widthDp, height = heightDp)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .pointerInput(field.key) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val pageWidthPx = with(density) { pageWidthDp.toPx() }
                    val pageHeightPx = with(density) { pageHeightDp.toPx() }
                    val newXRatio = field.xRatio + dragAmount.x / pageWidthPx
                    val newYRatio = field.yRatio + dragAmount.y / pageHeightPx
                    onMove(newXRatio, newYRatio)
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Assinatura: $signerInitial",
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Remover campo", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
