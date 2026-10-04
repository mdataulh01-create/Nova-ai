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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Grading
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AgentExecutionPlan
import com.example.core.ai.AgentRole
import com.example.core.ai.AiService
import com.example.core.ai.CodingAgent
import com.example.core.ai.MultiAgentLog
import com.example.core.runtime.TerminalProcessManager
import com.example.core.storage.ProjectInfo
import com.example.core.storage.StorageManager
import com.example.ui.theme.NovaCodeBackground
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaRose
import com.example.ui.theme.NovaVioletAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AgentsScreen(
    storageManager: StorageManager,
    activeProject: ProjectInfo?,
    onOpenFile: (File) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { AiService(context) }

    var selectedRole by remember { mutableStateOf(AgentRole.COORDINATOR) }
    var taskInput by remember { mutableStateOf("") }
    var isPlanning by remember { mutableStateOf(false) }
    var currentPlan by remember { mutableStateOf<AgentExecutionPlan?>(null) }
    val logs = remember { mutableStateListOf<MultiAgentLog>() }

    val codingAgent = remember(activeProject) {
        val projDir = activeProject?.let { File(it.rootPath) } ?: storageManager.getProjectsDirectory()
        CodingAgent(projDir, storageManager, aiService, TerminalProcessManager(projDir))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .testTag("screen_agents")
    ) {
        // Agent Role Selector Carousel
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "SELECT AGENT ROLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NovaCyanPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AgentRole.values().forEach { role ->
                        val isSelected = selectedRole == role
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRole = role },
                            label = { Text(role.title, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getRoleIcon(role),
                                    contentDescription = null,
                                    tint = if (isSelected) NovaCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surface,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                Text(
                    text = selectedRole.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }

        // Active project warning if missing
        if (activeProject == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "⚠️ No active project selected. Select a project in the Workspace tab before running coding agent tasks.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Task Plan & Execution Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Display current plan if ready
            currentPlan?.let { plan ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Proposed Plan (${plan.role})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NovaCyanPrimary
                                )
                                Text(
                                    text = plan.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (plan.status == "EXECUTED") NovaEmerald else NovaVioletAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            plan.planSteps.forEachIndexed { idx, step ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${idx + 1}. ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NovaCyanPrimary
                                    )
                                    Text(
                                        text = step,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Proposed File Changes & Diffs
                items(plan.fileChanges) { change ->
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
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Difference,
                                        contentDescription = null,
                                        tint = NovaCyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = change.filePath,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = change.actionType.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (change.actionType.name == "CREATE") NovaEmerald else NovaCyanPrimary
                                )
                            }

                            // Diff View
                            Text(
                                text = change.diffPreview.ifBlank { change.proposedContent },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                // Approval actions
                if (plan.status == "READY_FOR_APPROVAL") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val success = codingAgent.applyChanges(plan)
                                        if (success) {
                                            currentPlan = plan.copy(status = "EXECUTED")
                                            logs.add(
                                                MultiAgentLog(
                                                    role = selectedRole,
                                                    message = "✅ Applied approved changes to ${plan.fileChanges.size} files."
                                                )
                                            )
                                            Toast.makeText(context, "Changes successfully applied!", Toast.LENGTH_SHORT).show()
                                            // Open the modified file
                                            val firstFile = plan.fileChanges.firstOrNull()?.filePath
                                            if (firstFile != null && activeProject != null) {
                                                onOpenFile(File(activeProject.rootPath, firstFile))
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NovaEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve & Apply")
                            }

                            OutlinedButton(
                                onClick = {
                                    currentPlan = plan.copy(status = "REJECTED")
                                    logs.add(MultiAgentLog(role = selectedRole, message = "❌ User rejected proposed plan."))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reject")
                            }
                        }
                    }
                }
            }

            // Agent logs
            if (logs.isNotEmpty()) {
                item {
                    Text(
                        text = "AGENT ACTIVITY LOG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NovaCyanPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(logs) { log ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "[${log.role.title}]: ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NovaVioletAccent
                        )
                        Text(
                            text = log.message,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Prompt Input
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
                    value = taskInput,
                    onValueChange = { taskInput = it },
                    placeholder = {
                        Text(
                            "Instruct ${selectedRole.title} (e.g. Add auth handler, review code, create tests)...",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_agent_task"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val task = taskInput.trim()
                        if (task.isNotBlank() && !isPlanning && activeProject != null) {
                            taskInput = ""
                            isPlanning = true
                            logs.add(MultiAgentLog(role = selectedRole, message = "Received task: '$task'. Inspecting project files..."))

                            coroutineScope.launch {
                                try {
                                    val plan = withContext(Dispatchers.IO) {
                                        codingAgent.planTask(task)
                                    }
                                    currentPlan = plan
                                    logs.add(
                                        MultiAgentLog(
                                            role = selectedRole,
                                            message = "Generated ${plan.planSteps.size}-step plan with ${plan.fileChanges.size} proposed file changes. Awaiting approval."
                                        )
                                    )
                                } catch (e: Exception) {
                                    logs.add(MultiAgentLog(role = selectedRole, message = "Error: ${e.localizedMessage}"))
                                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                                } finally {
                                    isPlanning = false
                                }
                            }
                        }
                    },
                    enabled = taskInput.isNotBlank() && !isPlanning && activeProject != null,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (taskInput.isNotBlank() && activeProject != null) NovaCyanPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .testTag("btn_run_agent")
                ) {
                    if (isPlanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Run Agent",
                            tint = if (taskInput.isNotBlank()) Color(0xFF00363F) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun getRoleIcon(role: AgentRole): ImageVector {
    return when (role) {
        AgentRole.COORDINATOR -> Icons.Filled.Hub
        AgentRole.ARCHITECT -> Icons.Filled.Architecture
        AgentRole.DEVELOPER -> Icons.Filled.Code
        AgentRole.CODE_REVIEWER -> Icons.AutoMirrored.Filled.Grading
        AgentRole.DEBUGGER -> Icons.Filled.BugReport
        AgentRole.TESTER -> Icons.Filled.CheckCircle
        AgentRole.DOCUMENTATION -> Icons.AutoMirrored.Filled.MenuBook
    }
}
