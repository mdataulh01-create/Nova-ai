package com.example.core.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class CommandResult(
    val command: String,
    val output: String,
    val exitCode: Int,
    val isError: Boolean = exitCode != 0,
    val durationMs: Long = 0L
)

enum class CommandSafety {
    SAFE,
    REQUIRES_APPROVAL,
    BLOCKED
}

class TerminalProcessManager(initialDir: File) {

    var currentDirectory: File = initialDir
        private set

    private var activeProcess: Process? = null

    companion object {
        private val DANGEROUS_PATTERNS = listOf(
            Regex("\\brm\\s+-r[fF]?\\s+/"),
            Regex("\\bmkfs"),
            Regex("\\bdd\\s+if="),
            Regex("\\bchmod\\s+-R\\s+777\\s+/"),
            Regex(":()\\{ :\\|:& \\};:") // Fork bomb
        )

        private val APPROVAL_PATTERNS = listOf(
            Regex("\\brm\\b"),
            Regex("\\bmv\\b"),
            Regex("\\bkill\\b"),
            Regex("\\bkillall\\b"),
            Regex("\\bchmod\\b")
        )
    }

    fun setDirectory(dir: File) {
        if (dir.exists() && dir.isDirectory) {
            currentDirectory = dir
        }
    }

    fun evaluateSafety(command: String): CommandSafety {
        val trimmed = command.trim()
        for (pattern in DANGEROUS_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return CommandSafety.BLOCKED
            }
        }
        for (pattern in APPROVAL_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return CommandSafety.REQUIRES_APPROVAL
            }
        }
        return CommandSafety.SAFE
    }

    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return@withContext CommandResult(command, "", 0)
        }

        // Handle internal shell 'cd' navigation
        if (trimmed.startsWith("cd ") || trimmed == "cd") {
            val targetPath = trimmed.removePrefix("cd").trim()
            val newDir = when {
                targetPath.isEmpty() || targetPath == "~" -> currentDirectory
                targetPath.startsWith("/") -> File(targetPath)
                else -> File(currentDirectory, targetPath)
            }.canonicalFile

            return@withContext if (newDir.exists() && newDir.isDirectory) {
                currentDirectory = newDir
                CommandResult(command, "Changed directory to ${newDir.absolutePath}", 0)
            } else {
                CommandResult(command, "cd: no such file or directory: $targetPath", 1, isError = true)
            }
        }

        val startTime = System.currentTimeMillis()
        try {
            val pb = ProcessBuilder("sh", "-c", trimmed)
            pb.directory(currentDirectory)
            pb.redirectErrorStream(true)

            // Add standard environment paths
            val env = pb.environment()
            val existingPath = env["PATH"] ?: "/system/bin:/system/xbin"
            env["PATH"] = "$existingPath:/apex/com.android.runtime/bin"
            env["TERM"] = "xterm-256color"
            env["PWD"] = currentDirectory.absolutePath

            val process = pb.start()
            activeProcess = process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val outputBuilder = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                outputBuilder.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            val duration = System.currentTimeMillis() - startTime
            activeProcess = null

            CommandResult(
                command = command,
                output = outputBuilder.toString().trimEnd(),
                exitCode = exitCode,
                isError = exitCode != 0,
                durationMs = duration
            )
        } catch (e: Exception) {
            activeProcess = null
            CommandResult(
                command = command,
                output = "Execution failed: ${e.localizedMessage}",
                exitCode = -1,
                isError = true,
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }

    fun terminateActiveProcess() {
        try {
            activeProcess?.destroy()
            activeProcess = null
        } catch (ignored: Exception) {}
    }
}
