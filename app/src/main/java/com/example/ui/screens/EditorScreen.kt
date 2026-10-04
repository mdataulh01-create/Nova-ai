package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.StorageManager
import com.example.ui.editor.SyntaxHighlighter
import com.example.ui.theme.NovaCodeBackground
import com.example.ui.theme.NovaCodeGutter
import com.example.ui.theme.NovaCodeLineNumber
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaVioletAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EditorScreen(
    storageManager: StorageManager,
    openFiles: List<File>,
    activeFile: File?,
    onSelectFile: (File) -> Unit,
    onCloseFile: (File) -> Unit,
    onSendToAgent: (File, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val fileContents = remember { mutableStateMapOf<String, String>() }
    val dirtyState = remember { mutableStateMapOf<String, Boolean>() }
    val undoHistory = remember { mutableStateMapOf<String, MutableList<String>>() }

    var fontSize by remember { mutableStateOf(13.sp) }
    var isWordWrap by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    var showGoToLineDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    // Load file contents when tabs change
    LaunchedEffect(openFiles) {
        withContext(Dispatchers.IO) {
            openFiles.forEach { file ->
                if (!fileContents.containsKey(file.absolutePath)) {
                    val text = storageManager.readFile(file)
                    fileContents[file.absolutePath] = text
                    dirtyState[file.absolutePath] = false
                    undoHistory[file.absolutePath] = mutableListOf(text)
                }
            }
        }
    }

    val currentFile = activeFile ?: openFiles.firstOrNull()
    val currentContent = currentFile?.let { fileContents[it.absolutePath] } ?: ""
    val isCurrentDirty = currentFile?.let { dirtyState[it.absolutePath] ?: false } ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .testTag("screen_editor")
    ) {
        if (openFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = null,
                        tint = NovaCyanPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No open files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Open a file from the Projects explorer to edit code",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Tabs Bar
            ScrollableTabRow(
                selectedTabIndex = openFiles.indexOf(currentFile).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = NovaCyanPrimary,
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                openFiles.forEach { file ->
                    val isSelected = file == currentFile
                    val isDirty = dirtyState[file.absolutePath] ?: false

                    Tab(
                        selected = isSelected,
                        onClick = { onSelectFile(file) },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            if (isDirty) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(NovaCyanPrimary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = file.name,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NovaCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onCloseFile(file) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close tab",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Editor Actions Toolbar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left controls: Save, Save As, Undo, Search, Go to line
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (currentFile != null) {
                                    coroutineScope.launch {
                                        val saved = withContext(Dispatchers.IO) {
                                            storageManager.saveFile(currentFile, currentContent)
                                        }
                                        if (saved) {
                                            dirtyState[currentFile.absolutePath] = false
                                            Toast.makeText(context, "Saved ${currentFile.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp),
                            enabled = isCurrentDirty
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Save file",
                                tint = if (isCurrentDirty) NovaCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSaveAsDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SaveAs,
                                contentDescription = "Save As",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentFile != null) {
                                    val history = undoHistory[currentFile.absolutePath]
                                    if (history != null && history.size > 1) {
                                        history.removeLast()
                                        val prev = history.last()
                                        fileContents[currentFile.absolutePath] = prev
                                        dirtyState[currentFile.absolutePath] = true
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = if (showSearchBar) NovaCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showGoToLineDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatLineSpacing,
                                contentDescription = "Go to line",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Right controls: AI Agent inspect, Font size, Word wrap
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (currentFile != null) {
                                    onSendToAgent(currentFile, currentContent)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Send to Agent",
                                tint = NovaVioletAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { isWordWrap = !isWordWrap },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.WrapText,
                                contentDescription = "Toggle wrap",
                                tint = if (isWordWrap) NovaCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                fontSize = if (fontSize.value >= 16f) 11.sp else (fontSize.value + 1f).sp
                            }
                        ) {
                            Text(
                                text = "${fontSize.value.toInt()}pt",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search & Replace bar
            AnimatedVisibility(visible = showSearchBar) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            placeholder = { Text("Replace...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        )
                        IconButton(
                            onClick = {
                                if (currentFile != null && searchQuery.isNotEmpty()) {
                                    val replaced = currentContent.replace(searchQuery, replaceQuery)
                                    fileContents[currentFile.absolutePath] = replaced
                                    dirtyState[currentFile.absolutePath] = true
                                    Toast.makeText(context, "Replaced matches", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FindReplace,
                                contentDescription = "Replace all",
                                tint = NovaCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Code View: Line Gutter + Editor
            val lineCount = currentContent.lines().size.coerceAtLeast(1)
            val lineNumbersString = (1..lineCount).joinToString("\n") { it.toString().padStart(3) }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(NovaCodeBackground)
            ) {
                // Line Number Gutter
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(42.dp)
                        .background(NovaCodeGutter)
                        .padding(top = 12.dp, end = 6.dp)
                        .verticalScroll(verticalScroll),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = lineNumbersString,
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize,
                        color = NovaCodeLineNumber,
                        lineHeight = (fontSize.value * 1.4f).sp
                    )
                }

                // Code Input Field with visual syntax coloring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, top = 12.dp, end = 8.dp)
                        .verticalScroll(verticalScroll)
                        .then(if (!isWordWrap) Modifier.horizontalScroll(horizontalScroll) else Modifier)
                ) {
                    BasicTextField(
                        value = currentContent,
                        onValueChange = { newText ->
                            if (currentFile != null) {
                                fileContents[currentFile.absolutePath] = newText
                                dirtyState[currentFile.absolutePath] = true
                                val hist = undoHistory.getOrPut(currentFile.absolutePath) { mutableListOf() }
                                hist.add(newText)
                                if (hist.size > 50) hist.removeFirst()
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize,
                            color = Color(0xFFE2E8F0),
                            lineHeight = (fontSize.value * 1.4f).sp
                        ),
                        cursorBrush = SolidColor(NovaCyanPrimary),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("editor_text_field")
                    )
                }
            }

            // Bottom Status Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${currentFile?.extension?.uppercase() ?: "TEXT"} • UTF-8",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NovaCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$lineCount lines • ${currentContent.length} chars",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = if (isCurrentDirty) "● Unsaved" else "Saved",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentDirty) NovaCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Go To Line Dialog
    if (showGoToLineDialog) {
        var lineInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGoToLineDialog = false },
            title = { Text("Go to Line") },
            text = {
                OutlinedTextField(
                    value = lineInput,
                    onValueChange = { lineInput = it },
                    label = { Text("Line number (1 - ${currentContent.lines().size})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val line = lineInput.toIntOrNull()
                        if (line != null && line > 0) {
                            showGoToLineDialog = false
                            coroutineScope.launch {
                                val approxScroll = (line - 1) * (fontSize.value * 1.4f * 2.5f).toInt()
                                verticalScroll.animateScrollTo(approxScroll)
                            }
                        }
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToLineDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Save As Dialog
    if (showSaveAsDialog && currentFile != null) {
        var newFileName by remember { mutableStateOf(currentFile.name) }
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("Save As") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newFileName.trim()
                        if (trimmed.isNotBlank()) {
                            showSaveAsDialog = false
                            coroutineScope.launch {
                                try {
                                    val newFile = File(currentFile.parentFile ?: currentFile, trimmed)
                                    val saved = withContext(Dispatchers.IO) {
                                        storageManager.saveFile(newFile, currentContent)
                                    }
                                    if (saved) {
                                        onSelectFile(newFile)
                                        Toast.makeText(context, "Saved as $trimmed", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Save As")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsDialog = false }) { Text("Cancel") }
            }
        )
    }
}
