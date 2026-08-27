package com.example.runtime

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.core.model.LogCategory
import com.example.core.model.LogLevel
import com.example.core.model.Project
import com.example.core.storage.LogStorageManager
import com.example.core.storage.ProjectStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

fun interface PythonOutputListener {
    fun onOutput(text: String)
}

fun interface PythonCancelChecker {
    fun isCancelled(): Boolean
}

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
        if (text.isEmpty()) return text
        var masked = text
        for (pattern in secretMaskPatterns) {
            try {
                masked = pattern.replace(masked) { matchResult ->
                    val prefix = matchResult.groups[1]?.value ?: ""
                    val secret = matchResult.groups[2]?.value ?: ""
                    if (secret.length > 6) {
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
            } catch (e: Exception) {
                // Ignore regex errors
            }
        }
        return masked
    }

    private fun ensurePythonStarted(): Python {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        return Python.getInstance()
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
            val err = "Error: Entry point '$entryPoint' not found in ${project.name}"
            onErrorLine(err)
            logManager.appendLog(project.id, project.name, LogCategory.RUN_HISTORY, LogLevel.ERROR, err)
            return@withContext 1
        }

        val header = "[PyMobile Runtime] Starting Python 3.11 for '${project.name}' ($entryPoint)..."
        onOutputLine(header)
        logManager.appendLog(project.id, project.name, LogCategory.RUN_HISTORY, LogLevel.INFO, header)

        try {
            val py = ensurePythonStarted()
            val runtimeModule = py.getModule("pymobile_runtime")

            val stdoutListener = PythonOutputListener { text ->
                val masked = maskSecrets(text)
                onOutputLine(masked)
                logManager.appendLog(project.id, project.name, LogCategory.OUTPUT, LogLevel.INFO, masked)
            }

            val stderrListener = PythonOutputListener { text ->
                val masked = maskSecrets(text)
                onErrorLine(masked)
                logManager.appendLog(project.id, project.name, LogCategory.ERROR, LogLevel.ERROR, masked)
            }

            val cancelChecker = PythonCancelChecker {
                isCancelled.get() || !isActive
            }

            val envMap = mutableMapOf<String, String>()
            envMap["APP_ENV"] = "development"
            envMap["PYTHONUNBUFFERED"] = "1"
            envMap.putAll(project.envVars)

            val exitCodeObj = runtimeModule.callAttr(
                "run_python_file",
                scriptFile.absolutePath,
                projectFolder.absolutePath,
                PyObject.fromJava(stdoutListener),
                PyObject.fromJava(stderrListener),
                PyObject.fromJava(cancelChecker),
                PyObject.fromJava(envMap),
                arguments
            )

            val exitCode = exitCodeObj?.toInt() ?: 0
            if (exitCode == 0) {
                onOutputLine("[PyMobile Runtime] Process completed with exit code 0.")
            } else if (exitCode == 130 || isCancelled.get()) {
                onOutputLine("[PyMobile Runtime] Process terminated by user.")
            } else {
                onErrorLine("[PyMobile Runtime] Process exited with code $exitCode.")
            }
            return@withContext exitCode
        } catch (e: Exception) {
            val errMsg = if (!isCancelled.get()) {
                "Runtime Exception: ${e.message ?: e.toString()}"
            } else {
                "Process stopped."
            }
            onErrorLine(maskSecrets(errMsg))
            logManager.appendLog(project.id, project.name, LogCategory.RUN_HISTORY, LogLevel.ERROR, errMsg)
            return@withContext 1
        }
    }

    suspend fun executeTerminalCommand(
        project: Project,
        command: String,
        onOutput: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val trimmed = command.trim()
        val projectFolder = File(project.path)

        when {
            trimmed.equals("clear", ignoreCase = true) -> {
                // Handled in caller, but safety return
                return@withContext
            }

            trimmed.equals("pip list", ignoreCase = true) -> {
                try {
                    val py = ensurePythonStarted()
                    val runtimeModule = py.getModule("pymobile_runtime")
                    val packagesObj = runtimeModule.callAttr("list_installed_packages", projectFolder.absolutePath)
                    val list = packagesObj?.asList() ?: emptyList()

                    onOutput(String.format("%-25s %s", "Package", "Version"))
                    onOutput("------------------------- -------")
                    for (pkg in list) {
                        val tuple = pkg.asList()
                        if (tuple.size >= 2) {
                            val name = tuple[0].toString()
                            val ver = tuple[1].toString()
                            onOutput(String.format("%-25s %s", name, ver))
                        }
                    }
                    onOutput("")
                } catch (e: Exception) {
                    onOutput("[ERROR] Failed to query pip packages: ${e.message}")
                }
            }

            trimmed.startsWith("python -c ") || trimmed.startsWith("python3 -c ") -> {
                val code = if (trimmed.startsWith("python -c ")) {
                    trimmed.removePrefix("python -c ").trim().trim('"', '\'')
                } else {
                    trimmed.removePrefix("python3 -c ").trim().trim('"', '\'')
                }

                try {
                    val py = ensurePythonStarted()
                    val runtimeModule = py.getModule("pymobile_runtime")
                    val stdoutListener = PythonOutputListener { onOutput(it) }
                    val stderrListener = PythonOutputListener { onOutput("[ERROR] $it") }

                    runtimeModule.callAttr(
                        "eval_python_code",
                        code,
                        projectFolder.absolutePath,
                        PyObject.fromJava(stdoutListener),
                        PyObject.fromJava(stderrListener),
                        PyObject.fromJava(project.envVars)
                    )
                } catch (e: Exception) {
                    onOutput("[ERROR] Python eval error: ${e.message}")
                }
            }

            trimmed.startsWith("python ") || trimmed.startsWith("python3 ") -> {
                val scriptName = if (trimmed.startsWith("python ")) {
                    trimmed.removePrefix("python ").trim()
                } else {
                    trimmed.removePrefix("python3 ").trim()
                }
                val parts = scriptName.split(Regex("\\s+"), limit = 2)
                val targetFile = parts[0]
                val args = if (parts.size > 1) parts[1] else ""

                val scriptFile = if (File(targetFile).isAbsolute) File(targetFile) else File(projectFolder, targetFile)
                if (!scriptFile.exists()) {
                    onOutput("[ERROR] python: can't open file '$targetFile': [Errno 2] No such file or directory")
                    return@withContext
                }

                try {
                    val py = ensurePythonStarted()
                    val runtimeModule = py.getModule("pymobile_runtime")
                    val stdoutListener = PythonOutputListener { onOutput(it) }
                    val stderrListener = PythonOutputListener { onOutput("[ERROR] $it") }
                    val dummyCancel = PythonCancelChecker { false }

                    runtimeModule.callAttr(
                        "run_python_file",
                        scriptFile.absolutePath,
                        projectFolder.absolutePath,
                        PyObject.fromJava(stdoutListener),
                        PyObject.fromJava(stderrListener),
                        PyObject.fromJava(dummyCancel),
                        PyObject.fromJava(project.envVars),
                        args
                    )
                } catch (e: Exception) {
                    onOutput("[ERROR] Execution error: ${e.message}")
                }
            }

            else -> {
                try {
                    val process = Runtime.getRuntime().exec(
                        arrayOf("sh", "-c", command),
                        null,
                        projectFolder
                    )
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        onOutput(line ?: "")
                    }
                    while (errorReader.readLine().also { line = it } != null) {
                        onOutput("[ERROR] " + (line ?: ""))
                    }
                    process.waitFor()
                } catch (e: Exception) {
                    onOutput("[ERROR] Command failed: ${e.message}")
                }
            }
        }
    }
}
