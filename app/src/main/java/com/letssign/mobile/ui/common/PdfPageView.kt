package com.letssign.mobile.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.letssign.mobile.data.storage.PdfPageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renderiza uma página do PDF ocupando a largura disponível, mantendo a
 * proporção original da página, e expõe o tamanho renderizado (em Dp) via
 * [content] para que overlays (campos de assinatura) possam ser posicionados
 * com base nos mesmos ratios (0..1) salvos no banco de dados.
 */
@Composable
fun PdfPageView(
    filePath: String,
    page: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraints.(pageWidthDp: Dp, pageHeightDp: Dp) -> Unit = { _, _ -> }
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val targetWidthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)

        val rendered = produceState<PdfPageRenderer.RenderedPage?>(initialValue = null, filePath, page, targetWidthPx) {
            value = withContext(Dispatchers.IO) {
                try {
                    PdfPageRenderer(File(filePath)).use { it.renderPage(page, targetWidthPx) }
                } catch (e: Exception) {
                    null
                }
            }
        }.value

        if (rendered == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val pageHeightDp = with(density) { rendered.bitmap.height.toFloat().toDp() }
            Image(
                bitmap = rendered.bitmap.asImageBitmap(),
                contentDescription = "Página ${page + 1} do documento",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
            content(maxWidth, pageHeightDp)
        }
    }
}
