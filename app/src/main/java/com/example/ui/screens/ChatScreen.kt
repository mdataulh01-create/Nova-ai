package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AiService
import com.example.core.ai.ModelOption
import com.example.core.ai.ProviderType
import com.example.core.ai.SupportedModels
import com.example.core.database.ConversationEntity
import com.example.core.database.MessageEntity
import com.example.core.database.NovaDatabase
import com.example.core.storage.ProjectInfo
import com.example.core.storage.StorageManager
import com.example.ui.theme.NovaCodeBackground
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaVioletAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    activeProject: ProjectInfo?,
    storageManager: StorageManager,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { NovaDatabase.getDatabase(context) }
    val aiService = remember { AiService(context) }

    var selectedProvider by remember { mutableStateOf(ProviderType.GEMINI) }
    var selectedModel by remember { mutableStateOf("gemini-3.5-flash") }
    var showModelMenu by remember { mutableStateOf(false) }

    var currentConversationId by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var attachProjectContext by remember { mutableStateOf(true) }
    var lastError by remember { mutableStateOf<String?>(null) }

    // Collect conversations and messages from Room
    val conversations by db.novaDao().getAllConversations().collectAsState(initial = emptyList())

    // If no active conversation, pick first or create
    LaunchedEffect(conversations) {
        if (currentConversationId == null && conversations.isNotEmpty()) {
            currentConversationId = conversations.first().id
        } else if (conversations.isEmpty() && currentConversationId == null) {
            val newId = UUID.randomUUID().toString()
            db.novaDao().insertConversation(
                ConversationEntity(
                    id = newId,
                    title = "New Code Session",
                    provider = selectedProvider.name,
                    model = selectedModel
                )
            )
            // Initial welcome message
            db.novaDao().insertMessage(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = newId,
                    role = "assistant",
                    content = "👋 Welcome to **Nova AI**! I am your native mobile AI coding assistant.\n\n" +
                            "I can:\n" +
                            "- 🔍 Inspect and explain files in your active project\n" +
                            "- 🛠️ Generate functions, classes, and complete project files\n" +
                            "- 🧪 Write unit tests and refactor logic\n" +
                            "- 💻 Execute commands in the in-app Terminal\n\n" +
                            "Select your project or model from the top bar and ask anything!"
                )
            )
            currentConversationId = newId
        }
    }

    val activeConvId = currentConversationId ?: ""
    val messages by db.novaDao().getMessagesForConversation(activeConvId).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .testTag("screen_chat")
    ) {
        // Chat Header with Model Selector & Context Indicator
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Model Dropdown Chip
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { showModelMenu = true },
                            label = {
                                Text(
                                    text = "$selectedModel (${selectedProvider.displayName})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Select Model",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = NovaCyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surface,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("chip_model_selector")
                        )

                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            ProviderType.values().forEach { prov ->
                                val models = SupportedModels.getAllForProvider(prov)
                                models.forEach { m ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(m.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(prov.displayName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            selectedProvider = prov
                                            selectedModel = m.id
                                            showModelMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Clear messages button
                    IconButton(
                        onClick = {
                            if (activeConvId.isNotBlank()) {
                                coroutineScope.launch {
                                    db.novaDao().deleteMessagesForConversation(activeConvId)
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Active Project context chip
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Topic,
                            contentDescription = null,
                            tint = if (attachProjectContext) NovaVioletAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activeProject != null) "Project: ${activeProject.name}" else "No active project",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilterChip(
                        selected = attachProjectContext,
                        onClick = { attachProjectContext = !attachProjectContext },
                        label = { Text("Include Context", fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = null,
                            tint = NovaCyanPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Start a conversation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask Nova AI to generate code, write tests, explain files, or diagnose issues.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onCopy = { text ->
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Nova AI", text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (isGenerating) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = NovaCyanPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nova AI is thinking...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Error banner if any
            if (lastError != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lastError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { lastError = null }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Input Field
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Nova AI to build, edit, or test...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_chat_prompt"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaCyanPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val prompt = inputText.trim()
                        if (prompt.isNotBlank() && !isGenerating && activeConvId.isNotBlank()) {
                            inputText = ""
                            lastError = null
                            isGenerating = true

                            coroutineScope.launch {
                                // Save user message to Room
                                val userMsg = MessageEntity(
                                    id = UUID.randomUUID().toString(),
                                    conversationId = activeConvId,
                                    role = "user",
                                    content = prompt
                                )
                                db.novaDao().insertMessage(userMsg)

                                // Prepare project context if enabled
                                val projCtx = if (attachProjectContext && activeProject != null) {
                                    val projDir = File(activeProject.rootPath)
                                    val files = projDir.walkTopDown().maxDepth(3).filter { it.isFile }.take(10)
                                        .map { it.name }.joinToString(", ")
                                    "Project '${activeProject.name}' with files: $files"
                                } else null

                                val response = withContext(Dispatchers.IO) {
                                    aiService.generateResponse(
                                        prompt = prompt,
                                        provider = selectedProvider,
                                        modelName = selectedModel,
                                        projectContext = projCtx
                                    )
                                }

                                isGenerating = false

                                if (response.isError) {
                                    lastError = response.errorMessage
                                    // Save error message to chat
                                    db.novaDao().insertMessage(
                                        MessageEntity(
                                            id = UUID.randomUUID().toString(),
                                            conversationId = activeConvId,
                                            role = "assistant",
                                            content = "⚠️ **Error:** ${response.errorMessage}\n\nPlease check your API key in Settings > AI Providers."
                                        )
                                    )
                                } else {
                                    db.novaDao().insertMessage(
                                        MessageEntity(
                                            id = UUID.randomUUID().toString(),
                                            conversationId = activeConvId,
                                            role = "assistant",
                                            content = response.content,
                                            tokensUsed = response.tokensUsed
                                        )
                                    )
                                }
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (inputText.isNotBlank()) NovaCyanPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .testTag("btn_send_chat")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank()) Color(0xFF00363F) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: MessageEntity,
    onCopy: (String) -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(NovaCyanPrimary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = "AI",
                    tint = NovaCyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                }
            ),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Render message text with basic code block parsing
                MessageContentRenderer(content = message.content, onCopy = onCopy)

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "You" else "Nova AI",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    IconButton(
                        onClick = { onCopy(message.content) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "User",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MessageContentRenderer(content: String, onCopy: (String) -> Unit) {
    // Check if message contains code blocks (```)
    val parts = content.split("```")
    if (parts.size <= 1) {
        // Plain text
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEachIndexed { index, part ->
                if (index % 2 == 0) {
                    // Regular text
                    if (part.isNotBlank()) {
                        Text(
                            text = part.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // Code block
                    val lines = part.trim().lines()
                    val lang = lines.firstOrNull()?.takeIf { it.isNotBlank() && !it.contains(" ") } ?: "code"
                    val codeContent = if (lines.size > 1 && lines.first().contains(lang)) {
                        lines.drop(1).joinToString("\n")
                    } else {
                        part.trim()
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NovaCodeBackground),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF131D31))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lang.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NovaCyanPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                IconButton(
                                    onClick = { onCopy(codeContent) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy Code",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Text(
                                text = codeContent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
