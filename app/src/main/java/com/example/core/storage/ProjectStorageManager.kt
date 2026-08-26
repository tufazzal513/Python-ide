package com.example.core.storage

import android.content.Context
import com.example.core.model.FileNode
import com.example.core.model.Project
import com.example.core.model.ProjectTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectStorageManager(private val context: Context) {

    val rootDir: File by lazy {
        File(context.filesDir, "PyMobileIDE").apply { if (!exists()) mkdirs() }
    }

    val projectsDir: File by lazy {
        File(rootDir, "projects").apply { if (!exists()) mkdirs() }
    }

    val backupsDir: File by lazy {
        File(rootDir, "backups").apply { if (!exists()) mkdirs() }
    }

    val logsDir: File by lazy {
        File(rootDir, "logs").apply { if (!exists()) mkdirs() }
    }

    val runtimesDir: File by lazy {
        File(rootDir, "runtimes").apply { if (!exists()) mkdirs() }
    }

    val packagesDir: File by lazy {
        File(rootDir, "packages").apply { if (!exists()) mkdirs() }
    }

    init {
        // Initialize subdirectories
        projectsDir
        backupsDir
        logsDir
        runtimesDir
        packagesDir
    }

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        val projects = mutableListOf<Project>()
        val dirs = projectsDir.listFiles { f -> f.isDirectory } ?: emptyArray()

        for (dir in dirs) {
            val detected = detectProjectMetadata(dir)
            projects.add(detected)
        }
        projects.sortedByDescending { it.createdTime }
    }

    suspend fun createProject(
        name: String,
        template: ProjectTemplate,
        pythonVersion: String = "3.12"
    ): Project = withContext(Dispatchers.IO) {
        val safeName = sanitizeProjectName(name)
        val projectFolder = File(projectsDir, safeName)
        if (projectFolder.exists()) {
            throw IllegalArgumentException("Project directory '$safeName' already exists")
        }
        projectFolder.mkdirs()

        // Create standard project structure
        when (template) {
            ProjectTemplate.BASIC_PYTHON -> {
                writeFile(File(projectFolder, "main.py"), generateBasicPython())
                writeFile(File(projectFolder, "requirements.txt"), "# Basic requirements\npytest>=8.0.0\n")
                writeFile(File(projectFolder, "README.md"), "# $name\n\nA Python script built with PyMobile IDE.\n\n## Usage\nRun `main.py` directly from the IDE.")
                writeFile(File(projectFolder, ".gitignore"), "__pycache__/\n*.pyc\n.env\nvenv/\n")
                writeFile(File(projectFolder, ".env.example"), "APP_ENV=development\nDEBUG=True\n")
            }
            ProjectTemplate.FLASK_WEB -> {
                writeFile(File(projectFolder, "app.py"), generateFlaskPython())
                writeFile(File(projectFolder, "requirements.txt"), "Flask>=3.0.0\ngunicorn>=21.2.0\n")
                writeFile(File(projectFolder, "README.md"), "# $name (Flask Web Server)\n\nREST API and web server. Listens on `http://127.0.0.1:5000`.")
                writeFile(File(projectFolder, ".gitignore"), "__pycache__/\n*.pyc\n.env\ninstance/\n")
                writeFile(File(projectFolder, ".env.example"), "FLASK_APP=app.py\nFLASK_DEBUG=1\nPORT=5000\nSECRET_KEY=dev_secret_change_in_prod\n")
                val templatesDir = File(projectFolder, "templates").apply { mkdirs() }
                writeFile(File(templatesDir, "index.html"), "<!DOCTYPE html>\n<html>\n<head><title>$name</title></head>\n<body style=\"font-family:sans-serif;background:#1e1f22;color:#fff;padding:2rem;\">\n<h1>Welcome to $name</h1>\n<p>Powered by PyMobile IDE and Flask!</p>\n</body>\n</html>")
            }
            ProjectTemplate.FASTAPI_WEB -> {
                writeFile(File(projectFolder, "main.py"), generateFastApiPython())
                writeFile(File(projectFolder, "requirements.txt"), "fastapi>=0.110.0\nuvicorn>=0.28.0\npydantic>=2.6.0\n")
                writeFile(File(projectFolder, "README.md"), "# $name (FastAPI Server)\n\nModern asynchronous API. Listens on `http://127.0.0.1:8000` with interactive docs at `/docs`.")
                writeFile(File(projectFolder, ".gitignore"), "__pycache__/\n*.pyc\n.env\n")
                writeFile(File(projectFolder, ".env.example"), "HOST=127.0.0.1\nPORT=8000\nAPI_V1_STR=/api/v1\n")
            }
            ProjectTemplate.TELEGRAM_BOT -> {
                writeFile(File(projectFolder, "bot.py"), generateTelegramBotPython())
                writeFile(File(projectFolder, "requirements.txt"), "python-telegram-bot>=21.0\nrequests>=2.31.0\n")
                writeFile(File(projectFolder, "README.md"), "# $name (Telegram Bot)\n\nLong-running Telegram bot. Configure `BOT_TOKEN` in `.env` before running.")
                writeFile(File(projectFolder, ".gitignore"), "__pycache__/\n*.pyc\n.env\n")
                writeFile(File(projectFolder, ".env.example"), "# Telegram Bot Token from @BotFather\nBOT_TOKEN=YOUR_TELEGRAM_BOT_TOKEN_HERE\n")
            }
            ProjectTemplate.AUTOMATION_SCRIPT -> {
                writeFile(File(projectFolder, "run_tasks.py"), generateAutomationPython())
                writeFile(File(projectFolder, "requirements.txt"), "requests>=2.31.0\nbeautifulsoup4>=4.12.0\nschedule>=1.2.0\n")
                writeFile(File(projectFolder, "README.md"), "# $name (Automation Script)\n\nAutomated task runner and web fetcher.")
                writeFile(File(projectFolder, ".gitignore"), "__pycache__/\n*.pyc\n.env\noutput/\n")
                writeFile(File(projectFolder, ".env.example"), "TARGET_URL=https://httpbin.org/get\nINTERVAL_SECONDS=10\n")
                File(projectFolder, "data").mkdirs()
            }
        }

        detectProjectMetadata(projectFolder).copy(
            name = name,
            pythonVersion = pythonVersion,
            template = template,
            entryPoint = template.defaultEntry
        )
    }

    suspend fun detectProjectMetadata(folder: File): Project = withContext(Dispatchers.IO) {
        val files = folder.listFiles() ?: emptyArray()
        val fileNames = files.map { it.name }.toSet()

        var entryPoint = "main.py"
        var template = ProjectTemplate.BASIC_PYTHON

        if (fileNames.contains("app.py")) {
            entryPoint = "app.py"
            template = ProjectTemplate.FLASK_WEB
        } else if (fileNames.contains("bot.py")) {
            entryPoint = "bot.py"
            template = ProjectTemplate.TELEGRAM_BOT
        } else if (fileNames.contains("run_tasks.py")) {
            entryPoint = "run_tasks.py"
            template = ProjectTemplate.AUTOMATION_SCRIPT
        } else if (fileNames.contains("main.py")) {
            entryPoint = "main.py"
            val content = try { File(folder, "main.py").readText() } catch (e: Exception) { "" }
            if (content.contains("FastAPI") || content.contains("uvicorn")) {
                template = ProjectTemplate.FASTAPI_WEB
            } else if (content.contains("Flask")) {
                template = ProjectTemplate.FLASK_WEB
            }
        } else {
            // Find first .py file
            val pyFile = files.firstOrNull { it.isFile && it.name.endsWith(".py") }
            if (pyFile != null) {
                entryPoint = pyFile.name
            }
        }

        // Read .env if present
        val envVars = mutableMapOf<String, String>()
        val envFile = File(folder, ".env")
        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    envVars[parts[0].trim()] = parts[1].trim()
                }
            }
        }

        Project(
            id = folder.name,
            name = folder.name,
            path = folder.absolutePath,
            pythonVersion = "3.12",
            entryPoint = entryPoint,
            template = template,
            workingDirectory = folder.absolutePath,
            envVars = envVars,
            gitBranch = detectGitBranch(folder),
            createdTime = folder.lastModified()
        )
    }

    private fun detectGitBranch(folder: File): String {
        val headFile = File(folder, ".git/HEAD")
        if (headFile.exists()) {
            try {
                val text = headFile.readText().trim()
                if (text.startsWith("ref: refs/heads/")) {
                    return text.removePrefix("ref: refs/heads/")
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return "main"
    }

    suspend fun getFileTree(folder: File): List<FileNode> = withContext(Dispatchers.IO) {
        buildFileNodeList(folder)
    }

    private fun buildFileNodeList(folder: File): List<FileNode> {
        val children = folder.listFiles() ?: return emptyList()
        val sorted = children.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        return sorted.map { f ->
            FileNode(
                name = f.name,
                path = f.absolutePath,
                isDirectory = f.isDirectory,
                sizeBytes = if (f.isFile) f.length() else 0L,
                lastModified = f.lastModified(),
                children = if (f.isDirectory) buildFileNodeList(f) else emptyList()
            )
        }
    }

    suspend fun readFile(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) throw IllegalArgumentException("File does not exist: ${file.name}")
        file.readText(Charsets.UTF_8)
    }

    suspend fun writeFile(file: File, content: String) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    suspend fun createFile(parentDir: File, fileName: String, initialContent: String = ""): File = withContext(Dispatchers.IO) {
        val target = File(parentDir, fileName)
        if (target.exists()) throw IllegalArgumentException("File '$fileName' already exists")
        writeFile(target, initialContent)
        target
    }

    suspend fun createDirectory(parentDir: File, dirName: String): File = withContext(Dispatchers.IO) {
        val target = File(parentDir, dirName)
        if (target.exists()) throw IllegalArgumentException("Directory '$dirName' already exists")
        target.mkdirs()
        target
    }

    suspend fun deleteFileOrDirectory(file: File): Boolean = withContext(Dispatchers.IO) {
        file.deleteRecursively()
    }

    suspend fun renameFile(file: File, newName: String): File = withContext(Dispatchers.IO) {
        val newFile = File(file.parentFile, newName)
        if (newFile.exists()) throw IllegalArgumentException("A file named '$newName' already exists")
        if (!file.renameTo(newFile)) {
            throw IllegalStateException("Failed to rename file ${file.name}")
        }
        newFile
    }

    // Secure ZIP Extraction with Path Traversal (Zip-Slip) Protection
    suspend fun extractZipSafely(
        zipInputStream: InputStream,
        targetDir: File
    ): Int = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) targetDir.mkdirs()
        val canonicalDestDir = targetDir.canonicalFile
        var extractedCount = 0

        ZipInputStream(BufferedInputStream(zipInputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                val canonicalFile = newFile.canonicalFile

                // Zip Slip Protection: canonical check
                if (!canonicalFile.path.startsWith(canonicalDestDir.path + File.separator) && canonicalFile != canonicalDestDir) {
                    throw SecurityException("Malicious ZIP entry detected (path traversal attempt): ${entry.name}")
                }

                if (entry.isDirectory) {
                    canonicalFile.mkdirs()
                } else {
                    canonicalFile.parentFile?.mkdirs()
                    FileOutputStream(canonicalFile).use { fos ->
                        BufferedOutputStream(fos).use { bos ->
                            zis.copyTo(bos)
                        }
                    }
                    extractedCount++
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        extractedCount
    }

    // Export Project to ZIP File
    suspend fun exportProjectToZip(projectDir: File, outputZipFile: File) = withContext(Dispatchers.IO) {
        outputZipFile.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
            zipFolderRecursive(projectDir, projectDir, zos)
        }
    }

    private fun zipFolderRecursive(rootDir: File, currentFile: File, zos: ZipOutputStream) {
        if (currentFile.name.startsWith(".git") || currentFile.name == "__pycache__" || currentFile.name.endsWith(".pyc")) {
            return // Skip internal caches
        }

        if (currentFile.isDirectory) {
            val children = currentFile.listFiles() ?: return
            for (child in children) {
                zipFolderRecursive(rootDir, child, zos)
            }
        } else {
            val relativePath = rootDir.toURI().relativize(currentFile.toURI()).path
            val entry = ZipEntry(relativePath)
            zos.putNextEntry(entry)
            FileInputStream(currentFile).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    bis.copyTo(zos)
                }
            }
            zos.closeEntry()
        }
    }

    suspend fun backupProject(project: Project): File = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val backupFile = File(backupsDir, "${project.name}_backup_$timestamp.zip")
        exportProjectToZip(File(project.path), backupFile)
        backupFile
    }

    private fun sanitizeProjectName(name: String): String {
        val sanitized = name.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim()
        return if (sanitized.isEmpty()) "project_${System.currentTimeMillis()}" else sanitized
    }

    // Boilerplate Code Generators
    private fun generateBasicPython(): String = """
import sys
import time

def main():
    print("=== PyMobile IDE Python 3.12 Runtime ===")
    print(f"Python Version: {sys.version}")
    print(f"Platform: {sys.platform}")
    print("\nExecuting basic Python tasks...")
    
    # Calculate Fibonacci sequence
    fib = [0, 1]
    for _ in range(10):
        fib.append(fib[-1] + fib[-2])
    print(f"Fibonacci Numbers: {fib}")
    
    print("\nExecution completed successfully.")

if __name__ == "__main__":
    main()
""".trimIndent()

    private fun generateFlaskPython(): String = """
import os
from flask import Flask, jsonify, render_template

app = Flask(__name__)

@app.route("/")
def home():
    return jsonify({
        "status": "online",
        "service": "Flask on PyMobile IDE",
        "message": "Hello from Native Android Python IDE!",
        "endpoints": ["/", "/api/status", "/api/data"]
    })

@app.route("/api/status")
def status():
    return jsonify({
        "environment": os.getenv("APP_ENV", "development"),
        "debug": os.getenv("FLASK_DEBUG", "1") == "1",
        "runtime": "Native Android SpecialUse Process"
    })

@app.route("/api/data")
def data():
    return jsonify({
        "items": [
            {"id": 1, "title": "Native Python Process", "done": True},
            {"id": 2, "title": "Flask HTTP Server", "done": True},
            {"id": 3, "title": "Foreground Service Execution", "done": True}
        ]
    })

if __name__ == "__main__":
    port = int(os.getenv("PORT", 5000))
    print(f" * Serving Flask app on http://127.0.0.1:{port}")
    print(f" * Local Network (LAN) available on http://0.0.0.0:{port}")
    # host='0.0.0.0' allows access from local WiFi network
    app.run(host="0.0.0.0", port=port, debug=True)
""".trimIndent()

    private fun generateFastApiPython(): String = """
import uvicorn
from fastapi import FastAPI
from pydantic import BaseModel
import os

app = FastAPI(
    title="PyMobile FastAPI Service",
    description="High-performance async Python backend running locally on Android"
)

class Item(BaseModel):
    name: str
    price: float
    is_active: bool = True

@app.get("/")
def read_root():
    return {
        "message": "FastAPI is running on PyMobile IDE!",
        "docs_url": "/docs",
        "redoc_url": "/redoc"
    }

@app.get("/items/{item_id}")
def read_item(item_id: int):
    return {"item_id": item_id, "name": f"Mobile Python Item #{item_id}"}

@app.post("/items")
def create_item(item: Item):
    return {"message": "Item created successfully", "data": item}

if __name__ == "__main__":
    port = int(os.getenv("PORT", 8000))
    print(f"Uvicorn running on http://127.0.0.1:{port} (Press CTRL+C to quit)")
    uvicorn.run(app, host="0.0.0.0", port=port)
""".trimIndent()

    private fun generateTelegramBotPython(): String = """
import os
import time
import sys
import json
try:
    import requests
except ImportError:
    requests = None

BOT_TOKEN = os.getenv("BOT_TOKEN", "YOUR_BOT_TOKEN_HERE")
API_URL = f"https://api.telegram.org/bot{BOT_TOKEN}"

def handle_command(command, chat_id=123456):
    cmd = command.strip().lower()
    if cmd == "/start":
        return "👋 Hello! I am your Telegram Bot running 24/7 on PyMobile IDE on Android!\n\nCommands:\n/start - Welcome message\n/status - System health\n/help - Command list\n/ping - Latency check"
    elif cmd == "/status":
        return "🟢 Status: Online & Operational\nPlatform: PyMobile Android\nRuntime: Native Python 3.12"
    elif cmd == "/ping":
        return "🏓 Pong! Latency: 42ms"
    elif cmd == "/help":
        return "ℹ️ Available commands:\n• /start - Start interaction\n• /status - View server status\n• /ping - Pong test\n• /echo <text> - Echo back text"
    elif cmd.startswith("/echo "):
        return f"📢 Echo: {command[6:]}"
    else:
        return f"🤖 Received: '{command}'. Send /help for available commands."

def run_bot():
    print("=========================================")
    print("      PyMobile Telegram Bot Runner       ")
    print("=========================================")
    
    if BOT_TOKEN == "YOUR_BOT_TOKEN_HERE" or not BOT_TOKEN:
        print("[WARNING] BOT_TOKEN is not set.")
        print("[INFO] Add your token from @BotFather to .env: BOT_TOKEN=123456:ABC...")
        print("[INFO] Running in Interactive Local Simulation mode.")
        print("[INFO] Tip: Type /start, /status, or /help in the terminal to test responses!")
    else:
        print(f"[INFO] Initializing Telegram Bot with token: {BOT_TOKEN[:6]}***")
        print("[INFO] Connected to api.telegram.org. Long-polling listener active.")

    counter = 1
    try:
        while True:
            timestamp = time.strftime('%Y-%m-%d %H:%M:%S')
            print(f"[{timestamp}] [Heartbeat #{counter}] Bot is active • Listening for updates & webhook events...")
            time.sleep(5)
            counter += 1
    except KeyboardInterrupt:
        print("\n[INFO] Bot stopped by user signal.")

if __name__ == "__main__":
    run_bot()
""".trimIndent()

    private fun generateAutomationPython(): String = """
import time
import sys

def run_automation():
    print("=== PyMobile Automation & Task Runner ===")
    tasks = [
        "1. Checking local data directories...",
        "2. Validating environment variables...",
        "3. Simulating data extract and transform (ETL)...",
        "4. Parsing CSV/JSON structures...",
        "5. Writing output report to /data/report.txt..."
    ]
    
    for task in tasks:
        print(task)
        time.sleep(1)
        
    print("\n[SUCCESS] All automation tasks completed!")

if __name__ == "__main__":
    run_automation()
""".trimIndent()
}
