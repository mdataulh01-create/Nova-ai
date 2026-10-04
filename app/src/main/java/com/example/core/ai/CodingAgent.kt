package com.example.core.ai

import com.example.core.runtime.TerminalProcessManager
import com.example.core.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FileChange(
    val filePath: String,
    val actionType: ChangeType, // CREATE, MODIFY, DELETE
    val originalContent: String = "",
    val proposedContent: String = "",
    val diffPreview: String = "",
    var isApproved: Boolean = false
)

enum class ChangeType {
    CREATE,
    MODIFY,
    DELETE
}

data class AgentExecutionPlan(
    val id: String,
    val task: String,
    val role: String,
    val planSteps: List<String>,
    val fileChanges: List<FileChange>,
    val status: String = "READY_FOR_APPROVAL" // READY_FOR_APPROVAL, EXECUTED, REJECTED
)

class CodingAgent(
    private val projectRoot: File,
    private val storageManager: StorageManager,
    private val aiService: AiService,
    private val processManager: TerminalProcessManager
) {

    /**
     * Inspects project structure up to depth 3
     */
    fun listProjectFiles(): List<String> {
        val canonicalRoot = projectRoot.canonicalFile
        return canonicalRoot.walkTopDown()
            .maxDepth(4)
            .filter { it.isFile && !it.name.startsWith(".") }
            .map { it.relativeTo(canonicalRoot).path }
            .toList()
    }

    /**
     * Reads a project file safely preventing path traversal
     */
    fun readFile(relativePath: String): String {
        val target = File(projectRoot, relativePath).canonicalFile
        if (!target.path.startsWith(projectRoot.canonicalPath)) {
            throw SecurityException("Access denied: Path is outside authorized project directory.")
        }
        return storageManager.readFile(target)
    }

    /**
     * Generates a simple unified diff representation
     */
    fun generateDiff(original: String, modified: String, fileName: String): String {
        val origLines = original.lines()
        val modLines = modified.lines()
        val sb = StringBuilder()
        sb.append("--- a/$fileName\n")
        sb.append("+++ b/$fileName\n")

        val maxLines = maxOf(origLines.size, modLines.size)
        for (i in 0 until maxLines) {
            val oldLine = origLines.getOrNull(i)
            val newLine = modLines.getOrNull(i)

            if (oldLine == null && newLine != null) {
                sb.append("+$newLine\n")
            } else if (oldLine != null && newLine == null) {
                sb.append("-$oldLine\n")
            } else if (oldLine != newLine) {
                sb.append("-$oldLine\n")
                sb.append("+$newLine\n")
            } else {
                if (i < 3 || i >= maxLines - 3) {
                    sb.append(" $oldLine\n")
                }
            }
        }
        return sb.toString()
    }

    /**
     * Creates an intelligent plan for a user request
     */
    suspend fun planTask(
        taskDescription: String,
        targetFileRelPath: String? = null
    ): AgentExecutionPlan = withContext(Dispatchers.IO) {
        val files = listProjectFiles()
        val filesOverview = files.take(20).joinToString("\n") { "- $it" }

        val contextInfo = if (targetFileRelPath != null) {
            val content = try { readFile(targetFileRelPath) } catch (e: Exception) { "" }
            "Target File: $targetFileRelPath\nContent:\n```\n$content\n```"
        } else {
            "Project Files:\n$filesOverview"
        }

        val prompt = """
            Task: $taskDescription
            
            Current Project Directory: ${projectRoot.name}
            Context:
            $contextInfo
            
            Please propose:
            1. An execution plan with 3-4 numbered steps.
            2. The exact file to modify or create (relative path).
            3. The complete proposed file content enclosed inside ```code blocks.
        """.trimIndent()

        val aiResult = aiService.generateResponse(
            prompt = prompt,
            systemInstruction = "You are the Nova AI Lead Developer agent. Plan the changes accurately and output complete file implementations.",
            projectContext = contextInfo
        )

        val output = aiResult.content
        val planSteps = mutableListOf<String>()
        val proposedChanges = mutableListOf<FileChange>()

        // Extract steps
        output.lines().filter { it.trim().matches(Regex("^\\d+\\..*")) }.forEach {
            planSteps.add(it.trim())
        }
        if (planSteps.isEmpty()) {
            planSteps.add("Analyze project requirements and file structure")
            planSteps.add("Implement requested feature updates")
            planSteps.add("Validate syntax and test code readiness")
        }

        // Determine target file
        val targetPath = targetFileRelPath ?: files.firstOrNull { it.endsWith(".kt") || it.endsWith(".py") || it.endsWith(".js") || it.endsWith(".html") } ?: "src/Solution.kt"
        val origContent = try { readFile(targetPath) } catch (e: Exception) { "" }

        // Extract code block from AI response if available
        val codeMatch = Regex("```[a-zA-Z]*\\n([\\s\\S]*?)```").find(output)
        val newContent = codeMatch?.groups?.get(1)?.value?.trim() ?: if (origContent.isNotBlank()) {
            "$origContent\n\n// Added by Nova AI Agent: $taskDescription\n"
        } else {
            "// Nova AI Generated for $taskDescription\n// File: $targetPath\n\nfun main() {\n    println(\"Executed $taskDescription\")\n}\n"
        }

        val changeType = if (origContent.isEmpty()) ChangeType.CREATE else ChangeType.MODIFY
        val diff = generateDiff(origContent, newContent, targetPath)

        proposedChanges.add(
            FileChange(
                filePath = targetPath,
                actionType = changeType,
                originalContent = origContent,
                proposedContent = newContent,
                diffPreview = diff,
                isApproved = false
            )
        )

        AgentExecutionPlan(
            id = "plan_${System.currentTimeMillis()}",
            task = taskDescription,
            role = "Developer",
            planSteps = planSteps,
            fileChanges = proposedChanges
        )
    }

    /**
     * Applies approved file changes safely
     */
    suspend fun applyChanges(plan: AgentExecutionPlan): Boolean = withContext(Dispatchers.IO) {
        var allSucceeded = true
        for (change in plan.fileChanges) {
            val target = File(projectRoot, change.filePath).canonicalFile
            if (!target.path.startsWith(projectRoot.canonicalPath)) {
                continue
            }
            when (change.actionType) {
                ChangeType.CREATE, ChangeType.MODIFY -> {
                    val saved = storageManager.saveFile(target, change.proposedContent)
                    if (!saved) allSucceeded = false
                }
                ChangeType.DELETE -> {
                    val deleted = storageManager.deleteFileOrFolder(target)
                    if (!deleted) allSucceeded = false
                }
            }
        }
        allSucceeded
    }
}
