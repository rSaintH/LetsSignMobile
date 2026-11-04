package com.letssign.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.letssign.mobile.data.local.entity.UserRole
import com.letssign.mobile.di.AppContainer
import com.letssign.mobile.ui.admin.AdminHomeScreen
import com.letssign.mobile.ui.admin.documents.DocumentDetailScreen
import com.letssign.mobile.ui.admin.newdocument.NewDocumentViewModel
import com.letssign.mobile.ui.admin.newdocument.PositioningScreen
import com.letssign.mobile.ui.admin.newdocument.ReviewSendScreen
import com.letssign.mobile.ui.admin.newdocument.SignerSelectStep
import com.letssign.mobile.ui.admin.newdocument.TitlePdfStep
import com.letssign.mobile.ui.admin.signers.SignerFormScreen
import com.letssign.mobile.ui.auth.LoginScreen
import com.letssign.mobile.ui.signer.SignDocumentScreen
import com.letssign.mobile.ui.signer.SignerHomeScreen

@Composable
fun LetsSignNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val session by container.sessionManager.currentUser.collectAsState()

    val startDestination = remember(session) {
        when (session?.role) {
            UserRole.ADMIN -> Routes.ADMIN_HOME
            UserRole.SIGNER -> Routes.SIGNER_HOME
            null -> Routes.LOGIN
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                container = container,
                onLoggedIn = { role ->
                    val destination = if (role == UserRole.ADMIN) Routes.ADMIN_HOME else Routes.SIGNER_HOME
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ADMIN_HOME) {
            val admin = session
            if (admin != null) {
                AdminHomeScreen(
                    container = container,
                    admin = admin,
                    onNewDocument = { navController.navigate(Routes.DOCUMENT_WIZARD_GRAPH) },
                    onOpenDocument = { id -> navController.navigate(Routes.documentDetail(id)) },
                    onAddSigner = { navController.navigate(Routes.signerForm(null)) },
                    onEditSigner = { id -> navController.navigate(Routes.signerForm(id)) },
                    onLogout = {
                        container.sessionManager.logout()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    }
                )
            }
        }

        composable(
            route = Routes.SIGNER_FORM,
            arguments = listOf(navArgument("signerId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val signerId = backStackEntry.arguments?.getLong("signerId")?.takeIf { it != -1L }
            val admin = session
            if (admin != null) {
                SignerFormScreen(
                    container = container,
                    adminId = admin.id,
                    signerId = signerId,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Routes.DOCUMENT_DETAIL,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: return@composable
            DocumentDetailScreen(
                container = container,
                documentId = documentId,
                onBack = { navController.popBackStack() },
                onNewVersionCreated = {
                    navController.popBackStack(Routes.ADMIN_HOME, inclusive = false)
                }
            )
        }

        composable(
            route = Routes.SIGN_DOCUMENT,
            arguments = listOf(navArgument("documentSignerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val documentSignerId = backStackEntry.arguments?.getLong("documentSignerId") ?: return@composable
            SignDocumentScreen(
                container = container,
                documentSignerId = documentSignerId,
                onFinished = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SIGNER_HOME) {
            val signer = session
            if (signer != null) {
                SignerHomeScreen(
                    container = container,
                    signer = signer,
                    onOpenDocument = { documentSignerId -> navController.navigate(Routes.signDocument(documentSignerId)) },
                    onLogout = {
                        container.sessionManager.logout()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    }
                )
            }
        }

        navigation(startDestination = Routes.WIZARD_TITLE, route = Routes.DOCUMENT_WIZARD_GRAPH) {
            composable(Routes.WIZARD_TITLE) { backStackEntry ->
                val admin = session ?: return@composable
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.DOCUMENT_WIZARD_GRAPH) }
                val wizardViewModel: NewDocumentViewModel = viewModel(
                    parentEntry,
                    factory = viewModelFactory {
                        initializer { NewDocumentViewModel(container.documentRepository, container.userRepository, admin.id) }
                    }
                )
                TitlePdfStep(
                    viewModel = wizardViewModel,
                    onCancel = { navController.popBackStack(Routes.ADMIN_HOME, inclusive = false) },
                    onNext = { navController.navigate(Routes.WIZARD_SIGNERS) }
                )
            }
            composable(Routes.WIZARD_SIGNERS) { backStackEntry ->
                val admin = session ?: return@composable
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.DOCUMENT_WIZARD_GRAPH) }
                val wizardViewModel: NewDocumentViewModel = viewModel(
                    parentEntry,
                    factory = viewModelFactory {
                        initializer { NewDocumentViewModel(container.documentRepository, container.userRepository, admin.id) }
                    }
                )
                SignerSelectStep(
                    viewModel = wizardViewModel,
                    onBack = { navController.popBackStack() },
                    onNext = { navController.navigate(Routes.WIZARD_POSITIONING) }
                )
            }
            composable(Routes.WIZARD_POSITIONING) { backStackEntry ->
                val admin = session ?: return@composable
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.DOCUMENT_WIZARD_GRAPH) }
                val wizardViewModel: NewDocumentViewModel = viewModel(
                    parentEntry,
                    factory = viewModelFactory {
                        initializer { NewDocumentViewModel(container.documentRepository, container.userRepository, admin.id) }
                    }
                )
                PositioningScreen(
                    viewModel = wizardViewModel,
                    onBack = { navController.popBackStack() },
                    onNext = { navController.navigate(Routes.WIZARD_REVIEW) }
                )
            }
            composable(Routes.WIZARD_REVIEW) { backStackEntry ->
                val admin = session ?: return@composable
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.DOCUMENT_WIZARD_GRAPH) }
                val wizardViewModel: NewDocumentViewModel = viewModel(
                    parentEntry,
                    factory = viewModelFactory {
                        initializer { NewDocumentViewModel(container.documentRepository, container.userRepository, admin.id) }
                    }
                )
                ReviewSendScreen(
                    viewModel = wizardViewModel,
                    onBack = { navController.popBackStack() },
                    onFinished = {
                        navController.popBackStack(Routes.ADMIN_HOME, inclusive = false)
                    }
                )
            }
        }
    }
}
