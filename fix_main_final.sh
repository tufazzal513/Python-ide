awk '
/val outputs = projectOutputs/ {
    print
    print "                                com.example.ui.screens.IdeWorkspaceScreen("
    print "                                    project = activeProj,"
    print "                                    viewModel = viewModel,"
    print "                                    editorState = editorState,"
    print "                                    fileTree = fileTree,"
    print "                                    processInfo = currentProcess,"
    print "                                    terminalOutputs = outputs"
    print "                                )"
    print "                            } else {"
    print "                                EmptyProjectState(\"Select or create a project to open the IDE.\")"
    print "                            }"
    print "                        }"
    skip = 1
    next
}
skip == 1 {
    if (/NavRoute.Dashboard ->/) {
        skip = 0
        print
    }
    next
}
{ print }
' /app/applet/app/src/main/java/com/example/MainActivity.kt > tmp_main_final.kt
mv tmp_main_final.kt /app/applet/app/src/main/java/com/example/MainActivity.kt
