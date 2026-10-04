package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.runtime.CommandResult
import com.example.core.runtime.CommandSafety
import com.example.core.runtime.TerminalProcessManager
import com.example.core.storage.ProjectInfo
import com.example.core.storage.StorageManager
import com.example.ui.theme.NovaCodeBackground
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaRose
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TerminalScreen(
    storageManager: StorageManager,
    activeProject: ProjectInfo?
) {
    val coroutineScope = rememberCoroutineScope()
    val initialDir = activeProject?.let { File(it.rootPath) } ?: storageManager.getTerminalWorkspaceDirectory()
    val processManager = remember(activeProject) { TerminalProcessManager(initialDir) }

    val history = remember { mutableStateListOf<CommandResult>() }
    var currentInput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var pendingApprovalCommand by remember { mutableStateOf<String?>(null) }
    var blockedCommandNotice by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Add initial terminal welcome message
    LaunchedEffect(Unit) {
        if (history.isEmpty()) {
            history.add(
                CommandResult(
                    command = "system --info",
                    output = "Nova AI Embedded Terminal [Runtime: Android Toybox/POSIX Shell]\n" +
                            "Working Dir: ${processManager.currentDirectory.absolutePath}\n" +
                            "Security: Sandbox Enforced (No Root / Approval Gate Active)\n" +
                            "Type 'ls', 'pwd', 'cat <file>', or tap a quick command below.",
                    exitCode = 0
                )
            )
        }
    }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    fun dispatchCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        when (processManager.evaluateSafety(trimmed)) {
            CommandSafety.BLOCKED -> {
                blockedCommandNotice = "Command '$trimmed' is blocked for system security."
                return
            }
            CommandSafety.REQUIRES_APPROVAL -> {
                pendingApprovalCommand = trimmed
                return
            }
            CommandSafety.SAFE -> {
                // Execute directly
            }
        }

        currentInput = ""
        isRunning = true
        coroutineScope.launch {
            val result = processManager.executeCommand(trimmed)
            history.add(result)
            isRunning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .background(NovaCodeBackground)
            .testTag("screen_terminal")
    ) {
        // Terminal Status Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = null,
                        tint = NovaCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PWD: ~/${processManager.currentDirectory.name}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1
                    )
                }

                Row {
                    if (isRunning) {
                        IconButton(
                            onClick = { processManager.terminateActiveProcess() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Kill Process",
                                tint = NovaRose,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { history.clear() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ClearAll,
                            contentDescription = "Clear Terminal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Quick Command Chips
        val quickCommands = listOf("ls -la", "pwd", "date", "whoami", "df -h", "cat README.md")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCommands.forEach { cmd ->
                FilterChip(
                    selected = false,
                    onClick = { dispatchCommand(cmd) },
                    label = {
                        Text(
                            text = cmd,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.height(26.dp)
                )
            }
        }

        // Terminal Output History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { item ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "nova@android:~$ ",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NovaCyanPrimary
                        )
                        Text(
                            text = item.command,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (item.exitCode == 0) "0" else "code ${item.exitCode}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (item.exitCode == 0) NovaEmerald else NovaRose
                        )
                    }

                    if (item.output.isNotBlank()) {
                        Text(
                            text = item.output,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (item.isError) NovaRose.copy(alpha = 0.9f) else Color(0xFFCBD5E1),
                            modifier = Modifier.padding(top = 2.dp, start = 8.dp)
                        )
                    }
                }
            }

            if (isRunning) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = NovaCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Executing in sandbox...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NovaCyanPrimary
                        )
                    }
                }
            }
        }

        // Input Line
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NovaCyanPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { currentInput = it },
                    placeholder = { Text("enter command...", fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_terminal_cmd"),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.White
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaCyanPrimary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                IconButton(
                    onClick = { dispatchCommand(currentInput) },
                    enabled = currentInput.isNotBlank() && !isRunning,
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (currentInput.isNotBlank()) NovaCyanPrimary else Color.Transparent, CircleShape)
                        .testTag("btn_run_terminal")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Execute",
                        tint = if (currentInput.isNotBlank()) Color(0xFF00363F) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Command Approval Dialog
    if (pendingApprovalCommand != null) {
        val cmd = pendingApprovalCommand!!
        AlertDialog(
            onDismissRequest = { pendingApprovalCommand = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Security Alert",
                    tint = NovaRose
                )
            },
            title = { Text("Command Approval Required") },
            text = {
                Column {
                    Text(
                        "This command may modify or delete files. Review carefully before execution:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cmd,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NovaRose,
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingApprovalCommand = null
                        currentInput = ""
                        isRunning = true
                        coroutineScope.launch {
                            val result = processManager.executeCommand(cmd)
                            history.add(result)
                            isRunning = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaRose)
                ) {
                    Text("Approve & Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingApprovalCommand = null }) { Text("Reject") }
            }
        )
    }

    // Blocked Dialog
    if (blockedCommandNotice != null) {
        AlertDialog(
            onDismissRequest = { blockedCommandNotice = null },
            icon = { Icon(Icons.Filled.Block, contentDescription = null, tint = NovaRose) },
            title = { Text("Command Blocked") },
            text = { Text(blockedCommandNotice ?: "") },
            confirmButton = {
                Button(onClick = { blockedCommandNotice = null }) { Text("OK") }
            }
        )
    }
}
