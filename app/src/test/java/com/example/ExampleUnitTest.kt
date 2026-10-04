package com.example

import com.example.core.runtime.CommandSafety
import com.example.core.runtime.TerminalProcessManager
import com.example.core.security.KeystoreHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    @Test
    fun `secret redaction obscures api keys`() {
        val raw = "Bearer AIzaSyABC123456789012345678901234567890 and key sk-1234567890123456789012"
        val redacted = KeystoreHelper.redactSecrets(raw)
        assertFalse(redacted.contains("AIzaSyABC123456789012345678901234567890"))
        assertTrue(redacted.contains("[REDACTED]") || redacted.contains("****"))
    }

    @Test
    fun `terminal evaluator flags dangerous and approval commands`() {
        val mgr = TerminalProcessManager(File("."))
        assertEquals(CommandSafety.SAFE, mgr.evaluateSafety("ls -la"))
        assertEquals(CommandSafety.SAFE, mgr.evaluateSafety("pwd"))
        assertEquals(CommandSafety.REQUIRES_APPROVAL, mgr.evaluateSafety("rm -f myfile.txt"))
        assertEquals(CommandSafety.REQUIRES_APPROVAL, mgr.evaluateSafety("chmod 755 script.sh"))
        assertEquals(CommandSafety.BLOCKED, mgr.evaluateSafety("rm -rf /"))
    }
}
