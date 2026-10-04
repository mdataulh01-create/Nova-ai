package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.core.storage.FileDetails
import com.example.core.storage.FileNode
import com.example.core.storage.FileSearchResult
import com.example.core.storage.FileSortOrder
import com.example.core.storage.ProjectInfo
import com.example.core.storage.StorageManager
import com.example.core.storage.TemplateManager
import com.example.ui.CreateProjectDialog
import com.example.ui.formatBytes
import com.example.ui.getFileIconColor
import com.example.ui.theme.NovaCodeBackground
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaRose
import com.example.ui.theme.NovaVioletAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    storageManager: StorageManager,
    activeProject: ProjectInfo?,
    onSelectProject: (ProjectInfo) -> Unit,
    onOpenFileInEditor: (File) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var projects by remember { mutableStateOf<List<ProjectInfo>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(if (activeProject != null) 1 else 0) } // 0: Projects, 1: Active Explorer
    var showCreateDialog by remember { mutableStateOf(false) }
    var showBackupsDialog by remember { mutableStateOf(false) }

    // Explorer State
    var fileTreeRoot by remember { mutableStateOf<FileNode?>(null) }
    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }
    var sortOrder by remember { mutableStateOf(FileSortOrder.NAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showHiddenFiles by remember { mutableStateOf(false) }

    // Search in project
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FileSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Dialogs for file operations
    var targetFolderForCreation by remember { mutableStateOf<File?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var fileDetailsTarget by remember { mutableStateOf<FileDetails?>(null) }

    // Project action targets
    var renameProjectTarget by remember { mutableStateOf<ProjectInfo?>(null) }

    fun refreshProjects() {
        coroutineScope.launch {
            val list = withContext(Dispatchers.IO) {
                storageManager.listProjects()
            }
            projects = list
            if (activeProject != null) {
                val projDir = File(activeProject.rootPath)
                if (projDir.exists()) {
                    val root = withContext(Dispatchers.IO) {
                        storageManager.getFileTree(projDir, sortOrder, showHiddenFiles)
                    }
                    fileTreeRoot = root
                }
            }
        }
    }

    fun executeSearch(q: String) {
        if (activeProject == null || q.isBlank()) {
            searchResults = emptyList()
            return
        }
        isSearching = true
        coroutineScope.launch {
            val res = withContext(Dispatchers.IO) {
                storageManager.searchProjectFiles(File(activeProject.rootPath), q, matchContent = true)
            }
            searchResults = res
            isSearching = false
        }
    }

    LaunchedEffect(activeProject, sortOrder, showHiddenFiles) {
        refreshProjects()
        if (activeProject != null) {
            selectedTab = 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("screen_projects")
    ) {
        // Tab Row: Projects List vs Active File Explorer
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = NovaCyanPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Workspace (${projects.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        if (activeProject != null) "Explorer: ${activeProject.name}" else "Explorer",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }

        if (selectedTab == 0) {
            // Projects Workspace
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Toolbar for workspace
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROJECTS IN NovaAI/Projects/",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NovaCyanPrimary
                            )

                            Row {
                                IconButton(
                                    onClick = { showBackupsDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Backup,
                                        contentDescription = "Backups",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { refreshProjects() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Refresh",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (projects.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                    tint = NovaCyanPrimary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No projects found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tap '+' to create your first project from a template",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(projects, key = { it.rootPath }) { proj ->
                                val isActive = activeProject?.rootPath == proj.rootPath
                                var showProjectMenu by remember { mutableStateOf(false) }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isActive) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectProject(proj)
                                            selectedTab = 1
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    if (isActive) NovaCyanPrimary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Folder,
                                                contentDescription = null,
                                                tint = if (isActive) Color(0xFF00363F) else NovaCyanPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = proj.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                if (isActive) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "ACTIVE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = NovaCyanPrimary,
                                                        modifier = Modifier
                                                            .background(NovaCyanPrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = "${proj.fileCount} files • ${formatBytes(proj.totalSizeBytes)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(proj.lastModified))
                                            Text(
                                                text = "Modified $dateStr",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }

                                        // Menu button for project actions
                                        Box {
                                            IconButton(
                                                onClick = { showProjectMenu = true },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.MoreVert,
                                                    contentDescription = "Project options",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showProjectMenu,
                                                onDismissRequest = { showProjectMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Open in Explorer") },
                                                    leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        showProjectMenu = false
                                                        onSelectProject(proj)
                                                        selectedTab = 1
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Create Backup (.zip)") },
                                                    leadingIcon = { Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        showProjectMenu = false
                                                        coroutineScope.launch {
                                                            val b = withContext(Dispatchers.IO) {
                                                                storageManager.createProjectBackup(proj.name)
                                                            }
                                                            Toast.makeText(context, "Backup created: ${b.name}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Duplicate Project") },
                                                    leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        showProjectMenu = false
                                                        coroutineScope.launch {
                                                            withContext(Dispatchers.IO) {
                                                                storageManager.duplicateProject(proj.name)
                                                            }
                                                            refreshProjects()
                                                            Toast.makeText(context, "Duplicated project", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Rename Project") },
                                                    leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        showProjectMenu = false
                                                        renameProjectTarget = proj
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Export to ZIP") },
                                                    leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        showProjectMenu = false
                                                        coroutineScope.launch {
                                                            val zip = withContext(Dispatchers.IO) {
                                                                storageManager.exportProjectToZip(proj.name)
                                                            }
                                                            Toast.makeText(context, "Exported: ${zip.name}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete Project", color = NovaRose) },
                                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = NovaRose, modifier = Modifier.size(16.dp)) },
                                                    onClick = {
                                                        showProjectMenu = false
                                                        deleteTarget = File(proj.rootPath)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // FAB to create new project
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .testTag("fab_create_project"),
                    containerColor = NovaCyanPrimary,
                    contentColor = Color(0xFF00363F)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Create Project")
                }
            }
        } else {
            // Active Project File Explorer Tree
            Column(modifier = Modifier.fillMaxSize()) {
                if (activeProject == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Please select or create a project from the Workspace tab.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    // Explorer Toolbar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ROOT: ${activeProject.name}/",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NovaCyanPrimary
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Search toggle
                                    IconButton(
                                        onClick = {
                                            isSearchActive = !isSearchActive
                                            if (!isSearchActive) {
                                                searchQuery = ""
                                                searchResults = emptyList()
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = "Search Files",
                                            tint = if (isSearchActive) NovaCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Sort dropdown
                                    Box {
                                        IconButton(
                                            onClick = { showSortMenu = true },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                                contentDescription = "Sort Files",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false }
                                        ) {
                                            FileSortOrder.values().forEach { order ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = order.displayName,
                                                            fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (order == sortOrder) NovaCyanPrimary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        sortOrder = order
                                                        showSortMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Hidden files toggle
                                    IconButton(
                                        onClick = { showHiddenFiles = !showHiddenFiles },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showHiddenFiles) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = "Toggle hidden files",
                                            tint = if (showHiddenFiles) NovaCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // New file in root
                                    IconButton(
                                        onClick = {
                                            targetFolderForCreation = File(activeProject.rootPath)
                                            showNewFileDialog = true
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                            contentDescription = "New File",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // New folder in root
                                    IconButton(
                                        onClick = {
                                            targetFolderForCreation = File(activeProject.rootPath)
                                            showNewFolderDialog = true
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CreateNewFolder,
                                            contentDescription = "New Folder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Refresh
                                    IconButton(
                                        onClick = { refreshProjects() },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Refresh",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Search bar in project
                            AnimatedVisibility(visible = isSearchActive) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = {
                                            searchQuery = it
                                            executeSearch(it)
                                        },
                                        placeholder = { Text("Search file names or code content...", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        singleLine = true,
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                                    Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isSearchActive && searchQuery.isNotBlank()) {
                        // Display Search Results View
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Text(
                                text = "SEARCH RESULTS (${searchResults.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NovaCyanPrimary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            if (isSearching) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Searching files...", fontSize = 12.sp)
                                }
                            } else if (searchResults.isEmpty()) {
                                Text(
                                    text = "No matching files or code lines found for '$searchQuery'.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(searchResults) { result ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onOpenFileInEditor(result.file) }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Description,
                                                        contentDescription = null,
                                                        tint = getFileIconColor(result.file.extension),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = result.relativePath,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = NovaCyanPrimary
                                                    )
                                                    if (result.lineNumber != null) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = ":${result.lineNumber}",
                                                            fontSize = 11.sp,
                                                            color = NovaVioletAccent,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }

                                                if (result.matchedContent != null) {
                                                    Text(
                                                        text = result.matchedContent,
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Tree View
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        ) {
                            fileTreeRoot?.children?.let { nodes ->
                                items(nodes) { node ->
                                    EnhancedFileNodeRow(
                                        node = node,
                                        level = 0,
                                        expandedMap = expandedFolders,
                                        onToggleFolder = { path ->
                                            expandedFolders[path] = !(expandedFolders[path] ?: false)
                                        },
                                        onOpenFile = onOpenFileInEditor,
                                        onNewFileInFolder = { folder ->
                                            targetFolderForCreation = folder
                                            showNewFileDialog = true
                                        },
                                        onNewFolderInFolder = { folder ->
                                            targetFolderForCreation = folder
                                            showNewFolderDialog = true
                                        },
                                        onRename = { file -> renameTarget = file },
                                        onDelete = { file -> deleteTarget = file },
                                        onDuplicate = { file ->
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    storageManager.copyFileOrFolder(file, file.parentFile ?: file)
                                                }
                                                refreshProjects()
                                                Toast.makeText(context, "Duplicated ${file.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onShowDetails = { file ->
                                            val details = storageManager.getFileDetails(file, File(activeProject.rootPath))
                                            fileDetailsTarget = details
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Project Dialog
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, template ->
                showCreateDialog = false
                coroutineScope.launch {
                    try {
                        val dir = withContext(Dispatchers.IO) {
                            storageManager.createProject(name, template)
                        }
                        refreshProjects()
                        val newProj = ProjectInfo(
                            name = dir.name,
                            rootPath = dir.absolutePath,
                            fileCount = 1,
                            totalSizeBytes = 0L,
                            lastModified = System.currentTimeMillis()
                        )
                        onSelectProject(newProj)
                        selectedTab = 1
                        Toast.makeText(context, "Created project ${dir.name}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Failed to create", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Rename Project Dialog
    renameProjectTarget?.let { proj ->
        var newProjName by remember { mutableStateOf(proj.name) }
        AlertDialog(
            onDismissRequest = { renameProjectTarget = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = newProjName,
                    onValueChange = { newProjName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newProjName.trim()
                        if (trimmed.isNotBlank() && trimmed != proj.name) {
                            renameProjectTarget = null
                            coroutineScope.launch {
                                try {
                                    val ren = withContext(Dispatchers.IO) {
                                        storageManager.renameProject(proj.name, trimmed)
                                    }
                                    refreshProjects()
                                    if (activeProject?.rootPath == proj.rootPath) {
                                        onSelectProject(
                                            ProjectInfo(ren.name, ren.absolutePath, proj.fileCount, proj.totalSizeBytes, ren.lastModified())
                                        )
                                    }
                                    Toast.makeText(context, "Renamed to ${ren.name}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameProjectTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Backups List & Restore Dialog
    if (showBackupsDialog) {
        val backups = remember { storageManager.listBackups() }
        AlertDialog(
            onDismissRequest = { showBackupsDialog = false },
            title = { Text("Project Backups in NovaAI/Backups/") },
            text = {
                if (backups.isEmpty()) {
                    Text(
                        "No backups found. You can create a backup from any project's options menu.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(backups) { bFile ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bFile.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(formatBytes(bFile.length()), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(
                                        onClick = {
                                            showBackupsDialog = false
                                            coroutineScope.launch {
                                                try {
                                                    val restored = withContext(Dispatchers.IO) {
                                                        storageManager.restoreProjectFromBackup(bFile)
                                                    }
                                                    refreshProjects()
                                                    Toast.makeText(context, "Restored: ${restored.name}", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBackupsDialog = false }) { Text("Close") }
            }
        )
    }

    // New File Dialog
    if (showNewFileDialog) {
        val parent = targetFolderForCreation ?: activeProject?.let { File(it.rootPath) }
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create File in ${parent?.name ?: "Project"}") },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name (e.g. Model.kt, script.py)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = fileName.trim()
                        if (trimmed.isNotBlank() && parent != null) {
                            showNewFileDialog = false
                            coroutineScope.launch {
                                try {
                                    val f = withContext(Dispatchers.IO) {
                                        storageManager.createFile(parent, trimmed, "")
                                    }
                                    refreshProjects()
                                    onOpenFileInEditor(f)
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        val parent = targetFolderForCreation ?: activeProject?.let { File(it.rootPath) }
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create Folder in ${parent?.name ?: "Project"}") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name (e.g. utils, network)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = folderName.trim()
                        if (trimmed.isNotBlank() && parent != null) {
                            showNewFolderDialog = false
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        storageManager.createFolder(parent, trimmed)
                                    }
                                    refreshProjects()
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Item Dialog
    renameTarget?.let { target ->
        var newName by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename ${if (target.isDirectory) "Folder" else "File"}") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isNotBlank() && trimmed != target.name) {
                            renameTarget = null
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        storageManager.renameFileOrFolder(target, trimmed)
                                    }
                                    refreshProjects()
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = NovaRose) },
            title = { Text("Delete ${if (target.isDirectory) "Folder" else "File"}") },
            text = {
                Text("Are you sure you want to permanently delete '${target.name}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTarget = null
                        coroutineScope.launch {
                            val deleted = withContext(Dispatchers.IO) {
                                storageManager.deleteFileOrFolder(target)
                            }
                            if (deleted) {
                                refreshProjects()
                                Toast.makeText(context, "Deleted ${target.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // File Details Dialog
    fileDetailsTarget?.let { details ->
        AlertDialog(
            onDismissRequest = { fileDetailsTarget = null },
            icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = NovaCyanPrimary) },
            title = { Text("File Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${details.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Relative Path: ${details.relativePath}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("Size: ${formatBytes(details.sizeBytes)} (${details.sizeBytes} bytes)", fontSize = 11.sp)
                    if (!details.isDirectory) {
                        Text("Lines of Code: ${details.lineCount}", fontSize = 11.sp)
                    }
                    Text("MIME Type: ${details.mimeType}", fontSize = 11.sp)
                    Text("Permissions: " + (if (details.canRead) "r" else "-") + (if (details.canWrite) "w" else "-") + (if (details.canExecute) "x" else "-"), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    val mod = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(details.lastModified))
                    Text("Last Modified: $mod", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { fileDetailsTarget = null }) { Text("OK") }
            }
        )
    }
}

@Composable
fun EnhancedFileNodeRow(
    node: FileNode,
    level: Int,
    expandedMap: Map<String, Boolean>,
    onToggleFolder: (String) -> Unit,
    onOpenFile: (File) -> Unit,
    onNewFileInFolder: (File) -> Unit,
    onNewFolderInFolder: (File) -> Unit,
    onRename: (File) -> Unit,
    onDelete: (File) -> Unit,
    onDuplicate: (File) -> Unit,
    onShowDetails: (File) -> Unit
) {
    val isExpanded = expandedMap[node.path] ?: false
    var showNodeMenu by remember { mutableStateOf(false) }
    val nodeFile = File(node.path)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDirectory) {
                        onToggleFolder(node.path)
                    } else {
                        onOpenFile(nodeFile)
                    }
                }
                .padding(vertical = 3.dp, horizontal = (level * 14 + 6).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                    contentDescription = null,
                    tint = NovaCyanPrimary,
                    modifier = Modifier.size(17.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(19.dp))
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = getFileIconColor(node.extension),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = node.name,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                color = if (node.isDirectory) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            if (!node.isDirectory) {
                Text(
                    text = formatBytes(node.sizeBytes),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // 3-dots Context Menu Button
            Box {
                IconButton(
                    onClick = { showNodeMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "File actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showNodeMenu,
                    onDismissRequest = { showNodeMenu = false }
                ) {
                    if (!node.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("Open in Editor") },
                            leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            onClick = {
                                showNodeMenu = false
                                onOpenFile(nodeFile)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("New File Here") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            onClick = {
                                showNodeMenu = false
                                onNewFileInFolder(nodeFile)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Folder Here") },
                            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            onClick = {
                                showNodeMenu = false
                                onNewFolderInFolder(nodeFile)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Details & Stats") },
                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        onClick = {
                            showNodeMenu = false
                            onShowDetails(nodeFile)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        onClick = {
                            showNodeMenu = false
                            onDuplicate(nodeFile)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        onClick = {
                            showNodeMenu = false
                            onRename(nodeFile)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = NovaRose) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = NovaRose, modifier = Modifier.size(14.dp)) },
                        onClick = {
                            showNodeMenu = false
                            onDelete(nodeFile)
                        }
                    )
                }
            }
        }

        if (node.isDirectory && isExpanded) {
            node.children.forEach { child ->
                EnhancedFileNodeRow(
                    node = child,
                    level = level + 1,
                    expandedMap = expandedMap,
                    onToggleFolder = onToggleFolder,
                    onOpenFile = onOpenFile,
                    onNewFileInFolder = onNewFileInFolder,
                    onNewFolderInFolder = onNewFolderInFolder,
                    onRename = onRename,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onShowDetails = onShowDetails
                )
            }
        }
    }
}
