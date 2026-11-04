package com.letssign.mobile.di

import android.content.Context
import com.letssign.mobile.data.local.AppDatabase
import com.letssign.mobile.data.repository.DocumentRepository
import com.letssign.mobile.data.repository.DocumentRepositoryImpl
import com.letssign.mobile.data.repository.UserRepository
import com.letssign.mobile.data.repository.UserRepositoryImpl
import com.letssign.mobile.data.storage.PdfFileStorage
import com.letssign.mobile.session.SessionManager

/**
 * Container simples de injeção de dependências (sem frameworks), expondo
 * repositórios como interfaces. Isso permite, na Sprint 2, substituir as
 * implementações Room por implementações baseadas em Retrofit/API REST
 * sem alterar nenhuma tela/ViewModel.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val pdfFileStorage = PdfFileStorage(context)

    val sessionManager = SessionManager(context)

    val userRepository: UserRepository = UserRepositoryImpl(database.userDao())

    val documentRepository: DocumentRepository = DocumentRepositoryImpl(
        documentDao = database.documentDao(),
        documentSignerDao = database.documentSignerDao(),
        signatureFieldDao = database.signatureFieldDao(),
        documentEventDao = database.documentEventDao(),
        pdfFileStorage = pdfFileStorage
    )
}
