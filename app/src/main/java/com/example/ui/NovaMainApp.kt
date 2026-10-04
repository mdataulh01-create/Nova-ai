package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.ProjectInfo
import com.example.core.storage.StorageManager
import com.example.core.storage.StorageType
import com.example.ui.navigation.NavDestination
import com.example.ui.screens.AgentsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.GitScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageSetupDialog
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaVioletAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaMainApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageManager = remember { StorageManager.getInstance(context) }

    var currentDestination by remember { mutableStateOf(NavDestination.CHAT) }
    var activeProject by remember { mutableStateOf<ProjectInfo?>(null) }
    val openFiles = remember { mutableStateListOf<File>() }
    var activeFile by remember { mutableStateOf<File?>(null) }

    var showStorageSetupDialog by remember { mutableStateOf(!storageManager.isStorageInitialized()) }

    // SAF folder picker launcher
    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                storageManager.markStorageInitialized(StorageType.SAF_SHARED, uri)
                showStorageSetupDialog = false
                Toast.makeText(context, "NovaAI directory linked via SAF!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Fallback to app private
                storageManager.markStorageInitialized(StorageType.APP_PRIVATE)
                showStorageSetupDialog = false
                Toast.makeText(context, "Using App-Private sandbox", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Initialize starter project & load active project
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            storageManager.getRootDirectory()
            storageManager.initializeStarterProjectsIfNeeded()
            val list = storageManager.listProjects()
            if (list.isNotEmpty() && activeProject == null) {
                activeProject = list.first()
                // Open first file (e.g. Main.kt or README.md)
                val firstFile = File(list.first().rootPath).walkTopDown()
                    .firstOrNull { it.isFile && (it.name == "Main.kt" || it.name == "README.md" || it.name == "main.py") }
                if (firstFile != null) {
                    openFiles.add(firstFile)
                    activeFile = firstFile
                }
            }
        }
    }

    // Back handler: return to CHAT if on other screen
    BackHandler(enabled = currentDestination != NavDestination.CHAT) {
        currentDestination = NavDestination.CHAT
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // App Brand Logo
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NovaCyanPrimary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "N",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF00363F),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Nova AI",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "IDE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NovaCyanPrimary,
                                    modifier = Modifier
                                        .background(NovaCyanPrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = "Mobile Native Engineering Agent",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Active project badge
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { currentDestination = NavDestination.PROJECTS }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (activeProject != null) NovaCyanPrimary else Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeProject?.name ?: "No Project",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Switch project",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            // M3 Bottom Navigation Bar with explicit navigationBars windowInsets
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets.navigationBars,
                tonalElevation = 4.dp
            ) {
                NavDestination.values().forEach { dest ->
                    val isSelected = currentDestination == dest
                    val showEditorBadge = dest == NavDestination.EDITOR && openFiles.isNotEmpty()

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = dest },
                        icon = {
                            if (showEditorBadge) {
                                BadgedBox(badge = { Badge { Text("${openFiles.size}") } }) {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = dest.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00363F),
                            indicatorColor = NovaCyanPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = NovaCyanPrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(dest.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                NavDestination.CHAT -> {
                    ChatScreen(
                        activeProject = activeProject,
                        storageManager = storageManager,
                        onNavigateToSettings = { currentDestination = NavDestination.SETTINGS }
                    )
                }

                NavDestination.PROJECTS -> {
                    ProjectsScreen(
                        storageManager = storageManager,
                        activeProject = activeProject,
                        onSelectProject = { proj ->
                            activeProject = proj
                        },
                        onOpenFileInEditor = { file ->
                            if (!openFiles.contains(file)) {
                                openFiles.add(file)
                            }
                            activeFile = file
                            currentDestination = NavDestination.EDITOR
                        }
                    )
                }

                NavDestination.EDITOR -> {
                    EditorScreen(
                        storageManager = storageManager,
                        openFiles = openFiles,
                        activeFile = activeFile,
                        onSelectFile = { f -> activeFile = f },
                        onCloseFile = { f ->
                            openFiles.remove(f)
                            if (activeFile == f) {
                                activeFile = openFiles.firstOrNull()
                            }
                        },
                        onSendToAgent = { file, content ->
                            currentDestination = NavDestination.AGENTS
                        }
                    )
                }

                NavDestination.TERMINAL -> {
                    TerminalScreen(
                        storageManager = storageManager,
                        activeProject = activeProject
                    )
                }

                NavDestination.AGENTS -> {
                    AgentsScreen(
                        storageManager = storageManager,
                        activeProject = activeProject,
                        onOpenFile = { file ->
                            if (!openFiles.contains(file)) {
                                openFiles.add(file)
                            }
                            activeFile = file
                            currentDestination = NavDestination.EDITOR
                        }
                    )
                }

                NavDestination.GIT -> {
                    GitScreen(
                        storageManager = storageManager,
                        activeProject = activeProject
                    )
                }

                NavDestination.SETTINGS -> {
                    SettingsScreen(
                        storageManager = storageManager,
                        onTriggerSafPicker = {
                            safPickerLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }

    // First Launch Storage Setup Dialog
    if (showStorageSetupDialog) {
        StorageSetupDialog(
            onDismiss = {
                storageManager.markStorageInitialized(StorageType.APP_PRIVATE)
                showStorageSetupDialog = false
            },
            onSelectSaf = {
                safPickerLauncher.launch(null)
            },
            onSelectAppPrivate = {
                storageManager.markStorageInitialized(StorageType.APP_PRIVATE)
                showStorageSetupDialog = false
                Toast.makeText(context, "App-Private sandbox initialized at NovaAI/", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
