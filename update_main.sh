cat /app/applet/app/src/main/java/com/example/MainActivity.kt | awk '
/NavRoute.Editor ->/ {
    print "                        NavRoute.Workspace -> {"
    print "                            val activeProj = selectedProject ?: projects.firstOrNull()"
    print "                            if (activeProj != null) {"
    print "                                val currentProcess = runningProcesses.firstOrNull { it.projectId == activeProj.id }"
    print "                                val outputs = projectOutputs[activeProj.id] ?: emptyList()"
    print "                                com.example.ui.screens.IdeWorkspaceScreen("
    print "                                    project = activeProj,"
    print "                                    editorState = editorState,"
    print "                                    fileTree = fileTree,"
    print "                                    processInfo = currentProcess,"
    print "                                    terminalOutputs = outputs,"
    print "                                    onFileContentChange = { viewModel.updateFileContent(it) },"
    print "                                    onSaveFile = { viewModel.saveCurrentFile() },"
    print "                                    onOpenFile = { file -> viewModel.openFileInEditor(file) },"
    print "                                    onCreateFile = { dir, name -> viewModel.createFile(dir, name) },"
    print "                                    onCreateDirectory = { dir, name -> viewModel.createDirectory(dir, name) },"
    print "                                    onDeleteFile = { file -> viewModel.deleteFile(file) },"
    print "                                    onRenameFile = { file, newName -> viewModel.renameFile(file, newName) },"
    print "                                    onRunProject = { viewModel.runProject(activeProj) },"
    print "                                    onStopProject = { viewModel.stopProject(activeProj.id) },"
    print "                                    onRestartProject = { viewModel.restartProject(activeProj) },"
    print "                                    onInstallDependencies = { viewModel.installDependencies(activeProj) },"
    print "                                    onClearOutput = { viewModel.processManager.clearOutput(activeProj.id) },"
    print "                                    onExecuteCommand = { cmd -> viewModel.executeTerminalCommand(activeProj, cmd) },"
    print "                                    onGoToLine = { viewModel.goToLine(it) },"
    print "                                    onInsertSymbol = { viewModel.insertSymbol(it) }"
    print "                                )"
    print "                            } else {"
    print "                                EmptyProjectState(\"Select or create a project to open the IDE.\")"
    print "                            }"
    print "                        }"
    skip = 1
    next
}
skip > 0 {
    if (/NavRoute.Dashboard ->/) {
        skip = 0
        print
    }
    next
}
{ print }
' > temp_main.kt
mv temp_main.kt /app/applet/app/src/main/java/com/example/MainActivity.kt
