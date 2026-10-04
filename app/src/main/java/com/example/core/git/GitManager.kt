package com.example.core.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class GitCommit(
    val hash: String,
    val author: String,
    val message: String,
    val timestamp: Long,
    val branch: String
)

data class GitFileStatus(
    val path: String,
    val isStaged: Boolean,
    val isNew: Boolean,
    val isModified: Boolean
)

class GitManager(private val projectDir: File) {

    private val gitMetaFile = File(projectDir, ".nova_git.json")

    init {
        ensureRepo()
    }

    private fun ensureRepo() {
        if (!gitMetaFile.exists()) {
            val initialCommit = GitCommit(
                hash = generateHash("Initial commit ${projectDir.name}"),
                author = "Nova AI Developer",
                message = "Initial project workspace creation",
                timestamp = System.currentTimeMillis(),
                branch = "main"
            )
            saveRepoState("main", listOf("main"), listOf(initialCommit), emptyList())
        }
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(8)
    }

    suspend fun getBranchList(): List<String> = withContext(Dispatchers.IO) {
        val json = readJson()
        val arr = json.optJSONArray("branches") ?: JSONArray()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        if (list.isEmpty()) listOf("main") else list
    }

    suspend fun getCurrentBranch(): String = withContext(Dispatchers.IO) {
        readJson().optString("currentBranch", "main")
    }

    suspend fun getCommitHistory(): List<GitCommit> = withContext(Dispatchers.IO) {
        val json = readJson()
        val arr = json.optJSONArray("commits") ?: JSONArray()
        val list = mutableListOf<GitCommit>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                GitCommit(
                    hash = obj.getString("hash"),
                    author = obj.getString("author"),
                    message = obj.getString("message"),
                    timestamp = obj.getLong("timestamp"),
                    branch = obj.optString("branch", "main")
                )
            )
        }
        list.sortedByDescending { it.timestamp }
    }

    suspend fun getFileStatuses(): List<GitFileStatus> = withContext(Dispatchers.IO) {
        val json = readJson()
        val stagedArr = json.optJSONArray("stagedFiles") ?: JSONArray()
        val stagedSet = mutableSetOf<String>()
        for (i in 0 until stagedArr.length()) {
            stagedSet.add(stagedArr.getString(i))
        }

        val allFiles = projectDir.walkTopDown()
            .filter { it.isFile && !it.name.startsWith(".nova_git") }
            .map { it.relativeTo(projectDir).path }
            .toList()

        allFiles.map { path ->
            GitFileStatus(
                path = path,
                isStaged = stagedSet.contains(path),
                isNew = path.endsWith(".kt") || path.endsWith(".py") || path.endsWith(".html"),
                isModified = true
            )
        }
    }

    suspend fun stageFile(path: String, stage: Boolean) = withContext(Dispatchers.IO) {
        val json = readJson()
        val stagedArr = json.optJSONArray("stagedFiles") ?: JSONArray()
        val currentStaged = mutableListOf<String>()
        for (i in 0 until stagedArr.length()) {
            val item = stagedArr.getString(i)
            if (item != path) currentStaged.add(item)
        }
        if (stage) {
            currentStaged.add(path)
        }
        json.put("stagedFiles", JSONArray(currentStaged))
        gitMetaFile.writeText(json.toString(), Charsets.UTF_8)
    }

    suspend fun createCommit(message: String, author: String = "Nova AI Developer"): GitCommit = withContext(Dispatchers.IO) {
        val json = readJson()
        val currentBranch = json.optString("currentBranch", "main")
        val newCommit = GitCommit(
            hash = generateHash("$message ${System.currentTimeMillis()}"),
            author = author,
            message = message,
            timestamp = System.currentTimeMillis(),
            branch = currentBranch
        )

        val commitsArr = json.optJSONArray("commits") ?: JSONArray()
        val commitObj = JSONObject().apply {
            put("hash", newCommit.hash)
            put("author", newCommit.author)
            put("message", newCommit.message)
            put("timestamp", newCommit.timestamp)
            put("branch", newCommit.branch)
        }
        commitsArr.put(commitObj)
        json.put("commits", commitsArr)
        json.put("stagedFiles", JSONArray()) // clear staging on commit

        gitMetaFile.writeText(json.toString(), Charsets.UTF_8)
        newCommit
    }

    suspend fun createBranch(branchName: String): Boolean = withContext(Dispatchers.IO) {
        val json = readJson()
        val branches = json.optJSONArray("branches") ?: JSONArray()
        for (i in 0 until branches.length()) {
            if (branches.getString(i) == branchName) return@withContext false
        }
        branches.put(branchName)
        json.put("branches", branches)
        json.put("currentBranch", branchName)
        gitMetaFile.writeText(json.toString(), Charsets.UTF_8)
        true
    }

    suspend fun switchBranch(branchName: String): Boolean = withContext(Dispatchers.IO) {
        val json = readJson()
        json.put("currentBranch", branchName)
        gitMetaFile.writeText(json.toString(), Charsets.UTF_8)
        true
    }

    private fun readJson(): JSONObject {
        return if (gitMetaFile.exists()) {
            try {
                JSONObject(gitMetaFile.readText(Charsets.UTF_8))
            } catch (e: Exception) {
                JSONObject()
            }
        } else {
            JSONObject()
        }
    }

    private fun saveRepoState(
        currentBranch: String,
        branches: List<String>,
        commits: List<GitCommit>,
        staged: List<String>
    ) {
        val json = JSONObject()
        json.put("currentBranch", currentBranch)
        json.put("branches", JSONArray(branches))

        val commitsArr = JSONArray()
        commits.forEach { c ->
            val obj = JSONObject().apply {
                put("hash", c.hash)
                put("author", c.author)
                put("message", c.message)
                put("timestamp", c.timestamp)
                put("branch", c.branch)
            }
            commitsArr.put(obj)
        }
        json.put("commits", commitsArr)
        json.put("stagedFiles", JSONArray(staged))

        gitMetaFile.writeText(json.toString(), Charsets.UTF_8)
    }
}
