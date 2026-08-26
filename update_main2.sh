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
    skip = 15
    next
}
skip > 0 {
    skip--
    next
}
{ print }
' > temp_main2.kt
mv temp_main2.kt /app/applet/app/src/main/java/com/example/MainActivity.kt
