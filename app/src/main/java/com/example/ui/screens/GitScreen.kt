package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.core.git.GitCommit
import com.example.core.git.GitFileStatus
import com.example.core.git.GitManager
import com.example.core.storage.ProjectInfo
import com.example.core.storage.StorageManager
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaVioletAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GitScreen(
    storageManager: StorageManager,
    activeProject: ProjectInfo?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: Changes/Staging, 1: Commit History
    var currentBranch by remember { mutableStateOf("main") }
    var branches by remember { mutableStateOf(listOf("main")) }
    var showBranchMenu by remember { mutableStateOf(false) }
    var showNewBranchDialog by remember { mutableStateOf(false) }

    var fileStatuses by remember { mutableStateOf<List<GitFileStatus>>(emptyList()) }
    var commitHistory by remember { mutableStateOf<List<GitCommit>>(emptyList()) }
    var commitMessage by remember { mutableStateOf("") }

    val gitManager = remember(activeProject) {
        val dir = activeProject?.let { File(it.rootPath) } ?: storageManager.getProjectsDirectory()
        GitManager(dir)
    }

    fun refreshGit() {
        coroutineScope.launch {
            val b = withContext(Dispatchers.IO) { gitManager.getCurrentBranch() }
            val bList = withContext(Dispatchers.IO) { gitManager.getBranchList() }
            val files = withContext(Dispatchers.IO) { gitManager.getFileStatuses() }
            val commits = withContext(Dispatchers.IO) { gitManager.getCommitHistory() }

            currentBranch = b
            branches = bList
            fileStatuses = files
            commitHistory = commits
        }
    }

    LaunchedEffect(activeProject) {
        refreshGit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .testTag("screen_git")
    ) {
        // Git Header & Branch Switcher
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Branch chip
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { showBranchMenu = true },
                        label = { Text("Branch: $currentBranch", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                contentDescription = null,
                                tint = NovaCyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showBranchMenu,
                        onDismissRequest = { showBranchMenu = false }
                    ) {
                        branches.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b, fontWeight = if (b == currentBranch) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    showBranchMenu = false
                                    coroutineScope.launch {
                                        gitManager.switchBranch(b)
                                        refreshGit()
                                    }
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ New Branch...", color = NovaCyanPrimary) },
                            onClick = {
                                showBranchMenu = false
                                showNewBranchDialog = true
                            }
                        )
                    }
                }

                IconButton(
                    onClick = { refreshGit() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh Git")
                }
            }
        }

        // Tab Row: Staging vs Commit History
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = NovaCyanPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Working Tree (${fileStatuses.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Commits (${commitHistory.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
        }

        if (activeProject == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Select an active project in Workspace to manage Git.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (selectedTab == 0) {
            // Working Tree & Staging
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Commit input box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        OutlinedTextField(
                            value = commitMessage,
                            onValueChange = { commitMessage = it },
                            placeholder = { Text("Commit message (e.g. feat: implement auth module)...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val stagedCount = fileStatuses.count { it.isStaged }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$stagedCount files staged for commit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    if (commitMessage.isNotBlank()) {
                                        coroutineScope.launch {
                                            gitManager.createCommit(commitMessage.trim())
                                            commitMessage = ""
                                            refreshGit()
                                            Toast.makeText(context, "Committed successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = commitMessage.isNotBlank(),
                                modifier = Modifier.testTag("btn_git_commit")
                            ) {
                                Icon(imageVector = Icons.Filled.Commit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Commit")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "FILES IN REPOSITORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NovaCyanPrimary
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(fileStatuses) { fileStatus ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = fileStatus.isStaged,
                                onCheckedChange = { checked ->
                                    coroutineScope.launch {
                                        gitManager.stageFile(fileStatus.path, checked)
                                        refreshGit()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = fileStatus.path,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = if (fileStatus.isStaged) "STAGED" else "UNSTAGED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (fileStatus.isStaged) NovaEmerald else NovaCyanPrimary
                            )
                        }
                    }
                }
            }
        } else {
            // Commit History
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(commitHistory) { commit ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Commit,
                                        contentDescription = null,
                                        tint = NovaCyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = commit.hash,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = NovaCyanPrimary
                                    )
                                }

                                Text(
                                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(commit.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = commit.message,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "By ${commit.author} on [${commit.branch}]",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // New Branch Dialog
    if (showNewBranchDialog) {
        var branchInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewBranchDialog = false },
            title = { Text("Create New Branch") },
            text = {
                OutlinedTextField(
                    value = branchInput,
                    onValueChange = { branchInput = it },
                    label = { Text("Branch name (e.g. feature/auth)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = branchInput.trim()
                        if (trimmed.isNotBlank()) {
                            showNewBranchDialog = false
                            coroutineScope.launch {
                                gitManager.createBranch(trimmed)
                                refreshGit()
                            }
                        }
                    }
                ) {
                    Text("Create & Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBranchDialog = false }) { Text("Cancel") }
            }
        )
    }
}
