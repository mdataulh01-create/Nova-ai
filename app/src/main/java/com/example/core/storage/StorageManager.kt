package com.example.core.storage

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class StorageManager private constructor(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("nova_ai_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "StorageManager"
        private const val KEY_STORAGE_TYPE = "storage_type"
        private const val KEY_SAF_URI = "saf_authorized_uri"
        private const val KEY_STORAGE_INITIALIZED = "storage_initialized"

        val SUBDIRECTORIES = listOf(
            "Projects",
            "Files",
            "Downloads",
            "Backups",
            "Exports",
            "Temp",
            "Terminal",
            "Linux",
            "Config"
        )

        @Volatile
        private var instance: StorageManager? = null

        fun getInstance(context: Context): StorageManager {
            return instance ?: synchronized(this) {
                instance ?: StorageManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getRootDirectory(): File {
        val root = File(context.filesDir, "NovaAI")
        if (!root.exists()) {
            root.mkdirs()
        }
        ensureSubdirectories(root)
        return root
    }

    fun getProjectsDirectory(): File {
        val dir = File(getRootDirectory(), "Projects")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExportsDirectory(): File {
        val dir = File(getRootDirectory(), "Exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getBackupsDirectory(): File {
        val dir = File(getRootDirectory(), "Backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTempDirectory(): File {
        val dir = File(getRootDirectory(), "Temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTerminalWorkspaceDirectory(): File {
        val dir = File(getRootDirectory(), "Terminal")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun ensureSubdirectories(root: File) {
        SUBDIRECTORIES.forEach { sub ->
            val subDir = File(root, sub)
            if (!subDir.exists()) {
                subDir.mkdirs()
            }
        }
    }

    fun isStorageInitialized(): Boolean {
        return sharedPrefs.getBoolean(KEY_STORAGE_INITIALIZED, false)
    }

    fun markStorageInitialized(type: StorageType, safUri: Uri? = null) {
        sharedPrefs.edit()
            .putBoolean(KEY_STORAGE_INITIALIZED, true)
            .putString(KEY_STORAGE_TYPE, type.name)
            .apply {
                if (safUri != null) {
                    putString(KEY_SAF_URI, safUri.toString())
                }
            }
            .apply()
    }

    fun getStorageConfig(): StorageConfig {
        val typeStr = sharedPrefs.getString(KEY_STORAGE_TYPE, StorageType.APP_PRIVATE.name)
        val type = try {
            StorageType.valueOf(typeStr ?: StorageType.APP_PRIVATE.name)
        } catch (e: Exception) {
            StorageType.APP_PRIVATE
        }

        val root = getRootDirectory()
        val stat = StatFs(root.path)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong

        val display = if (type == StorageType.SAF_SHARED) {
            val uriStr = sharedPrefs.getString(KEY_SAF_URI, "Internal storage/NovaAI (SAF)")
            "SAF Shared Directory: $uriStr"
        } else {
            "Internal App-Private Storage: ${root.absolutePath}"
        }

        return StorageConfig(
            storageType = type,
            displayPath = display,
            isAuthorized = true,
            freeSpaceBytes = freeBytes,
            totalSpaceBytes = totalBytes
        )
    }

    fun initializeStarterProjectsIfNeeded() {
        val projectsDir = getProjectsDirectory()
        val existing = projectsDir.listFiles { file -> file.isDirectory }
        if (existing.isNullOrEmpty()) {
            val demoProject = File(projectsDir, "NovaDemo")
            demoProject.mkdirs()
            TemplateManager.populateProject(demoProject, "Kotlin Console")

            val webProject = File(projectsDir, "WebDashboard")
            webProject.mkdirs()
            TemplateManager.populateProject(webProject, "Web HTML/JS/CSS")

            val notesProject = File(projectsDir, "DevNotes")
            notesProject.mkdirs()
            TemplateManager.populateProject(notesProject, "Markdown Notes")
        }
    }

    fun listProjects(): List<ProjectInfo> {
        val projectsDir = getProjectsDirectory()
        val dirs = projectsDir.listFiles { f -> f.isDirectory } ?: emptyArray()
        return dirs.map { dir ->
            val files = dir.walkTopDown().filter { it.isFile }.toList()
            val totalSize = files.sumOf { it.length() }
            ProjectInfo(
                name = dir.name,
                rootPath = dir.absolutePath,
                fileCount = files.size,
                totalSizeBytes = totalSize,
                lastModified = dir.lastModified()
            )
        }.sortedByDescending { it.lastModified }
    }

    fun createProject(name: String, template: String): File {
        val sanitized = sanitizeFileName(name)
        val projectDir = File(getProjectsDirectory(), sanitized)
        if (projectDir.exists()) {
            throw IllegalArgumentException("A project named '$sanitized' already exists.")
        }
        projectDir.mkdirs()
        TemplateManager.populateProject(projectDir, template)
        return projectDir
    }

    fun renameProject(oldName: String, newName: String): File {
        val oldDir = File(getProjectsDirectory(), oldName)
        if (!oldDir.exists() || !oldDir.isDirectory) {
            throw IllegalArgumentException("Project '$oldName' does not exist.")
        }
        val sanitized = sanitizeFileName(newName)
        val newDir = File(getProjectsDirectory(), sanitized)
        if (newDir.exists()) {
            throw IllegalArgumentException("Project '$sanitized' already exists.")
        }
        val success = oldDir.renameTo(newDir)
        if (!success) {
            throw IllegalStateException("Failed to rename project '$oldName' to '$sanitized'.")
        }
        return newDir
    }

    fun duplicateProject(projectName: String): File {
        val source = File(getProjectsDirectory(), projectName)
        if (!source.exists() || !source.isDirectory) {
            throw IllegalArgumentException("Project '$projectName' does not exist.")
        }
        var targetName = "${projectName}_copy"
        var targetDir = File(getProjectsDirectory(), targetName)
        var counter = 1
        while (targetDir.exists()) {
            targetName = "${projectName}_copy_$counter"
            targetDir = File(getProjectsDirectory(), targetName)
            counter++
        }
        targetDir.mkdirs()
        source.copyRecursively(targetDir, overwrite = true)
        return targetDir
    }

    fun deleteProject(projectName: String): Boolean {
        val projectDir = File(getProjectsDirectory(), projectName)
        if (projectDir.exists() && projectDir.isDirectory) {
            return projectDir.deleteRecursively()
        }
        return false
    }

    fun getFileTree(
        directory: File,
        sortOrder: FileSortOrder = FileSortOrder.NAME_ASC,
        showHidden: Boolean = false
    ): FileNode {
        val rawChildren = directory.listFiles()?.filter { showHidden || !it.name.startsWith(".") } ?: emptyList()

        val sortedChildren = rawChildren.sortedWith { a, b ->
            if (a.isDirectory != b.isDirectory) {
                if (a.isDirectory) -1 else 1
            } else {
                when (sortOrder) {
                    FileSortOrder.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                    FileSortOrder.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                    FileSortOrder.DATE_MODIFIED -> b.lastModified().compareTo(a.lastModified())
                    FileSortOrder.SIZE -> b.length().compareTo(a.length())
                    FileSortOrder.TYPE -> a.extension.compareTo(b.extension, ignoreCase = true)
                }
            }
        }

        val childNodes = sortedChildren.map { child ->
            if (child.isDirectory) {
                getFileTree(child, sortOrder, showHidden)
            } else {
                FileNode(
                    name = child.name,
                    path = child.absolutePath,
                    isDirectory = false,
                    sizeBytes = child.length(),
                    lastModified = child.lastModified()
                )
            }
        }

        return FileNode(
            name = directory.name,
            path = directory.absolutePath,
            isDirectory = true,
            sizeBytes = childNodes.sumOf { it.sizeBytes },
            lastModified = directory.lastModified(),
            children = childNodes
        )
    }

    fun readFile(file: File): String {
        return if (file.exists() && file.isFile) {
            file.readText(Charsets.UTF_8)
        } else {
            ""
        }
    }

    fun saveFile(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file ${file.path}", e)
            false
        }
    }

    fun createFile(parentDir: File, name: String, initialContent: String = ""): File {
        val sanitized = sanitizeFileName(name)
        val file = File(parentDir, sanitized)
        if (file.exists()) {
            throw IllegalArgumentException("File '$sanitized' already exists.")
        }
        file.parentFile?.mkdirs()
        file.writeText(initialContent, Charsets.UTF_8)
        return file
    }

    fun createFolder(parentDir: File, name: String): File {
        val sanitized = sanitizeFileName(name)
        val dir = File(parentDir, sanitized)
        if (dir.exists()) {
            throw IllegalArgumentException("Folder '$sanitized' already exists.")
        }
        dir.mkdirs()
        return dir
    }

    fun renameFileOrFolder(target: File, newName: String): File {
        val sanitized = sanitizeFileName(newName)
        val destination = File(target.parentFile ?: target, sanitized)
        if (destination.exists()) {
            throw IllegalArgumentException("An item named '$sanitized' already exists in this folder.")
        }
        val renamed = target.renameTo(destination)
        if (!renamed) {
            throw IllegalStateException("Failed to rename '${target.name}'.")
        }
        return destination
    }

    fun moveFileOrFolder(source: File, destinationDir: File): File {
        if (!destinationDir.exists() || !destinationDir.isDirectory) {
            throw IllegalArgumentException("Destination is not a valid directory.")
        }
        val target = File(destinationDir, source.name)
        if (target.exists()) {
            throw IllegalArgumentException("An item named '${source.name}' already exists in destination.")
        }
        val moved = source.renameTo(target)
        if (!moved) {
            // Fallback copy + delete
            source.copyRecursively(target, overwrite = true)
            source.deleteRecursively()
        }
        return target
    }

    fun copyFileOrFolder(source: File, destinationDir: File): File {
        if (!destinationDir.exists() || !destinationDir.isDirectory) {
            throw IllegalArgumentException("Destination is not a valid directory.")
        }
        var targetName = source.name
        var target = File(destinationDir, targetName)
        var counter = 1
        while (target.exists()) {
            val base = source.nameWithoutExtension
            val ext = if (source.extension.isNotEmpty()) ".${source.extension}" else ""
            targetName = "${base}_copy$counter$ext"
            target = File(destinationDir, targetName)
            counter++
        }
        source.copyRecursively(target, overwrite = true)
        return target
    }

    fun deleteFileOrFolder(target: File): Boolean {
        return if (target.isDirectory) {
            target.deleteRecursively()
        } else {
            target.delete()
        }
    }

    fun getFileDetails(file: File, projectRoot: File? = null): FileDetails {
        val rel = if (projectRoot != null && file.canonicalPath.startsWith(projectRoot.canonicalPath)) {
            file.relativeTo(projectRoot).path
        } else {
            file.name
        }

        val lineCount = if (file.isFile && file.length() < 1024 * 1024) {
            try { file.readLines().size } catch (e: Exception) { 0 }
        } else 0

        val mime = when (file.extension.lowercase()) {
            "kt", "kts" -> "text/x-kotlin"
            "py" -> "text/x-python"
            "js" -> "text/javascript"
            "html" -> "text/html"
            "css" -> "text/css"
            "json" -> "application/json"
            "md" -> "text/markdown"
            "xml" -> "application/xml"
            "sh" -> "application/x-sh"
            else -> if (file.isDirectory) "inode/directory" else "text/plain"
        }

        return FileDetails(
            name = file.name,
            absolutePath = file.absolutePath,
            relativePath = rel,
            sizeBytes = if (file.isDirectory) file.walkTopDown().filter { it.isFile }.sumOf { it.length() } else file.length(),
            lineCount = lineCount,
            lastModified = file.lastModified(),
            isDirectory = file.isDirectory,
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            canExecute = file.canExecute(),
            mimeType = mime
        )
    }

    fun searchProjectFiles(
        projectDir: File,
        query: String,
        matchContent: Boolean = true
    ): List<FileSearchResult> {
        val results = mutableListOf<FileSearchResult>()
        val canonicalProject = projectDir.canonicalFile
        val lowerQuery = query.lowercase().trim()

        if (lowerQuery.isEmpty()) return results

        canonicalProject.walkTopDown()
            .maxDepth(6)
            .filter { it.isFile && !it.name.startsWith(".nova_git") }
            .forEach { file ->
                val relPath = file.relativeTo(canonicalProject).path
                // 1. File name match
                if (file.name.lowercase().contains(lowerQuery)) {
                    results.add(FileSearchResult(file, relPath, null, "File name matches '$query'"))
                } else if (matchContent && file.length() < 512 * 1024) {
                    // 2. Content search (limit to files under 512KB for performance)
                    try {
                        file.useLines { lines ->
                            lines.forEachIndexed { index, line ->
                                if (line.lowercase().contains(lowerQuery)) {
                                    results.add(
                                        FileSearchResult(
                                            file = file,
                                            relativePath = relPath,
                                            lineNumber = index + 1,
                                            matchedContent = line.trim()
                                        )
                                    )
                                }
                            }
                        }
                    } catch (ignored: Exception) {}
                }
            }
        return results.take(100) // bounded result set
    }

    fun exportProjectToZip(projectName: String): File {
        val projectDir = File(getProjectsDirectory(), projectName)
        if (!projectDir.exists() || !projectDir.isDirectory) {
            throw IllegalArgumentException("Project '$projectName' not found.")
        }
        val exportDir = getExportsDirectory()
        val zipFile = File(exportDir, "${projectName}_export_${System.currentTimeMillis()}.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            projectDir.walkTopDown().forEach { file ->
                val relPath = file.relativeTo(projectDir).path
                if (relPath.isNotEmpty()) {
                    if (file.isDirectory) {
                        zos.putNextEntry(ZipEntry("$relPath/"))
                        zos.closeEntry()
                    } else {
                        zos.putNextEntry(ZipEntry(relPath))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
        return zipFile
    }

    /**
     * Imports a project safely from a ZIP InputStream with path traversal (Zip-Slip) protection.
     */
    fun importProjectFromZip(inputStream: InputStream, targetProjectName: String): File {
        val sanitized = sanitizeFileName(targetProjectName)
        val projectDir = File(getProjectsDirectory(), sanitized)
        if (projectDir.exists()) {
            throw IllegalArgumentException("A project named '$sanitized' already exists.")
        }
        projectDir.mkdirs()

        val canonicalDest = projectDir.canonicalFile
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(projectDir, entry.name).canonicalFile
                // Zip-Slip protection check
                if (!newFile.path.startsWith(canonicalDest.path)) {
                    throw SecurityException("Zip entry '${entry.name}' attempts directory traversal.")
                }
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return projectDir
    }

    fun createProjectBackup(projectName: String): File {
        val projectDir = File(getProjectsDirectory(), projectName)
        if (!projectDir.exists() || !projectDir.isDirectory) {
            throw IllegalArgumentException("Project '$projectName' does not exist.")
        }
        val backupsDir = getBackupsDirectory()
        val backupFile = File(backupsDir, "${projectName}_backup_${System.currentTimeMillis()}.zip")

        ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
            projectDir.walkTopDown().forEach { file ->
                val relPath = file.relativeTo(projectDir).path
                if (relPath.isNotEmpty()) {
                    if (file.isDirectory) {
                        zos.putNextEntry(ZipEntry("$relPath/"))
                        zos.closeEntry()
                    } else {
                        zos.putNextEntry(ZipEntry(relPath))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
        return backupFile
    }

    fun restoreProjectFromBackup(backupZipFile: File): File {
        if (!backupZipFile.exists() || !backupZipFile.isFile) {
            throw IllegalArgumentException("Backup file does not exist.")
        }
        val projName = backupZipFile.name.substringBefore("_backup_")
        val restoredDir = File(getProjectsDirectory(), projName)
        if (restoredDir.exists()) {
            restoredDir.deleteRecursively()
        }
        restoredDir.mkdirs()

        FileInputStream(backupZipFile).use { fis ->
            importProjectFromZip(fis, projName)
        }
        return restoredDir
    }

    fun listBackups(): List<File> {
        val dir = getBackupsDirectory()
        return dir.listFiles { f -> f.isFile && f.extension == "zip" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun cleanTempFiles(): Int {
        val dir = getTempDirectory()
        val files = dir.listFiles() ?: return 0
        var count = 0
        files.forEach {
            if (it.deleteRecursively()) count++
        }
        return count
    }

    fun migrateProjects(targetDirectory: File): Int {
        if (!targetDirectory.exists()) targetDirectory.mkdirs()
        val projects = listProjects()
        var migrated = 0
        projects.forEach { proj ->
            val src = File(proj.rootPath)
            val dest = File(targetDirectory, proj.name)
            src.copyRecursively(dest, overwrite = true)
            migrated++
        }
        return migrated
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
