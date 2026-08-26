package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.sp
import com.example.core.model.LogEntry
import com.example.core.model.ProcessInfo
import com.example.core.model.Project
import com.example.ui.editor.CodeEditorView
import com.example.ui.terminal.TerminalView
import com.example.ui.theme.IdeDarkBackground
import com.example.ui.theme.IdeDarkSurface
import com.example.ui.theme.IdeDarkBorder
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import com.example.ui.editor.CodeEditorUiState
import com.example.core.model.FileNode
import kotlinx.coroutines.launch
import com.example.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeWorkspaceScreen(
    project: Project,
    viewModel: MainViewModel,
    editorState: CodeEditorUiState,
    fileTree: List<FileNode>,
    processInfo: ProcessInfo?,
    terminalOutputs: List<String>,
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    var selectedBottomTabIndex by remember { mutableIntStateOf(0) }
    val bottomTabs = listOf("Terminal", "App Logs", "IDE Logs")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = IdeDarkBackground
            ) {
                FilesScreen(
                    project = project,
                    fileTree = fileTree,
                    onOpenFile = { file -> 
                        viewModel.openFileInEditor(file)
                        scope.launch { drawerState.close() }
                    },
                    onCreateFile = { dir, name -> viewModel.createFile(dir, name) },
                    onCreateDirectory = { dir, name -> viewModel.createDirectory(dir, name) },
                    onDeleteFile = { file -> viewModel.deleteFile(file) },
                    onRenameFile = { file, newName -> viewModel.renameFile(file, newName) }
                )
            }
        },
        gesturesEnabled = true
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f).background(IdeDarkBackground)) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedBottomTabIndex,
                        containerColor = IdeDarkBackground,
                        contentColor = IdeTextPrimary,
                        edgePadding = 0.dp,
                        divider = { Box(Modifier.height(1.dp).background(IdeDarkBorder)) },
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedBottomTabIndex]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        bottomTabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedBottomTabIndex == index,
                                onClick = { selectedBottomTabIndex = index },
                                text = { Text(title, fontSize = 13.sp) }
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedBottomTabIndex) {
                            0 -> {
                                TerminalView(
                                    project = project,
                                    processInfo = processInfo,
                                    outputLines = terminalOutputs,
                                    onRunProject = { viewModel.runProject(project) },
                                    onStopProject = { viewModel.stopProject(project.id) },
                                    onRestartProject = { viewModel.restartProject(project) },
                                    onInstallDependencies = { viewModel.installDependencies(project) },
                                    onClearOutput = { viewModel.processManager.clearOutput(project.id) },
                                    onExecuteCommand = { cmd -> viewModel.executeTerminalCommand(project, cmd) }
                                )
                            }
                            1 -> {
                                LogsScreen(
                                    logs = logs.filter { it.projectName == project.name },
                                    onClearLogs = { viewModel.clearLogs() },
                                    onExportLogs = { viewModel.exportLogs() }
                                )
                            }
                            2 -> {
                                LogsScreen(
                                    logs = logs,
                                    onClearLogs = { viewModel.clearLogs() },
                                    onExportLogs = { viewModel.exportLogs() }
                                )
                            }
                        }
                    }
                }
            },
            sheetPeekHeight = 48.dp, // just enough to show the tab bar or handle
            sheetDragHandle = {
                BottomSheetDefaults.DragHandle(color = IdeTextSecondary)
            },
            containerColor = IdeDarkBackground
        ) { innerPadding ->
            if (editorState.tabs.isEmpty()) {
                // Empty state mimicking Android Code Studio
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Android Code Studio",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = IdeTextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Open the left drawer for files.",
                            fontSize = 16.sp,
                            color = IdeTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = IdeDarkSurface,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Preparing\nSwipe up or click for build output, logs and more.",
                                fontSize = 12.sp,
                                color = IdeTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            } else {
                CodeEditorView(
                    state = editorState,
                    onTabSelect = { viewModel.selectTab(it) },
                    onTabClose = { viewModel.closeTab(it) },
                    onContentChange = { viewModel.onEditorContentChange(it) },
                    onSave = { viewModel.saveActiveFile() },
                    onRun = { viewModel.runProject(project) },
                    onUndo = { viewModel.undoEditor() },
                    onRedo = { viewModel.redoEditor() },
                    onToggleSearch = { viewModel.toggleSearch() },
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onReplaceQueryChange = { viewModel.onReplaceQueryChange(it) },
                    onReplaceOne = { viewModel.replaceOne() },
                    onReplaceAll = { viewModel.replaceAll() },
                    onGoToLine = { viewModel.goToLine(it) },
                    onInsertSymbol = { viewModel.insertSymbol(it) },
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
            }
        }
    }
}
