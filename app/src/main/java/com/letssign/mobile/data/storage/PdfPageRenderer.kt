package com.letssign.mobile.data.storage

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Utilitário para renderizar páginas de um PDF em Bitmap, usado tanto na
 * tela de posicionamento de assinaturas (Admin) quanto na tela de
 * assinatura (Signatário). Usa a API nativa android.graphics.pdf.PdfRenderer.
 */
class PdfPageRenderer(private val file: File) : AutoCloseable {

    private val fileDescriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer: PdfRenderer = PdfRenderer(fileDescriptor)

    val pageCount: Int get() = renderer.pageCount

    /** Retorna o bitmap da página e a razão largura/altura original (pt). */
    fun renderPage(index: Int, targetWidthPx: Int): RenderedPage {
        renderer.openPage(index).use { page ->
            val scale = targetWidthPx.toFloat() / page.width.toFloat()
            val targetHeightPx = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return RenderedPage(
                bitmap = bitmap,
                pageWidthPt = page.width,
                pageHeightPt = page.height
            )
        }
    }

    override fun close() {
        renderer.close()
        fileDescriptor.close()
    }

    data class RenderedPage(
        val bitmap: Bitmap,
        val pageWidthPt: Int,
        val pageHeightPt: Int
    )

    companion object {
        /** Validação rápida: tenta abrir o arquivo como PDF, usada no fluxo de upload. */
        fun isValidPdf(file: File): Boolean {
            return try {
                PdfPageRenderer(file).use { it.pageCount > 0 }
            } catch (e: Exception) {
                false
            }
        }
    }
}
