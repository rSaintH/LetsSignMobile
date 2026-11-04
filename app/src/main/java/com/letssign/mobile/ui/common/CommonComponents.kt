package com.letssign.mobile.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letssign.mobile.data.local.entity.DocumentStatus
import com.letssign.mobile.data.local.entity.SignatureStatus

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).then(Modifier), strokeWidth = 2.dp)
        }
        Text(text)
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            Text(message, color = MaterialTheme.colorScheme.outline)
        }
    }
}

fun documentStatusLabel(status: DocumentStatus): String = when (status) {
    DocumentStatus.DRAFT -> "Rascunho"
    DocumentStatus.SENDING_FAILED -> "Falha no envio"
    DocumentStatus.SENT -> "Enviado"
    DocumentStatus.PARTIALLY_SIGNED -> "Parcialmente assinado"
    DocumentStatus.COMPLETED -> "Concluído"
}

fun documentStatusColor(status: DocumentStatus): Color = when (status) {
    DocumentStatus.DRAFT -> Color(0xFF757575)
    DocumentStatus.SENDING_FAILED -> Color(0xFFBA1A1A)
    DocumentStatus.SENT -> Color(0xFF1565C0)
    DocumentStatus.PARTIALLY_SIGNED -> Color(0xFFE6A700)
    DocumentStatus.COMPLETED -> Color(0xFF2E7D32)
}

fun signatureStatusLabel(status: SignatureStatus): String = when (status) {
    SignatureStatus.PENDING -> "Pendente"
    SignatureStatus.SIGNED -> "Assinado"
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
