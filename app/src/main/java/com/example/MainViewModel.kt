package com.example

import android.app.Application
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.DependencyItem
import com.example.core.model.FileNode
import com.example.core.model.GitStatusInfo
import com.example.core.model.LogCategory
import com.example.core.model.LogEntry
import com.example.core.model.LogLevel
import com.example.core.model.ProcessInfo
import com.example.core.model.Project
import com.example.core.model.ProjectTemplate
import com.example.core.storage.LogStorageManager
import com.example.core.storage.ProjectStorageManager
import com.example.git.GitManager
import com.example.process.ProcessManager
import com.example.runtime.DependencyManager
import com.example.runtime.PythonRuntimeManager
import com.example.ui.editor.CodeEditorUiState
import com.example.ui.editor.EditorTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val storageManager = ProjectStorageManager(context)
    val logManager = LogStorageManager(context, storageManager)
    val runtimeManager = PythonRuntimeManager(context, storageManager, logManager)
    val dependencyManager = DependencyManager(context, storageManager, logManager)
    val gitManager = GitManager(context, storageManager, logManager)
    val processManager = ProcessManager(context, storageManager, logManager, runtimeManager, viewModelScope)

    val runningProcesses: StateFlow<List<ProcessInfo>> = processManager.runningProcesses
    val projectOutputs: StateFlow<Map<String, List<String>>> = processManager.projectOutputs
    val logs: StateFlow<List<LogEntry>> = logManager.logs

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()

    private val _editorState = MutableStateFlow(CodeEditorUiState())
    val editorState: StateFlow<CodeEditorUiState> = _editorState.asStateFlow()

    private val _gitStatus = MutableStateFlow(GitStatusInfo())
    val gitStatus: StateFlow<GitStatusInfo> = _gitStatus.asStateFlow()

    private val _dependencies = MutableStateFlow<List<DependencyItem>>(emptyList())
    val dependencies: StateFlow<List<DependencyItem>> = _dependencies.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadProjects()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun loadProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = storageManager.getProjects()
            _projects.value = list
            if (_selectedProject.value == null && list.isNotEmpty()) {
                selectProject(list[0])
            }
        }
    }

    fun selectProject(project: Project) {
        _selectedProject.value = project
        refreshFileTree(project)
        refreshDependencies(project)
        refreshGitStatus(project)

        // Open entry point file in editor by default if no tabs are open
        val entryFile = File(project.path, project.entryPoint)
        if (entryFile.exists() && _editorState.value.tabs.isEmpty()) {
            openFileInEditor(entryFile)
        }
    }

    fun refreshFileTree(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            val tree = storageManager.getFileTree(File(project.path))
            _fileTree.value = tree
        }
    }

    fun refreshDependencies(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            val deps = dependencyManager.parseDependencies(project)
            _dependencies.value = deps
        }
    }

    fun refreshGitStatus(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = gitManager.getGitStatus(project)
            _gitStatus.value = status
        }
    }

    fun createProject(name: String, template: ProjectTemplate, pythonVersion: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val proj = storageManager.createProject(name, template, pythonVersion)
                logManager.appendLog(proj.id, proj.name, LogCategory.SYSTEM, LogLevel.INFO, "Created project '${proj.name}' with template ${template.displayName}")
                loadProjects()
                selectProject(proj)
                _statusMessage.value = "Created project '${proj.name}'"
            } catch (e: Exception) {
                _statusMessage.value = "Error creating project: ${e.message}"
            }
        }
    }

    fun cloneGitHub(url: String, targetName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _statusMessage.value = "Cloning $url..."
            val result = gitManager.clonePublicRepository(url, targetName) { progress ->
                _statusMessage.value = progress
            }
            result.onSuccess { proj ->
                loadProjects()
                selectProject(proj)
                _statusMessage.value = "Successfully cloned repository!"
            }.onFailure { err ->
                _statusMessage.value = "Clone failed: ${err.message}"
            }
        }
    }

    fun importZip(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val projName = "Imported_${System.currentTimeMillis() % 10000}"
                    val targetDir = File(storageManager.projectsDir, projName).apply { mkdirs() }
                    storageManager.extractZipSafely(inputStream, targetDir)
                    val proj = storageManager.detectProjectMetadata(targetDir).copy(name = projName)
                    loadProjects()
                    selectProject(proj)
                    _statusMessage.value = "Successfully imported ZIP project!"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    fun backupProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupFile = storageManager.backupProject(project)
                _statusMessage.value = "Backup created: ${backupFile.name} (${backupFile.length() / 1024} KB)"
                logManager.appendLog(project.id, project.name, LogCategory.SYSTEM, LogLevel.INFO, "Backup created: ${backupFile.absolutePath}")
            } catch (e: Exception) {
                _statusMessage.value = "Backup failed: ${e.message}"
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            processManager.stopProject(project.id)
            storageManager.deleteFileOrDirectory(File(project.path))
            loadProjects()
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = _projects.value.firstOrNull()
            }
            _statusMessage.value = "Deleted project '${project.name}'"
        }
    }

    fun updateProject(updated: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            // Save .env file if env vars changed
            if (updated.envVars.isNotEmpty()) {
                val envFile = File(updated.path, ".env")
                val envContent = updated.envVars.entries.joinToString("\n") { "${it.key}=${it.value}" }
                storageManager.writeFile(envFile, envContent)
            }
            _selectedProject.value = updated
            loadProjects()
            _statusMessage.value = "Saved configuration for ${updated.name}"
        }
    }

    // Code Editor Operations
    fun openFileInEditor(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = try { storageManager.readFile(file) } catch (e: Exception) { "" }
            val state = _editorState.value
            val existingIndex = state.tabs.indexOfFirst { it.file.absolutePath == file.absolutePath }

            if (existingIndex >= 0) {
                _editorState.value = state.copy(activeTabIndex = existingIndex)
            } else {
                val newTab = EditorTab(
                    file = file,
                    content = TextFieldValue(content, TextRange(0)),
                    undoStack = emptyList(),
                    redoStack = emptyList(),
                    isModified = false
                )
                val newTabs = state.tabs + newTab
                _editorState.value = state.copy(tabs = newTabs, activeTabIndex = newTabs.size - 1)
            }
        }
    }

    fun selectTab(index: Int) {
        if (index in _editorState.value.tabs.indices) {
            _editorState.value = _editorState.value.copy(activeTabIndex = index)
        }
    }

    fun closeTab(index: Int) {
        val state = _editorState.value
        if (index in state.tabs.indices) {
            val newTabs = state.tabs.toMutableList().apply { removeAt(index) }
            val newIndex = (if (state.activeTabIndex >= newTabs.size) newTabs.size - 1 else state.activeTabIndex).coerceAtLeast(0)
            _editorState.value = state.copy(tabs = newTabs, activeTabIndex = newIndex)
        }
    }

    fun onEditorContentChange(newContent: TextFieldValue) {
        val state = _editorState.value
        val tab = state.activeTab ?: return
        val currentContent = tab.content

        val newUndo = if (currentContent.text != newContent.text) {
            (tab.undoStack + currentContent).takeLast(50)
        } else {
            tab.undoStack
        }

        val updatedTab = tab.copy(
            content = newContent,
            undoStack = newUndo,
            redoStack = emptyList(),
            isModified = true
        )

        val updatedTabs = state.tabs.toMutableList().apply { set(state.activeTabIndex, updatedTab) }
        _editorState.value = state.copy(tabs = updatedTabs)
    }

    fun undoEditor() {
        val state = _editorState.value
        val tab = state.activeTab ?: return
        if (tab.undoStack.isEmpty()) return

        val previous = tab.undoStack.last()
        val newUndo = tab.undoStack.dropLast(1)
        val newRedo = tab.redoStack + tab.content

        val updatedTab = tab.copy(
            content = previous,
            undoStack = newUndo,
            redoStack = newRedo,
            isModified = true
        )
        val updatedTabs = state.tabs.toMutableList().apply { set(state.activeTabIndex, updatedTab) }
        _editorState.value = state.copy(tabs = updatedTabs)
    }

    fun redoEditor() {
        val state = _editorState.value
        val tab = state.activeTab ?: return
        if (tab.redoStack.isEmpty()) return

        val next = tab.redoStack.last()
        val newRedo = tab.redoStack.dropLast(1)
        val newUndo = tab.undoStack + tab.content

        val updatedTab = tab.copy(
            content = next,
            undoStack = newUndo,
            redoStack = newRedo,
            isModified = true
        )
        val updatedTabs = state.tabs.toMutableList().apply { set(state.activeTabIndex, updatedTab) }
        _editorState.value = state.copy(tabs = updatedTabs)
    }

    fun saveActiveFile() {
        val state = _editorState.value
        val tab = state.activeTab ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                storageManager.writeFile(tab.file, tab.content.text)
                val updatedTab = tab.copy(isModified = false)
                val updatedTabs = state.tabs.toMutableList().apply { set(state.activeTabIndex, updatedTab) }
                _editorState.value = state.copy(tabs = updatedTabs)
                _statusMessage.value = "Saved ${tab.file.name}"
            } catch (e: Exception) {
                _statusMessage.value = "Error saving file: ${e.message}"
            }
        }
    }

    fun insertSymbol(symbol: String) {
        val state = _editorState.value
        val tab = state.activeTab ?: return
        val text = tab.content.text
        val selection = tab.content.selection

        val newText = text.substring(0, selection.min) + symbol + text.substring(selection.max)
        val newCursor = selection.min + symbol.length
        onEditorContentChange(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun toggleSearch() {
        _editorState.value = _editorState.value.copy(isSearching = !_editorState.value.isSearching)
    }

    fun onSearchQueryChange(query: String) {
        val state = _editorState.value
        val text = state.activeTab?.content?.text ?: ""
        val count = if (query.isNotBlank()) {
            Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(text).count()
        } else 0
        _editorState.value = state.copy(searchQuery = query, searchMatchCount = count)
    }

    fun onReplaceQueryChange(query: String) {
        _editorState.value = _editorState.value.copy(replaceQuery = query)
    }

    fun replaceOne() {
        val state = _editorState.value
        val tab = state.activeTab ?: return
        val text = tab.content.text
        val query = state.searchQuery
        val replacement = state.replaceQuery
        if (query.isBlank()) return

        val index = text.indexOf(query, ignoreCase = true)
        if (index >= 0) {
            val newText = text.substring(0, index) + replacement + text.substring(index + query.length)
            onEditorContentChange(TextFieldValue(newText, TextRange(index + replacement.length)))
            onSearchQueryChange(query)
        }
    }

    fun replaceAll() {
        val state = _editorState.value
        val tab = state.activeTab ?: return
        val text = tab.content.text
        val query = state.searchQuery
        val replacement = state.replaceQuery
        if (query.isBlank()) return

        val newText = text.replace(query, replacement, ignoreCase = true)
        onEditorContentChange(TextFieldValue(newText, TextRange(0)))
        onSearchQueryChange(query)
    }

    fun goToLine(line: Int) {
        val state = _editorState.value
        val tab = state.activeTab ?: return
        val lines = tab.content.text.lines()
        if (line <= lines.size && line > 0) {
            var charOffset = 0
            for (i in 0 until line - 1) {
                charOffset += lines[i].length + 1
            }
            onEditorContentChange(tab.content.copy(selection = TextRange(charOffset)))
        }
    }

    // File Management
    fun createFile(parentDir: File, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newFile = storageManager.createFile(parentDir, name)
                _selectedProject.value?.let { refreshFileTree(it) }
                openFileInEditor(newFile)
                _statusMessage.value = "Created file $name"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun createDirectory(parentDir: File, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                storageManager.createDirectory(parentDir, name)
                _selectedProject.value?.let { refreshFileTree(it) }
                _statusMessage.value = "Created directory $name"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                storageManager.deleteFileOrDirectory(file)
                val tabs = _editorState.value.tabs.filter { it.file.absolutePath != file.absolutePath }
                _editorState.value = _editorState.value.copy(tabs = tabs, activeTabIndex = 0)
                _selectedProject.value?.let { refreshFileTree(it) }
                _statusMessage.value = "Deleted ${file.name}"
            } catch (e: Exception) {
                _statusMessage.value = "Delete error: ${e.message}"
            }
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                storageManager.renameFile(file, newName)
                _selectedProject.value?.let { refreshFileTree(it) }
                _statusMessage.value = "Renamed to $newName"
            } catch (e: Exception) {
                _statusMessage.value = "Rename error: ${e.message}"
            }
        }
    }

    // Process & Execution Controls
    fun runProject(project: Project) {
        saveActiveFile()
        processManager.startProject(project)
        _statusMessage.value = "Running '${project.name}' (${project.entryPoint})..."
    }

    fun stopProject(projectId: String) {
        processManager.stopProject(projectId)
        _statusMessage.value = "Stopped process"
    }

    fun restartProject(project: Project) {
        processManager.restartProject(project)
        _statusMessage.value = "Restarting '${project.name}'..."
    }

    fun installDependencies(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            _statusMessage.value = "Installing dependencies for ${project.name}..."
            val result = dependencyManager.installDependencies(project) { progress ->
                // Progress logging
            }
            result.onSuccess {
                refreshDependencies(project)
                _statusMessage.value = "Dependencies installed successfully!"
            }.onFailure { err ->
                _statusMessage.value = "Installation error: ${err.message}"
            }
        }
    }

    fun executeTerminalCommand(project: Project, command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runtimeManager.executeTerminalCommand(
                project = project,
                command = command,
                onOutput = { line ->
                    processManager.appendTerminalOutput(project.id, line)
                }
            )
        }
    }

    // Git Operations
    fun gitCommit(project: Project, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = gitManager.commitChanges(project, message)
            result.onSuccess { hash ->
                refreshGitStatus(project)
                _statusMessage.value = "Committed ($hash): $message"
            }.onFailure { err ->
                _statusMessage.value = "Commit error: ${err.message}"
            }
        }
    }

    fun gitSwitchBranch(project: Project, branch: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = project.copy(gitBranch = branch)
            _selectedProject.value = updated
            refreshGitStatus(updated)
            _statusMessage.value = "Switched to branch $branch"
        }
    }

    fun gitCreateBranch(project: Project, branch: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = project.copy(gitBranch = branch)
            _selectedProject.value = updated
            refreshGitStatus(updated)
            _statusMessage.value = "Created and switched to branch $branch"
        }
    }

    // Logs & Settings
    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            logManager.clearLogs()
            _statusMessage.value = "Logs cleared"
        }
    }

    fun exportLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val exportFile = File(context.cacheDir, "pymobile_logs_${System.currentTimeMillis()}.txt")
            val text = logManager.logs.value.joinToString("\n") { "[${it.category}] [${it.level}] ${it.projectName}: ${it.message}" }
            exportFile.writeText(text)
            _statusMessage.value = "Exported logs to ${exportFile.name}"
        }
    }

    fun updateEditorSettings(fontSize: Int? = null, wordWrap: Boolean? = null, showLineNumbers: Boolean? = null) {
        val curr = _editorState.value
        _editorState.value = curr.copy(
            fontSizeSp = fontSize ?: curr.fontSizeSp,
            wordWrap = wordWrap ?: curr.wordWrap,
            showLineNumbers = showLineNumbers ?: curr.showLineNumbers
        )
    }
}
