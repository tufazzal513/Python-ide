package com.example.core.model

enum class ProjectTemplate(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultEntry: String,
    val iconName: String
) {
    BASIC_PYTHON(
        id = "basic",
        displayName = "Basic Python Script",
        description = "Minimal Python project with main.py and unit tests",
        defaultEntry = "main.py",
        iconName = "code"
    ),
    FLASK_WEB(
        id = "flask",
        displayName = "Flask Web Server",
        description = "Lightweight REST API & Web application with Flask",
        defaultEntry = "app.py",
        iconName = "web"
    ),
    FASTAPI_WEB(
        id = "fastapi",
        displayName = "FastAPI Server",
        description = "Modern, high-performance async API with Uvicorn",
        defaultEntry = "main.py",
        iconName = "bolt"
    ),
    TELEGRAM_BOT(
        id = "telegram_bot",
        displayName = "Telegram Bot",
        description = "Background Telegram bot with long-polling/webhook",
        defaultEntry = "bot.py",
        iconName = "chat"
    ),
    AUTOMATION_SCRIPT(
        id = "automation",
        displayName = "Automation & Scraping",
        description = "Data processing and automation workflow script",
        defaultEntry = "run_tasks.py",
        iconName = "schedule"
    )
}

data class Project(
    val id: String,
    val name: String,
    val path: String,
    val pythonVersion: String = "3.12",
    val entryPoint: String = "main.py",
    val template: ProjectTemplate = ProjectTemplate.BASIC_PYTHON,
    val workingDirectory: String = "",
    val runArguments: String = "",
    val envVars: Map<String, String> = emptyMap(),
    val gitBranch: String = "main",
    val lastRunTime: Long = 0L,
    val lastRunStatus: String = "Not run",
    val isRunning: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
)

enum class ProcessStatus {
    STARTING,
    RUNNING,
    STOPPED,
    FAILED,
    COMPLETED
}

data class ProcessInfo(
    val id: String,
    val projectId: String,
    val projectName: String,
    val entryPoint: String,
    val startTime: Long,
    val status: ProcessStatus,
    val cpuUsage: String = "0%",
    val memoryUsage: String = "0 MB",
    val localPort: Int? = null,
    val localUrl: String? = null,
    val lanUrl: String? = null,
    val exitCode: Int? = null,
    val errorMessage: String? = null,
    val outputLineCount: Int = 0
)

enum class LogCategory {
    RUN_HISTORY,
    OUTPUT,
    ERROR,
    DEPENDENCY,
    GIT,
    SYSTEM
}

enum class LogLevel {
    INFO,
    WARN,
    ERROR,
    DEBUG
}

data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val projectName: String,
    val category: LogCategory,
    val level: LogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DependencyItem(
    val name: String,
    val versionSpec: String = "",
    val isInstalled: Boolean = false,
    val isPurePython: Boolean = true,
    val statusText: String = "Pending"
)

data class GitStatusInfo(
    val currentBranch: String = "main",
    val branches: List<String> = listOf("main"),
    val modifiedFiles: List<String> = emptyList(),
    val addedFiles: List<String> = emptyList(),
    val deletedFiles: List<String> = emptyList(),
    val untrackedFiles: List<String> = emptyList(),
    val isClean: Boolean = true,
    val remoteUrl: String? = null
)

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false
)
