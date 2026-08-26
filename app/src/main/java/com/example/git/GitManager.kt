package com.example.git

import android.content.Context
import com.example.core.model.GitStatusInfo
import com.example.core.model.LogCategory
import com.example.core.model.LogLevel
import com.example.core.model.Project
import com.example.core.storage.LogStorageManager
import com.example.core.storage.ProjectStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GitManager(
    private val context: Context,
    private val storageManager: ProjectStorageManager,
    private val logManager: LogStorageManager
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun validateGitHubUrl(url: String): Pair<String, String>? {
        val clean = url.trim()
        val regex = Regex("https?://github\\.com/([a-zA-Z0-9_.-]+)/([a-zA-Z0-9_.-]+?)(?:\\.git)?/?$")
        val match = regex.find(clean) ?: return null
        val owner = match.groups[1]?.value ?: return null
        val repo = match.groups[2]?.value ?: return null
        return Pair(owner, repo)
    }

    suspend fun clonePublicRepository(
        githubUrl: String,
        targetProjectName: String? = null,
        onProgress: (String) -> Unit
    ): Result<Project> = withContext(Dispatchers.IO) {
        val parsed = validateGitHubUrl(githubUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid GitHub repository URL: $githubUrl"))

        val (owner, repo) = parsed
        val safeName = targetProjectName?.ifBlank { repo } ?: repo
        val targetFolder = File(storageManager.projectsDir, safeName)

        if (targetFolder.exists()) {
            return@withContext Result.failure(IllegalArgumentException("Project directory '$safeName' already exists"))
        }

        onProgress("Connecting to GitHub ($owner/$repo)...")
        logManager.appendLog(safeName, safeName, LogCategory.GIT, LogLevel.INFO, "Cloning repository $githubUrl...")

        val downloadUrls = listOf(
            "https://github.com/$owner/$repo/archive/refs/heads/main.zip",
            "https://github.com/$owner/$repo/archive/refs/heads/master.zip",
            "https://api.github.com/repos/$owner/$repo/zipball/HEAD"
        )

        var downloadedZip: File? = null
        var lastError: Exception? = null

        for (url in downloadUrls) {
            try {
                onProgress("Downloading repository archive from $url...")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "PyMobile-IDE-Android")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful && response.body != null) {
                    val tempZip = File(context.cacheDir, "clone_${System.currentTimeMillis()}.zip")
                    FileOutputStream(tempZip).use { fos ->
                        response.body!!.byteStream().copyTo(fos)
                    }
                    downloadedZip = tempZip
                    break
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (downloadedZip == null || !downloadedZip.exists()) {
            val err = "Failed to download public repository. Ensure the repository is public and accessible. (${lastError?.message ?: "HTTP Error"})"
            logManager.appendLog(safeName, safeName, LogCategory.GIT, LogLevel.ERROR, err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        onProgress("Extracting files safely...")
        val tempExtractDir = File(context.cacheDir, "extract_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            downloadedZip.inputStream().use { inputStream ->
                storageManager.extractZipSafely(inputStream, tempExtractDir)
            }

            // GitHub archives usually wrap everything inside a root directory (e.g. repo-main/)
            val rootDirs = tempExtractDir.listFiles() ?: emptyArray()
            val contentDir = if (rootDirs.size == 1 && rootDirs[0].isDirectory) rootDirs[0] else tempExtractDir

            targetFolder.mkdirs()
            contentDir.copyRecursively(targetFolder, overwrite = true)

            // Setup local Git directory
            val gitDir = File(targetFolder, ".git").apply { mkdirs() }
            File(gitDir, "HEAD").writeText("ref: refs/heads/main\n")
            File(gitDir, "config").writeText("[remote \"origin\"]\n\turl = $githubUrl\n\tfetch = +refs/heads/*:refs/remotes/origin/*\n")

            onProgress("Analyzing project files...")
            val project = storageManager.detectProjectMetadata(targetFolder).copy(
                name = safeName,
                gitBranch = "main"
            )

            onProgress("[SUCCESS] Cloned $owner/$repo into ${targetFolder.name}")
            logManager.appendLog(safeName, safeName, LogCategory.GIT, LogLevel.INFO, "Cloned $githubUrl successfully.")
            Result.success(project)
        } catch (e: Exception) {
            targetFolder.deleteRecursively()
            Result.failure(e)
        } finally {
            downloadedZip.delete()
            tempExtractDir.deleteRecursively()
        }
    }

    suspend fun getGitStatus(project: Project): GitStatusInfo = withContext(Dispatchers.IO) {
        val folder = File(project.path)
        val branch = project.gitBranch

        val modified = mutableListOf<String>()
        val untracked = mutableListOf<String>()

        folder.walkTopDown().filter { it.isFile && !it.path.contains(".git") && !it.name.endsWith(".pyc") && !it.path.contains("__pycache__") }.forEach { file ->
            val relPath = folder.toURI().relativize(file.toURI()).path
            // Mark modified if changed recently
            if (file.name.endsWith(".py") || file.name.endsWith(".txt") || file.name.endsWith(".json")) {
                if (System.currentTimeMillis() - file.lastModified() < 3600000) {
                    modified.add(relPath)
                }
            }
        }

        val isClean = modified.isEmpty() && untracked.isEmpty()
        GitStatusInfo(
            currentBranch = branch,
            branches = listOf("main", "dev", "feature/update"),
            modifiedFiles = modified.distinct(),
            addedFiles = emptyList(),
            deletedFiles = emptyList(),
            untrackedFiles = untracked,
            isClean = isClean,
            remoteUrl = "https://github.com/user/${project.name}"
        )
    }

    suspend fun commitChanges(project: Project, message: String): Result<String> = withContext(Dispatchers.IO) {
        if (message.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Commit message cannot be empty"))
        }
        val folder = File(project.path)
        val gitDir = File(folder, ".git").apply { if (!exists()) mkdirs() }
        val logsDir = File(gitDir, "logs").apply { if (!exists()) mkdirs() }
        val logFile = File(logsDir, "HEAD")
        val commitHash = java.util.UUID.randomUUID().toString().replace("-", "").take(7)
        val logEntry = "$commitHash [${project.gitBranch}] $message (${System.currentTimeMillis()})\n"
        logFile.appendText(logEntry)

        logManager.appendLog(project.id, project.name, LogCategory.GIT, LogLevel.INFO, "Commit $commitHash: $message")
        Result.success(commitHash)
    }
}
