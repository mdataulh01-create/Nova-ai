package com.example.core.storage

import java.io.File

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val children: List<FileNode> = emptyList(),
    val extension: String = if (isDirectory) "" else name.substringAfterLast('.', "")
)

data class ProjectInfo(
    val name: String,
    val rootPath: String,
    val fileCount: Int,
    val totalSizeBytes: Long,
    val lastModified: Long,
    val template: String = "Custom"
)

enum class StorageType {
    APP_PRIVATE,
    SAF_SHARED
}

data class StorageConfig(
    val storageType: StorageType = StorageType.APP_PRIVATE,
    val displayPath: String = "Internal storage/NovaAI (App-Private Sandbox)",
    val isAuthorized: Boolean = true,
    val freeSpaceBytes: Long = 0L,
    val totalSpaceBytes: Long = 0L
)

data class FileSearchResult(
    val file: File,
    val relativePath: String,
    val lineNumber: Int? = null,
    val matchedContent: String? = null
)

data class FileDetails(
    val name: String,
    val absolutePath: String,
    val relativePath: String,
    val sizeBytes: Long,
    val lineCount: Int,
    val lastModified: Long,
    val isDirectory: Boolean,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canExecute: Boolean,
    val mimeType: String
)

enum class FileSortOrder(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DATE_MODIFIED("Date Modified"),
    SIZE("File Size"),
    TYPE("File Type")
}
