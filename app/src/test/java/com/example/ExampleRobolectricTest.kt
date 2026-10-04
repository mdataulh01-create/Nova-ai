package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.storage.StorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nova AI", appName)
    }

    @Test
    fun `storage manager initializes unified root and subfolders`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sm = StorageManager.getInstance(context)
        val root = sm.getRootDirectory()
        assertTrue(root.exists())

        // Check required subdirectories per specification
        listOf("Projects", "Files", "Downloads", "Backups", "Exports", "Temp", "Terminal", "Linux", "Config").forEach { sub ->
            val dir = File(root, sub)
            assertTrue("Subfolder $sub should exist", dir.exists())
        }
    }

    @Test
    fun `storage manager creates and lists projects`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sm = StorageManager.getInstance(context)
        val proj = sm.createProject("ProjectAlpha", "Empty Project")
        assertTrue(proj.exists())

        val list = sm.listProjects()
        assertTrue(list.any { it.name == "ProjectAlpha" })
    }

    @Test
    fun `storage manager renames and duplicates projects`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sm = StorageManager.getInstance(context)
        val orig = sm.createProject("OriginalProj", "Kotlin Console")
        assertTrue(orig.exists())

        val renamed = sm.renameProject("OriginalProj", "RenamedProj")
        assertTrue(renamed.exists())
        assertEquals("RenamedProj", renamed.name)

        val duplicated = sm.duplicateProject("RenamedProj")
        assertTrue(duplicated.exists())
        assertTrue(duplicated.name.startsWith("RenamedProj_copy"))
    }

    @Test
    fun `search in project finds files by name and content`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sm = StorageManager.getInstance(context)
        val proj = sm.createProject("SearchTestProj", "Python Project")
        val mainPy = File(proj, "main.py")
        assertTrue(mainPy.exists())

        // Search by file name
        val nameResults = sm.searchProjectFiles(proj, "main.py")
        assertTrue(nameResults.any { it.file.name == "main.py" })

        // Search by content keyword
        val contentResults = sm.searchProjectFiles(proj, "Python Environment")
        assertTrue(contentResults.any { it.matchedContent?.contains("Python") == true })
    }

    @Test
    fun `export and backup project to zip works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sm = StorageManager.getInstance(context)
        val proj = sm.createProject("ExportProj", "Markdown Notes")
        val exportedZip = sm.exportProjectToZip("ExportProj")
        assertTrue(exportedZip.exists())
        assertTrue(exportedZip.length() > 0)

        val backup = sm.createProjectBackup("ExportProj")
        assertTrue(backup.exists())
        assertTrue(backup.length() > 0)
    }

    @Test
    fun `file details calculates accurate metadata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sm = StorageManager.getInstance(context)
        val proj = sm.createProject("DetailsProj", "Empty Project")
        val testFile = sm.createFile(proj, "sample.kt", "fun main() {\n    println(42)\n}\n")
        val details = sm.getFileDetails(testFile, proj)

        assertEquals("sample.kt", details.name)
        assertEquals("sample.kt", details.relativePath)
        assertEquals(3, details.lineCount)
        assertEquals("text/x-kotlin", details.mimeType)
        assertTrue(details.canRead)
        assertTrue(details.canWrite)
    }
}
