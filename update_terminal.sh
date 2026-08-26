cat /app/applet/app/src/main/java/com/example/runtime/PythonRuntimeManager.kt | awk '
/if \(command == "pip list"/ {
    print "        val process = Runtime.getRuntime().exec(arrayOf(\"sh\", \"-c\", command), null, java.io.File(project.path))"
    print "        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))"
    print "        val errorReader = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream))"
    print "        var line: String?"
    print "        while (reader.readLine().also { line = it } != null) {"
    print "            onOutput(line ?: \"\")"
    print "        }"
    print "        while (errorReader.readLine().also { line = it } != null) {"
    print "            onOutput(\"[ERROR] \" + (line ?: \"\"))"
    print "        }"
    print "        process.waitFor()"
    skip = 4
    next
}
skip > 0 {
    skip--
    next
}
{ print }
' > temp_runtime.kt
mv temp_runtime.kt /app/applet/app/src/main/java/com/example/runtime/PythonRuntimeManager.kt
