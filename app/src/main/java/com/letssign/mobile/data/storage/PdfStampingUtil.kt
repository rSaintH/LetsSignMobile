package com.letssign.mobile.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.File

/**
 * Usa o PDFBox-Android para "carimbar" a imagem da assinatura manuscrita
 * dentro do PDF, na página/posição/dimensão definidas pelo campo
 * (SignatureFieldEntity), convertendo os ratios salvos de volta para
 * coordenadas absolutas da página de destino.
 *
 * Importante: é necessário chamar PDFBoxResourceLoader.init(context) uma
 * vez (feito em LetsSignApp) antes de usar esta classe.
 */
object PdfStampingUtil {

    data class StampRequest(
        val page: Int,
        val xRatio: Float,
        val yRatio: Float,
        val widthRatio: Float,
        val heightRatio: Float,
        val signatureImagePath: String
    )

    fun stamp(sourcePdfPath: String, outputPdfPath: String, requests: List<StampRequest>) {
        val tempFile = File(File(outputPdfPath).parentFile, "${File(outputPdfPath).name}.tmp")
        PDDocument.load(File(sourcePdfPath)).use { document ->
            for (request in requests) {
                if (request.page < 0 || request.page >= document.numberOfPages) continue
                val page: PDPage = document.getPage(request.page)
                val mediaBox: PDRectangle = page.mediaBox

                val bitmap: Bitmap = BitmapFactory.decodeFile(request.signatureImagePath)
                    ?: continue
                val pdImage = LosslessFactory.createFromImage(document, bitmap)

                // Ratios (origem topo-esquerda) -> coordenadas PDF (origem inferior-esquerda).
                val xPt = request.xRatio * mediaBox.width
                val widthPt = request.widthRatio * mediaBox.width
                val heightPt = request.heightRatio * mediaBox.height
                val yTopPt = request.yRatio * mediaBox.height
                val yPt = mediaBox.height - yTopPt - heightPt

                PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { contentStream ->
                    contentStream.drawImage(pdImage, xPt, yPt, widthPt, heightPt)
                }
            }
            document.save(tempFile)
        }
        // Evita escrever no mesmo arquivo que ainda está sendo lido pelo PDDocument:
        // salva em um arquivo temporário e só então substitui o arquivo de trabalho.
        val destination = File(outputPdfPath)
        if (destination.exists()) destination.delete()
        tempFile.copyTo(destination, overwrite = true)
        tempFile.delete()
    }
}
