cat /app/applet/app/src/main/java/com/example/MainActivity.kt | awk '
/object Workspace : NavRoute\("workspace", "IDE", Icons.Default.Code\)/ {
    print "    object Editor : NavRoute(\"editor\", \"Editor\", Icons.Default.Code)"
    print "    object Terminal : NavRoute(\"terminal\", \"Terminal\", Icons.Default.Terminal)"
    print "    object Files : NavRoute(\"files\", \"Files\", Icons.Default.Folder)"
    next
}
/var currentRoute by remember { mutableStateOf<NavRoute>\(NavRoute.Workspace\) }/ {
    print "    var currentRoute by remember { mutableStateOf<NavRoute>(NavRoute.Home) }"
    next
}
/currentRoute == NavRoute.Workspace && editorState.isSearching -> viewModel.toggleSearch\(\)/ {
    print "            currentRoute == NavRoute.Editor && editorState.isSearching -> viewModel.toggleSearch()"
    next
}
/\(currentRoute == NavRoute.Workspace && editorState.isSearching\)/ {
    gsub(/NavRoute.Workspace/, "NavRoute.Editor")
    print
    next
}
/val navItems = listOf\(/ {
    print
    print "        NavRoute.Home,"
    print "        NavRoute.Editor,"
    print "        NavRoute.Terminal,"
    print "        NavRoute.Files,"
    print "        NavRoute.Dashboard,"
    print "        NavRoute.Running,"
    print "        NavRoute.Git,"
    print "        NavRoute.Logs,"
    print "        NavRoute.Settings"
    print "    )"
    skip = 8
    next
}
skip > 0 && /^\s*NavRoute\./ {
    skip--
    next
}
skip > 0 && /^\s*\)/ {
    skip = 0
    next
}
/val phoneNavItems = listOf\(/ {
    print
    print "                            NavRoute.Home,"
    print "                            NavRoute.Editor,"
    print "                            NavRoute.Terminal,"
    print "                            NavRoute.Files,"
    print "                            NavRoute.Running"
    print "                        )"
    skipPhone = 4
    next
}
skipPhone > 0 && /^\s*NavRoute\./ {
    skipPhone--
    next
}
skipPhone > 0 && /^\s*\)/ {
    skipPhone = 0
    next
}
/currentRoute = NavRoute.Workspace/ {
    gsub(/NavRoute.Workspace/, "NavRoute.Editor")
    print
    next
}
/NavRoute.Workspace -> {/ {
    print "                        NavRoute.Editor -> CodeEditorView("
    print "                            state = editorState,"
    print "                            onTabSelect = { viewModel.selectTab(it) },"
    print "                            onTabClose = { viewModel.closeTab(it) },"
    print "                            onContentChange = { viewModel.onEditorContentChange(it) },"
    print "                            onSave = { viewModel.saveActiveFile() },"
    print "                            onRun = { viewModel.runProject(activeProj) },"
    print "                            onUndo = { viewModel.undoEditor() },"
    print "                            onRedo = { viewModel.redoEditor() },"
    print "                            onToggleSearch = { viewModel.toggleSearch() },"
    print "                            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },"
    print "                            onReplaceQueryChange = { viewModel.onReplaceQueryChange(it) },"
    print "                            onReplaceOne = { viewModel.replaceOne() },"
    print "                            onReplaceAll = { viewModel.replaceAll() },"
    print "                            onGoToLine = { viewModel.goToLine(it) },"
    print "                            onInsertSymbol = { viewModel.insertSymbol(it) },"
    print "                            modifier = Modifier.fillMaxSize()"
    print "                        )"
    print "                        NavRoute.Terminal -> {"
    print "                            val activeProj = selectedProject ?: projects.firstOrNull()"
    print "                            if (activeProj != null) {"
    print "                                val currentProcess = runningProcesses.firstOrNull { it.projectId == activeProj.id }"
    print "                                val outputs = projectOutputs[activeProj.id] ?: emptyList()"
    print "                                TerminalView("
    print "                                    project = activeProj,"
    print "                                    processInfo = currentProcess,"
    print "                                    outputLines = outputs,"
    print "                                    onRunProject = { viewModel.runProject(activeProj) },"
    print "                                    onStopProject = { viewModel.stopProject(activeProj.id) },"
    print "                                    onRestartProject = { viewModel.restartProject(activeProj) },"
    print "                                    onInstallDependencies = { viewModel.installDependencies(activeProj) },"
    print "                                    onClearOutput = { viewModel.processManager.clearOutput(activeProj.id) },"
    print "                                    onExecuteCommand = { cmd -> viewModel.executeTerminalCommand(activeProj, cmd) }"
    print "                                )"
    print "                            } else {"
    print "                                EmptyProjectState(\"Select or create a project to use the terminal.\")"
    print "                            }"
    print "                        }"
    print "                        NavRoute.Files -> {"
    print "                            val activeProj = selectedProject ?: projects.firstOrNull()"
    print "                            if (activeProj != null) {"
    print "                                FilesScreen("
    print "                                    project = activeProj,"
    print "                                    fileTree = fileTree,"
    print "                                    onOpenFile = { file -> "
    print "                                        viewModel.openFileInEditor(file)"
    print "                                        currentRoute = NavRoute.Editor"
    print "                                    },"
    print "                                    onCreateFile = { dir, name -> viewModel.createFile(dir, name) },"
    print "                                    onCreateDirectory = { dir, name -> viewModel.createDirectory(dir, name) },"
    print "                                    onDeleteFile = { file -> viewModel.deleteFile(file) },"
    print "                                    onRenameFile = { file, newName -> viewModel.renameFile(file, newName) }"
    print "                                )"
    print "                            } else {"
    print "                                EmptyProjectState(\"Select or create a project to view files.\")"
    print "                            }"
    print "                        }"
    skipWorkspace = 1
    next
}
skipWorkspace == 1 {
    if (/NavRoute.Dashboard ->/) {
        skipWorkspace = 0
        print
    }
    next
}
{ print }
' > tmp_main_revert.kt
mv tmp_main_revert.kt /app/applet/app/src/main/java/com/example/MainActivity.kt
