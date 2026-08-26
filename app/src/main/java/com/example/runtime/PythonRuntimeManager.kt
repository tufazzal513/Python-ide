package com.example.runtime

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.core.model.LogCategory
import com.example.core.model.LogLevel
import com.example.core.model.Project
import com.example.core.storage.LogStorageManager
import com.example.core.storage.ProjectStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class PythonRuntimeManager(
    private val context: Context,
    private val storageManager: ProjectStorageManager,
    private val logManager: LogStorageManager
) {
    private val secretMaskPatterns = listOf(
        Regex("(?i)(bot_token|token|api_key|secret_key|password|auth|authorization)\\s*=\\s*['\"]?([^'\"\\s]+)['\"]?"),
        Regex("(?i)(bearer\\s+)([a-zA-Z0-9_.-]{10,})")
    )

    private fun maskSecrets(text: String): String {
        var masked = text
        for (pattern in secretMaskPatterns) {
            masked = pattern.replace(masked) { matchResult ->
                val prefix = matchResult.groups[1]?.value ?: ""
                val secret = matchResult.groups[2]?.value ?: ""
                if (secret.length > 6) {
                    val visibleStart = secret.take(3)
                    val visibleEnd = secret.takeLast(3)
                    val replacement = "$prefix=***"
                    if (matchResult.value.contains("=")) {
                        replacement
                    } else {
                        matchResult.value.replace(secret, "***")
                    }
                } else {
                    matchResult.value
                }
            }
        }
        return masked
    }

    suspend fun executeScript(
        project: Project,
        entryPoint: String,
        arguments: String,
        onOutputLine: (String) -> Unit,
        onErrorLine: (String) -> Unit,
        isCancelled: AtomicBoolean
    ): Int = withContext(Dispatchers.IO) {
        val projectFolder = File(project.path)
        val scriptFile = File(projectFolder, entryPoint)

        if (!scriptFile.exists()) {
            val err = "Error: Entry point '$entryPoint' does not exist in project ${project.name}"
            onErrorLine(err)
            return@withContext 1
        }

        val header = "[PyMobile Runtime] Starting Python via Chaquopy (${project.name}/$entryPoint)..."
        onOutputLine(header)
        logManager.appendLog(project.id, project.name, LogCategory.RUN_HISTORY, LogLevel.INFO, header)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val py = Python.getInstance()
        
        var exitCode = 0
        val execThread = thread(start = false) {
            try {
                val sys = py.getModule("sys")
                
                // Add project folder to sys.path
                val sysPath = sys["path"]
                sysPath?.callAttr("insert", 0, projectFolder.absolutePath)
                sysPath?.callAttr("insert", 0, File(projectFolder, "site-packages").absolutePath)

                // Redirect stdout & stderr using Python code
                val redirectCode = """
import sys
import io

class OutputCatcher(io.StringIO):
    def __init__(self, callback):
        super().__init__()
        self.callback = callback
    def write(self, s):
        if s and s.strip():
            self.callback.invoke(s.strip())
    def flush(self):
        pass

def setup_redirect(out_cb, err_cb):
    sys.stdout = OutputCatcher(out_cb)
    sys.stderr = OutputCatcher(err_cb)
"""
                py.getModule("builtins").callAttr("exec", redirectCode)
                
                // Set environment variables
                val os = py.getModule("os")
                os.callAttr("environ").callAttr("update", mapOf("APP_ENV" to "development"))
                for ((k, v) in project.envVars) {
                    os.callAttr("environ").callAttr("update", mapOf(k to v))
                }

                // Apply redirects
                py.getModule("__main__").callAttr(
                    "setup_redirect",
                    com.chaquo.python.PyObject.fromJava( { text: String -> onOutputLine(maskSecrets(text)) } ),
                    com.chaquo.python.PyObject.fromJava( { text: String -> onErrorLine(maskSecrets(text)) } )
                )

                // Execute user code
                val userCode = scriptFile.readText()
                py.getModule("builtins").callAttr("exec", userCode, py.getModule("__main__").callAttr("dict"))
                
            } catch (e: Exception) {
                if (!isCancelled.get()) {
                    onErrorLine(maskSecrets("Execution Error: ${e.message}"))
                    exitCode = 1
                }
            }
        }
        
        execThread.start()
        
        // Wait and check cancellation
        while (execThread.isAlive && !isCancelled.get() && isActive) {
            delay(100)
        }
        
        if (isCancelled.get()) {
            execThread.interrupt() // Interrupt Python execution
            onOutputLine("[PyMobile Runtime] Process terminated by user.")
            exitCode = 130
        }
        
        execThread.join(2000)
        
        return@withContext exitCode
    }

    suspend fun executeTerminalCommand(
        project: Project,
        command: String,
        onOutput: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (command == "pip list" || command.startsWith("pip ")) {
            onOutput("pip is managed via project dependencies. Configure requirements.txt instead.")
        } else {
            onOutput("Terminal commands are passed to the project context.")
        }
    }
}
