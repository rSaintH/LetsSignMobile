package com.letssign.mobile.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.letssign.mobile.di.AppContainer
import com.letssign.mobile.session.SessionUser
import com.letssign.mobile.ui.admin.documents.DocumentsListScreen
import com.letssign.mobile.ui.admin.signers.SignersListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    container: AppContainer,
    admin: SessionUser,
    onNewDocument: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onAddSigner: () -> Unit,
    onEditSigner: (Long) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) "Documentos" else "Signatários") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Description, contentDescription = null) },
                    label = { Text("Documentos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.People, contentDescription = null) },
                    label = { Text("Signatários") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> DocumentsListScreen(
                    container = container,
                    adminId = admin.id,
                    onNewDocument = onNewDocument,
                    onOpenDocument = onOpenDocument
                )
                else -> SignersListScreen(
                    container = container,
                    adminId = admin.id,
                    onAddSigner = onAddSigner,
                    onEditSigner = onEditSigner
                )
            }
        }
    }
}
