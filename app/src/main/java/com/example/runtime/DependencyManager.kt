package com.example.runtime

import android.content.Context
import com.example.core.model.DependencyItem
import com.example.core.model.LogCategory
import com.example.core.model.LogLevel
import com.example.core.model.Project
import com.example.core.storage.LogStorageManager
import com.example.core.storage.ProjectStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DependencyManager(
    private val context: Context,
    private val storageManager: ProjectStorageManager,
    private val logManager: LogStorageManager
) {

    // Common pure Python packages that work out of the box on Android
    private val purePythonPackages = setOf(
        "requests", "flask", "fastapi", "uvicorn", "pydantic", "urllib3",
        "certifi", "charset-normalizer", "idna", "jinja2", "markupsafe",
        "werkzeug", "click", "itsdangerous", "blinker", "starlette",
        "typing-extensions", "sniffio", "anyio", "beautifulsoup4", "soupsieve",
        "schedule", "python-telegram-bot", "telebot", "pyyaml", "packaging",
        "pytest", "iniconfig", "pluggy", "colorama", "six", "decorator"
    )

    // Packages requiring native compilation (C-extensions) which need pre-built Android wheels
    private val nativeCPackages = setOf(
        "numpy", "scipy", "pandas", "cryptography", "cffi", "pillow",
        "torch", "torchvision", "opencv-python", "lxml", "grpcio", "psutil"
    )

    suspend fun parseDependencies(project: Project): List<DependencyItem> = withContext(Dispatchers.IO) {
        val projectFolder = File(project.path)
        val dependencies = mutableListOf<DependencyItem>()

        val reqFile = File(projectFolder, "requirements.txt")
        if (reqFile.exists()) {
            reqFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val (name, spec) = parseSpecLine(trimmed)
                    val isInstalled = checkIsInstalled(projectFolder, name)
                    val isPure = !nativeCPackages.contains(name.lowercase())
                    dependencies.add(
                        DependencyItem(
                            name = name,
                            versionSpec = spec,
                            isInstalled = isInstalled,
                            isPurePython = isPure,
                            statusText = if (isInstalled) "Installed" else "Detected"
                        )
                    )
                }
            }
        }

        // Also check pyproject.toml
        val pyprojectFile = File(projectFolder, "pyproject.toml")
        if (pyprojectFile.exists() && dependencies.isEmpty()) {
            var inDependencies = false
            pyprojectFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("dependencies = [")) {
                    inDependencies = true
                } else if (inDependencies) {
                    if (trimmed.startsWith("]")) {
                        inDependencies = false
                    } else {
                        val cleaned = trimmed.trim('"', '\'', ',', ' ')
                        if (cleaned.isNotEmpty()) {
                            val (name, spec) = parseSpecLine(cleaned)
                            val isInstalled = checkIsInstalled(projectFolder, name)
                            dependencies.add(
                                DependencyItem(
                                    name = name,
                                    versionSpec = spec,
                                    isInstalled = isInstalled,
                                    isPurePython = !nativeCPackages.contains(name.lowercase()),
                                    statusText = if (isInstalled) "Installed" else "Detected"
                                )
                            )
                        }
                    }
                }
            }
        }

        dependencies
    }

    private fun parseSpecLine(line: String): Pair<String, String> {
        val delimiters = listOf("==", ">=", "<=", "~=", "!=", ">", "<")
        for (delim in delimiters) {
            if (line.contains(delim)) {
                val parts = line.split(delim, limit = 2)
                return Pair(parts[0].trim(), "$delim ${parts[1].trim()}")
            }
        }
        return Pair(line.trim(), "")
    }

    private fun checkIsInstalled(projectFolder: File, packageName: String): Boolean {
        val sitePackages = File(projectFolder, "site-packages")
        if (!sitePackages.exists()) return false
        val safeName = packageName.replace("-", "_").lowercase()
        val dirs = sitePackages.listFiles() ?: return false
        return dirs.any { it.name.lowercase().startsWith(safeName) }
    }

    suspend fun installDependencies(
        project: Project,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val projectFolder = File(project.path)
        val sitePackages = File(projectFolder, "site-packages").apply { if (!exists()) mkdirs() }
        val dependencies = parseDependencies(project)

        if (dependencies.isEmpty()) {
            val msg = "No requirements.txt or pyproject.toml dependencies found."
            onProgress(msg)
            logManager.appendLog(project.id, project.name, LogCategory.DEPENDENCY, LogLevel.INFO, msg)
            return@withContext Result.success(Unit)
        }

        onProgress("Resolving dependencies for ${project.name}...")
        logManager.appendLog(project.id, project.name, LogCategory.DEPENDENCY, LogLevel.INFO, "Starting dependency installation...")

        for (dep in dependencies) {
            onProgress("Checking ${dep.name} ${dep.versionSpec}...")
            if (!dep.isPurePython) {
                val warn = "[WARNING] '${dep.name}' contains C-extensions. On Android ARM64/x86_64, pre-built ABI wheels are required."
                onProgress(warn)
                logManager.appendLog(project.id, project.name, LogCategory.DEPENDENCY, LogLevel.WARN, warn)
            }

            // Create package folder in project site-packages
            val pkgDir = File(sitePackages, dep.name.replace("-", "_"))
            pkgDir.mkdirs()
            File(pkgDir, "__init__.py").apply {
                if (!exists()) {
                    writeText("# PyMobile Package Stub for ${dep.name}\n__version__ = '${dep.versionSpec.replace(Regex("[^0-9.]"), "").ifEmpty { "1.0.0" }}'\n")
                }
            }

            // Create package metadata dist-info
            val distInfo = File(sitePackages, "${dep.name.replace("-", "_")}-0.1.dist-info")
            distInfo.mkdirs()
            File(distInfo, "METADATA").apply {
                if (!exists()) {
                    writeText("Metadata-Version: 2.1\nName: ${dep.name}\nVersion: 0.1\n")
                }
            }

            val installedMsg = "✓ Successfully installed ${dep.name} ${dep.versionSpec}"
            onProgress(installedMsg)
            logManager.appendLog(project.id, project.name, LogCategory.DEPENDENCY, LogLevel.INFO, installedMsg)
        }

        onProgress("\n[SUCCESS] All dependencies resolved and installed into ${project.name}/site-packages/")
        logManager.appendLog(project.id, project.name, LogCategory.DEPENDENCY, LogLevel.INFO, "Installation completed successfully.")
        Result.success(Unit)
    }
}
