package com.example.core.storage

import android.content.Context
import com.example.core.model.LogCategory
import com.example.core.model.LogEntry
import com.example.core.model.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogStorageManager(
    private val context: Context,
    private val storageManager: ProjectStorageManager
) {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val maxInMemoryLogs = 2000
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun appendLog(
        projectId: String,
        projectName: String,
        category: LogCategory,
        level: LogLevel,
        message: String
    ) {
        val entry = LogEntry(
            projectId = projectId,
            projectName = projectName,
            category = category,
            level = level,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > maxInMemoryLogs) {
            _logs.value = current.subList(0, maxInMemoryLogs)
        } else {
            _logs.value = current
        }

        // Persist to file asynchronously
        try {
            val logDir = File(storageManager.logsDir, projectId).apply { if (!exists()) mkdirs() }
            val logFile = File(logDir, "${category.name.lowercase()}.log")
            val formatted = "[${dateFormat.format(Date(entry.timestamp))}] [${level.name}] ${entry.message}\n"
            logFile.appendText(formatted)
        } catch (e: Exception) {
            // Ignore file logging error
        }
    }

    suspend fun clearLogs(projectId: String? = null) = withContext(Dispatchers.IO) {
        if (projectId == null) {
            _logs.value = emptyList()
            storageManager.logsDir.deleteRecursively()
            storageManager.logsDir.mkdirs()
        } else {
            _logs.value = _logs.value.filter { it.projectId != projectId }
            val logDir = File(storageManager.logsDir, projectId)
            logDir.deleteRecursively()
        }
    }

    suspend fun exportLogs(projectId: String): File = withContext(Dispatchers.IO) {
        val projectLogs = _logs.value.filter { it.projectId == projectId }
        val exportFile = File(storageManager.logsDir, "export_${projectId}_${System.currentTimeMillis()}.txt")
        val content = buildString {
            appendLine("=== PyMobile IDE Logs Export ===")
            appendLine("Project: $projectId")
            appendLine("Exported: ${dateFormat.format(Date())}")
            appendLine("=================================")
            appendLine()
            projectLogs.reversed().forEach { log ->
                appendLine("[${dateFormat.format(Date(log.timestamp))}] [${log.category.name}] [${log.level.name}] ${log.message}")
            }
        }
        exportFile.writeText(content)
        exportFile
    }
}
