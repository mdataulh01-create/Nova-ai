package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    CHAT("chat", "Chat", Icons.AutoMirrored.Filled.Chat, "nav_chat"),
    PROJECTS("projects", "Projects", Icons.Filled.Folder, "nav_projects"),
    EDITOR("editor", "Editor", Icons.Filled.Code, "nav_editor"),
    TERMINAL("terminal", "Terminal", Icons.Filled.Terminal, "nav_terminal"),
    AGENTS("agents", "Agents", Icons.Filled.Groups, "nav_agents"),
    GIT("git", "Git", Icons.Filled.AccountTree, "nav_git"),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, "nav_settings")
}
