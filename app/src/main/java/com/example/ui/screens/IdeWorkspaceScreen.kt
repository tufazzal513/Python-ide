package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.model.ProcessInfo
import com.example.core.model.Project
import com.example.ui.editor.CodeEditorView
import com.example.ui.terminal.TerminalView
import com.example.ui.theme.IdeDarkBackground
import java.io.File
import com.example.ui.editor.CodeEditorUiState
import com.example.core.model.FileNode
import androidx.compose.ui.text.input.TextFieldValue
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
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

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
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f)) {
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
            },
            sheetPeekHeight = 60.dp,
            sheetDragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) { innerPadding ->
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
