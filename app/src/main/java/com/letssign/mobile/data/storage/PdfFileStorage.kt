package com.letssign.mobile.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Responsável por copiar o PDF escolhido pelo usuário para a pasta
 * privada do app (filesDir/documents), conforme a estratégia de
 * "Armazenamento em sistema de arquivos do device" exigida no desafio.
 */
class PdfFileStorage(private val context: Context) {

    private fun documentsDir(): File {
        val dir = File(context.filesDir, "documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Copia o conteúdo do Uri selecionado via SAF para o armazenamento interno.
     * Lança exceção se o arquivo não puder ser lido/for inválido.
     */
    fun importPdf(uri: Uri, suggestedName: String): ImportedFile {
        val resolver: ContentResolver = context.contentResolver
        val fileName = "${UUID.randomUUID()}.pdf"
        val destination = File(documentsDir(), fileName)

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Não foi possível abrir o arquivo selecionado.")

        if (destination.length() == 0L) {
            destination.delete()
            throw IllegalArgumentException("O arquivo selecionado está vazio ou corrompido.")
        }

        return ImportedFile(path = destination.absolutePath, originalName = suggestedName)
    }

    /** Cria uma cópia física do PDF, usada ao gerar uma nova versão do documento. */
    fun duplicate(sourcePath: String): String {
        val source = File(sourcePath)
        val destination = File(documentsDir(), "${UUID.randomUUID()}.pdf")
        source.copyTo(destination, overwrite = true)
        return destination.absolutePath
    }

    fun fileFor(path: String): File = File(path)

    data class ImportedFile(val path: String, val originalName: String)
}
