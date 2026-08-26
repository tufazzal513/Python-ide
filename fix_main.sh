cat /app/applet/app/src/main/java/com/example/MainActivity.kt | awk '
/com.example.ui.screens.IdeWorkspaceScreen/ {
    print "                                com.example.ui.screens.IdeWorkspaceScreen("
    print "                                    project = activeProj,"
    print "                                    viewModel = viewModel,"
    print "                                    editorState = editorState,"
    print "                                    fileTree = fileTree,"
    print "                                    processInfo = currentProcess,"
    print "                                    terminalOutputs = outputs"
    print "                                )"
    inside = 1
    next
}
inside == 1 {
    if (/\)/) {
        inside = 0
    }
    next
}
{ print }
' > tmp_main.kt
mv tmp_main.kt /app/applet/app/src/main/java/com/example/MainActivity.kt
