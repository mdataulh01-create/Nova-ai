package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.storage.TemplateManager
import com.example.ui.theme.NovaCyanPrimary
import java.util.Locale

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

fun getFileIconColor(ext: String): Color {
    return when (ext.lowercase()) {
        "kt", "kts" -> Color(0xFF7F52FF)
        "py" -> Color(0xFF3776AB)
        "js", "ts" -> Color(0xFFF7DF1E)
        "html" -> Color(0xFFE34F26)
        "css" -> Color(0xFF1572B6)
        "json" -> Color(0xFFCB3837)
        "md" -> Color(0xFF06B6D4)
        "sh", "bash" -> Color(0xFF4EAA25)
        "xml" -> Color(0xFFFFA000)
        "zip" -> Color(0xFFA855F7)
        else -> Color(0xFF94A3B8)
    }
}

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, template: String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(TemplateManager.availableTemplates.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Starter Template:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = NovaCyanPrimary
                )

                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(TemplateManager.availableTemplates) { (tmpl, desc) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplate = tmpl }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedTemplate == tmpl,
                                onClick = { selectedTemplate = tmpl }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(tmpl, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isNotBlank()) {
                        onCreate(projectName.trim(), selectedTemplate)
                    }
                },
                enabled = projectName.isNotBlank()
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
