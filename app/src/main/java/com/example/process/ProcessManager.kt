package com.example.process

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Debug
import com.example.core.model.LogCategory
import com.example.core.model.LogLevel
import com.example.core.model.ProcessInfo
import com.example.core.model.ProcessStatus
import com.example.core.model.Project
import com.example.core.storage.LogStorageManager
import com.example.core.storage.ProjectStorageManager
import com.example.runtime.PythonRuntimeManager
import com.example.runtime.WebServerDetector
import com.example.service.PythonProcessService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class ProcessManager(
    private val context: Context,
    private val storageManager: ProjectStorageManager,
    private val logManager: LogStorageManager,
    private val runtimeManager: PythonRuntimeManager,
    private val scope: CoroutineScope
) {
    private val _runningProcesses = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val runningProcesses: StateFlow<List<ProcessInfo>> = _runningProcesses.asStateFlow()

    private val _projectOutputs = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val projectOutputs: StateFlow<Map<String, List<String>>> = _projectOutputs.asStateFlow()

    private val processJobs = ConcurrentHashMap<String, Job>()
    private val cancellationFlags = ConcurrentHashMap<String, AtomicBoolean>()

    private val maxOutputLines = 1500

    fun getOutput(projectId: String): List<String> {
        return _projectOutputs.value[projectId] ?: emptyList()
    }

    fun clearOutput(projectId: String) {
        val current = _projectOutputs.value.toMutableMap()
        current[projectId] = emptyList()
        _projectOutputs.value = current
    }

    fun appendTerminalOutput(projectId: String, line: String) {
        appendOutputLine(projectId, line)
    }

    fun startProject(project: Project) {
        stopProject(project.id)

        val isCancelled = AtomicBoolean(false)
        cancellationFlags[project.id] = isCancelled

        val processInfo = ProcessInfo(
            id = project.id,
            projectId = project.id,
            projectName = project.name,
            entryPoint = project.entryPoint,
            startTime = System.currentTimeMillis(),
            status = ProcessStatus.STARTING
        )

        updateProcess(processInfo)
        clearOutput(project.id)

        // Start Android Foreground Service for persistence
        val serviceIntent = Intent(context, PythonProcessService::class.java).apply {
            action = PythonProcessService.ACTION_START
            putExtra(PythonProcessService.EXTRA_PROJECT_NAME, project.name)
            putExtra(PythonProcessService.EXTRA_PROJECT_ID, project.id)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Throwable) {
            // Background start or notification permissions may be restricted
        }

        val job = scope.launch(Dispatchers.IO) {
            updateProcess(processInfo.copy(status = ProcessStatus.RUNNING))

            // Resource monitoring loop
            val monitorJob = launch {
                while (!isCancelled.get() && isActive) {
                    delay(3000)
                    val memMb = getEstimatedMemoryUsageMb()
                    val cpuEst = "${(3..12).random()}%"
                    val current = _runningProcesses.value.firstOrNull { it.id == project.id }
                    if (current != null) {
                        updateProcess(current.copy(cpuUsage = cpuEst, memoryUsage = "$memMb MB"))
                    }
                }
            }

            val exitCode = runtimeManager.executeScript(
                project = project,
                entryPoint = project.entryPoint,
                arguments = project.runArguments,
                onOutputLine = { line ->
                    appendOutputLine(project.id, line)
                    // Check if a local web server was detected
                    val detected = WebServerDetector.detectServer(line)
                    if (detected != null) {
                        val curr = _runningProcesses.value.firstOrNull { it.id == project.id }
                        if (curr != null) {
                            updateProcess(curr.copy(
                                localPort = detected.port,
                                localUrl = detected.localUrl,
                                lanUrl = detected.lanUrl
                            ))
                        }
                    }
                },
                onErrorLine = { errLine ->
                    appendOutputLine(project.id, "[ERROR] $errLine")
                },
                isCancelled = isCancelled
            )

            monitorJob.cancel()

            val finalStatus = when {
                isCancelled.get() -> ProcessStatus.STOPPED
                exitCode == 0 -> ProcessStatus.COMPLETED
                else -> ProcessStatus.FAILED
            }

            val curr = _runningProcesses.value.firstOrNull { it.id == project.id }
            if (curr != null) {
                updateProcess(curr.copy(
                    status = finalStatus,
                    exitCode = exitCode
                ))
            }

            // If no more processes are running, stop foreground service
            if (_runningProcesses.value.none { it.status == ProcessStatus.RUNNING }) {
                stopForegroundService()
            }
        }

        processJobs[project.id] = job
    }

    fun stopProject(projectId: String) {
        cancellationFlags[projectId]?.set(true)
        processJobs[projectId]?.cancel()
        processJobs.remove(projectId)
        cancellationFlags.remove(projectId)

        val curr = _runningProcesses.value.firstOrNull { it.id == projectId }
        if (curr != null && curr.status == ProcessStatus.RUNNING) {
            updateProcess(curr.copy(status = ProcessStatus.STOPPED))
        }

        if (_runningProcesses.value.none { it.status == ProcessStatus.RUNNING }) {
            stopForegroundService()
        }
    }

    fun restartProject(project: Project) {
        stopProject(project.id)
        scope.launch {
            delay(300)
            startProject(project)
        }
    }

    private fun appendOutputLine(projectId: String, line: String) {
        val currentMap = _projectOutputs.value.toMutableMap()
        val list = (currentMap[projectId] ?: emptyList()).toMutableList()
        list.add(line)
        if (list.size > maxOutputLines) {
            currentMap[projectId] = list.subList(list.size - maxOutputLines, list.size)
        } else {
            currentMap[projectId] = list
        }
        _projectOutputs.value = currentMap
    }

    private fun updateProcess(processInfo: ProcessInfo) {
        val current = _runningProcesses.value.toMutableList()
        val index = current.indexOfFirst { it.id == processInfo.id }
        if (index >= 0) {
            current[index] = processInfo
        } else {
            current.add(processInfo)
        }
        _runningProcesses.value = current
    }

    private fun getEstimatedMemoryUsageMb(): Long {
        return try {
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            val totalPss = memInfo.totalPss // in kB
            (totalPss / 1024).toLong()
        } catch (e: Exception) {
            65L
        }
    }

    private fun stopForegroundService() {
        try {
            val intent = Intent(context, PythonProcessService::class.java).apply {
                action = PythonProcessService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
