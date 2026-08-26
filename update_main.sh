cat /app/applet/app/src/main/java/com/example/MainActivity.kt | awk '
/terminalOutputs = outputs/ {
    print "                                    terminalOutputs = outputs,"
    print "                                    logs = logs"
    next
}
{ print }
' > tmp_main.kt
mv tmp_main.kt /app/applet/app/src/main/java/com/example/MainActivity.kt
