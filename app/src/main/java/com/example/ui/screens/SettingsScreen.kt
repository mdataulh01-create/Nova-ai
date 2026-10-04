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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AiService
import com.example.core.ai.ProviderType
import com.example.core.firebase.FirebaseAuthHelper
import com.example.core.security.KeystoreHelper
import com.example.core.storage.StorageConfig
import com.example.core.storage.StorageManager
import com.example.core.storage.StorageType
import com.example.ui.formatBytes
import com.example.ui.theme.NovaCyanPrimary
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaVioletAccent
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    storageManager: StorageManager,
    onTriggerSafPicker: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keystoreHelper = remember { KeystoreHelper(context) }
    val authHelper = remember { FirebaseAuthHelper(context) }
    val aiService = remember { AiService(context) }

    val userProfile by authHelper.currentUser.collectAsState()
    var storageConfig by remember { mutableStateOf(storageManager.getStorageConfig()) }

    var selectedProviderForConfig by remember { mutableStateOf<ProviderType?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .testTag("screen_settings")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title Header
            item {
                Text(
                    text = "Nova AI Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NovaCyanPrimary
                )
                Text(
                    text = "Configure AI providers, device storage, runtime, and security credentials.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // AI Providers Section
            item {
                SettingsSectionHeader(title = "AI PROVIDERS & CREDENTIALS", icon = Icons.Filled.Key)
            }

            items(ProviderType.values().size) { idx ->
                val prov = ProviderType.values()[idx]
                val currentKey = keystoreHelper.getDecryptedString("api_key_${prov.name}", "")
                val isConfigured = currentKey.isNotBlank() || (prov == ProviderType.GEMINI && aiService.getEffectiveApiKey(prov).isNotBlank())

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prov.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = if (isConfigured) "● Key Configured (Keystore encrypted)" else "○ Not configured",
                                fontSize = 11.sp,
                                color = if (isConfigured) NovaEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { selectedProviderForConfig = prov }
                        ) {
                            Text("Configure")
                        }
                    }
                }
            }

            // Storage Section
            item {
                SettingsSectionHeader(title = "UNIFIED DEVICE STORAGE", icon = Icons.Filled.Storage)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Storage Root Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = storageConfig.displayPath,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Free Space: ${formatBytes(storageConfig.freeSpaceBytes)} / ${formatBytes(storageConfig.totalSpaceBytes)}",
                            fontSize = 11.sp,
                            color = NovaCyanPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onTriggerSafPicker,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select SAF Folder", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    storageManager.markStorageInitialized(StorageType.APP_PRIVATE)
                                    storageConfig = storageManager.getStorageConfig()
                                    Toast.makeText(context, "Using App-Private sandbox", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset to Sandbox", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Runtime Architecture
            item {
                SettingsSectionHeader(title = "EXECUTION RUNTIME & LINUX", icon = Icons.Filled.Terminal)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = NovaCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active Runtime: Android POSIX Sandbox",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Commands execute via native JVM ProcessBuilder inside your project's directory. Security sandbox is strictly enforced without root. Destructive commands require explicit approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Firebase & Account Section
            item {
                SettingsSectionHeader(title = "ACCOUNT & CLOUD PERSISTENCE", icon = Icons.Filled.AccountCircle)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = userProfile?.displayName ?: "Local Developer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = userProfile?.email ?: "developer@nova-ai.local",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (userProfile?.isAnonymous == true) {
                                Button(onClick = { showAuthDialog = true }) {
                                    Text("Sign In")
                                }
                            } else {
                                OutlinedButton(onClick = { authHelper.signOut() }) {
                                    Text("Sign Out")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = NovaEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Local-First Offline Persistence", fontSize = 12.sp)
                            }
                            Text(
                                "ENABLED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NovaEmerald
                            )
                        }
                    }
                }
            }
        }
    }

    // Configure Provider Dialog
    selectedProviderForConfig?.let { prov ->
        var inputKey by remember { mutableStateOf(keystoreHelper.getDecryptedString("api_key_${prov.name}", "")) }
        var isVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedProviderForConfig = null },
            title = { Text("Configure ${prov.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your API key. Stored using Android Keystore AES encryption and redacted from logs and exports.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isVisible = !isVisible }) {
                                Icon(
                                    imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keystoreHelper.saveEncryptedString("api_key_${prov.name}", inputKey.trim())
                        selectedProviderForConfig = null
                        Toast.makeText(context, "${prov.displayName} key saved securely!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProviderForConfig = null }) { Text("Cancel") }
            }
        )
    }

    // Auth Dialog
    if (showAuthDialog) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("Firebase Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            isSubmitting = true
                            coroutineScope.launch {
                                authHelper.signInWithEmail(email.trim(), password.trim()) { success, msg ->
                                    isSubmitting = false
                                    showAuthDialog = false
                                    Toast.makeText(context, msg ?: if (success) "Signed in!" else "Failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            isSubmitting = true
                            coroutineScope.launch {
                                authHelper.signUpWithEmail(email.trim(), password.trim()) { success, msg ->
                                    isSubmitting = false
                                    showAuthDialog = false
                                    Toast.makeText(context, msg ?: if (success) "Account created!" else "Failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    Text("Sign Up")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NovaCyanPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = NovaCyanPrimary
        )
    }
}
