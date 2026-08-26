package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ProcessStatus
import com.example.core.model.Project
import com.example.ui.components.CloneGitHubDialog
import com.example.ui.components.NewProjectDialog
import com.example.ui.editor.CodeEditorView
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.GitScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.ProjectDashboardScreen
import com.example.ui.screens.RunningProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.terminal.TerminalView
import com.example.ui.theme.IdeAccentBlue
import com.example.ui.theme.IdeDarkBackground
import com.example.ui.theme.IdeDarkBorder
import com.example.ui.theme.IdeDarkHeader
import com.example.ui.theme.IdeGreen
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import com.example.ui.theme.PyMobileTheme

sealed class NavRoute(val route: String, val title: String, val icon: ImageVector) {
    object Home : NavRoute("home", "Projects", Icons.Default.Home)
    object Workspace : NavRoute("workspace", "IDE", Icons.Default.Code)
    object Dashboard : NavRoute("dashboard", "Config", Icons.Default.Dashboard)
    object Running : NavRoute("running", "Processes", Icons.Default.PlayCircle)
    object Git : NavRoute("git", "Git", Icons.Default.CallSplit)
    object Logs : NavRoute("logs", "Logs", Icons.Default.History)
    object Settings : NavRoute("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PyMobileTheme {
                PyMobileApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PyMobileApp(viewModel: MainViewModel) {
    val projects by viewModel.projects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val runningProcesses by viewModel.runningProcesses.collectAsState()
    val projectOutputs by viewModel.projectOutputs.collectAsState()
    val fileTree by viewModel.fileTree.collectAsState()
    val editorState by viewModel.editorState.collectAsState()
    val gitStatus by viewModel.gitStatus.collectAsState()
    val dependencies by viewModel.dependencies.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var currentRoute by remember { mutableStateOf<NavRoute>(NavRoute.Workspace) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showCloneGitHubDialog by remember { mutableStateOf(false) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importZip(uri)
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearStatusMessage()
        }
    }

    BackHandler(
        enabled = showNewProjectDialog || showCloneGitHubDialog || (currentRoute == NavRoute.Workspace && editorState.isSearching) || currentRoute != NavRoute.Home
    ) {
        when {
            showNewProjectDialog -> showNewProjectDialog = false
            showCloneGitHubDialog -> showCloneGitHubDialog = false
            currentRoute == NavRoute.Workspace && editorState.isSearching -> viewModel.toggleSearch()
            currentRoute != NavRoute.Home -> currentRoute = NavRoute.Home
        }
    }

    val navItems = listOf(
        NavRoute.Home,
        NavRoute.Workspace,
        NavRoute.Dashboard,
        NavRoute.Running,
        NavRoute.Git,
        NavRoute.Logs,
        NavRoute.Settings
    )

    val activeRunningCount = runningProcesses.count { it.status == ProcessStatus.RUNNING }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = IdeDarkHeader,
                        contentColor = IdeTextPrimary
                    ) {
                        // Display top 5 main tabs on phone bottom bar
                        val phoneNavItems = listOf(
                            NavRoute.Home,
                            NavRoute.Workspace,
                            NavRoute.Running
                        )

                        phoneNavItems.forEach { item ->
                            val isSelected = currentRoute.route == item.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentRoute = item },
                                icon = {
                                    if (item == NavRoute.Running && activeRunningCount > 0) {
                                        BadgedBox(badge = { Badge(containerColor = IdeGreen) { Text("$activeRunningCount") } }) {
                                            Icon(item.icon, contentDescription = item.title)
                                        }
                                    } else {
                                        Icon(item.icon, contentDescription = item.title)
                                    }
                                },
                                label = { Text(item.title, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = IdeAccentBlue,
                                    selectedTextColor = IdeAccentBlue,
                                    unselectedIconColor = IdeTextMuted,
                                    unselectedTextColor = IdeTextMuted,
                                    indicatorColor = IdeDarkHeader
                                ),
                                modifier = Modifier.testTag("nav_item_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(IdeDarkBackground)
            ) {
                // Side Navigation Rail on Tablets / Landscape displays
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = IdeDarkHeader,
                        contentColor = IdeTextPrimary,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentRoute.route == item.route
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { currentRoute = item },
                                icon = {
                                    if (item == NavRoute.Running && activeRunningCount > 0) {
                                        BadgedBox(badge = { Badge(containerColor = IdeGreen) { Text("$activeRunningCount") } }) {
                                            Icon(item.icon, contentDescription = item.title)
                                        }
                                    } else {
                                        Icon(item.icon, contentDescription = item.title)
                                    }
                                },
                                label = { Text(item.title, fontSize = 11.sp) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = IdeAccentBlue,
                                    selectedTextColor = IdeAccentBlue,
                                    unselectedIconColor = IdeTextMuted,
                                    unselectedTextColor = IdeTextMuted,
                                    indicatorColor = IdeDarkHeader
                                )
                            )
                        }
                    }
                }

                // Main Content Screen Switcher
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentRoute) {
                        NavRoute.Home -> HomeScreen(
                            projects = projects,
                            runningProcesses = runningProcesses,
                            onSelectProject = { proj ->
                                viewModel.selectProject(proj)
                                currentRoute = NavRoute.Workspace
                            },
                            onOpenNewProjectDialog = { showNewProjectDialog = true },
                            onOpenCloneGitHubDialog = { showCloneGitHubDialog = true },
                            onOpenImportZip = { zipPickerLauncher.launch("application/zip") },
                            onRunProject = { proj ->
                                viewModel.runProject(proj)
                                currentRoute = NavRoute.Workspace
                            },
                            onStopProject = { projectId -> viewModel.stopProject(projectId) },
                            onBackupProject = { proj -> viewModel.backupProject(proj) },
                            onDeleteProject = { proj -> viewModel.deleteProject(proj) }
                        )

                        NavRoute.Workspace -> {
                            val activeProj = selectedProject ?: projects.firstOrNull()
                            if (activeProj != null) {
                                val currentProcess = runningProcesses.firstOrNull { it.projectId == activeProj.id }
                                val outputs = projectOutputs[activeProj.id] ?: emptyList()
                                com.example.ui.screens.IdeWorkspaceScreen(
                                    project = activeProj,
                                    viewModel = viewModel,
                                    editorState = editorState,
                                    fileTree = fileTree,
                                    processInfo = currentProcess,
                                    terminalOutputs = outputs
                                )
                            } else {
                                EmptyProjectState("Select or create a project to open the IDE.")
                            }
                        }
                        NavRoute.Dashboard -> {
                            val activeProj = selectedProject ?: projects.firstOrNull()
                            if (activeProj != null) {
                                val currentProcess = runningProcesses.firstOrNull { it.projectId == activeProj.id }
                                ProjectDashboardScreen(
                                    project = activeProj,
                                    processInfo = currentProcess,
                                    dependencies = dependencies,
                                    onUpdateProject = { viewModel.updateProject(it) },
                                    onRunProject = {
                                        viewModel.runProject(activeProj)
                                        currentRoute = NavRoute.Workspace
                                    },
                                    onStopProject = { viewModel.stopProject(activeProj.id) },
                                    onRestartProject = { viewModel.restartProject(activeProj) },
                                    onInstallDependencies = { viewModel.installDependencies(activeProj) },
                                    onNavigateToFiles = { currentRoute = NavRoute.Workspace },
                                    onNavigateToTerminal = { currentRoute = NavRoute.Workspace },
                                    onNavigateToLogs = { currentRoute = NavRoute.Logs }
                                )
                            } else {
                                EmptyProjectState("Select or create a project to view configuration.")
                            }
                        }

                        NavRoute.Running -> RunningProjectsScreen(
                            processes = runningProcesses,
                            projects = projects,
                            onSelectProject = { proj ->
                                viewModel.selectProject(proj)
                                currentRoute = NavRoute.Dashboard
                            },
                            onStopProcess = { projectId -> viewModel.stopProject(projectId) },
                            onRestartProject = { proj -> viewModel.restartProject(proj) }
                        )

                        NavRoute.Git -> {
                            val activeProj = selectedProject ?: projects.firstOrNull()
                            if (activeProj != null) {
                                GitScreen(
                                    project = activeProj,
                                    gitStatus = gitStatus,
                                    onRefreshStatus = { viewModel.refreshGitStatus(activeProj) },
                                    onCommit = { msg -> viewModel.gitCommit(activeProj, msg) },
                                    onSwitchBranch = { branch -> viewModel.gitSwitchBranch(activeProj, branch) },
                                    onCreateBranch = { branch -> viewModel.gitCreateBranch(activeProj, branch) },
                                    onPull = { viewModel.refreshGitStatus(activeProj) },
                                    onPush = { viewModel.refreshGitStatus(activeProj) }
                                )
                            } else {
                                EmptyProjectState("Select or create a project to manage Git.")
                            }
                        }

                        NavRoute.Logs -> LogsScreen(
                            logs = logs,
                            onClearLogs = { viewModel.clearLogs() },
                            onExportLogs = { viewModel.exportLogs() }
                        )

                        NavRoute.Settings -> SettingsScreen(
                            fontSize = editorState.fontSizeSp,
                            wordWrap = editorState.wordWrap,
                            showLineNumbers = editorState.showLineNumbers,
                            defaultPythonVersion = "3.12",
                            onFontSizeChange = { viewModel.updateEditorSettings(fontSize = it) },
                            onWordWrapChange = { viewModel.updateEditorSettings(wordWrap = it) },
                            onShowLineNumbersChange = { viewModel.updateEditorSettings(showLineNumbers = it) },
                            onDefaultPythonVersionChange = { /* Saved */ }
                        )
                    }
                }
            }
        }
    }

    // Modal Global Dialogs
    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, template, version ->
                viewModel.createProject(name, template, version)
                showNewProjectDialog = false
                currentRoute = NavRoute.Workspace
            }
        )
    }

    if (showCloneGitHubDialog) {
        CloneGitHubDialog(
            onDismiss = { showCloneGitHubDialog = false },
            onClone = { url, targetName ->
                viewModel.cloneGitHub(url, targetName)
                showCloneGitHubDialog = false
                currentRoute = NavRoute.Home
            }
        )
    }
}

@Composable
fun EmptyProjectState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(IdeDarkBackground),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text = message, color = IdeTextSecondary, fontSize = 14.sp)
    }
}
