package com.example.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ProcessInfo
import com.example.core.model.ProcessStatus
import com.example.core.model.Project
import com.example.ui.theme.IdeAccentBlue
import com.example.ui.theme.IdeCyan
import com.example.ui.theme.IdeDarkBackground
import com.example.ui.theme.IdeDarkBorder
import com.example.ui.theme.IdeDarkHeader
import com.example.ui.theme.IdeDarkSurface
import com.example.ui.theme.IdeDarkSurfaceVariant
import com.example.ui.theme.IdeGreen
import com.example.ui.theme.IdeOrange
import com.example.ui.theme.IdeRed
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import kotlinx.coroutines.launch

@Composable
fun TerminalView(
    project: Project,
    processInfo: ProcessInfo?,
    outputLines: List<String>,
    onRunProject: () -> Unit,
    onStopProject: () -> Unit,
    onRestartProject: () -> Unit,
    onInstallDependencies: () -> Unit,
    onClearOutput: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedModeIndex by remember { mutableIntStateOf(0) } // 0 = Beginner, 1 = Advanced Terminal
    var commandInput by remember { mutableStateOf("") }
    val commandHistory = remember { mutableListOf<String>() }
    var historyIndex by remember { mutableIntStateOf(-1) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(outputLines.size) {
        if (outputLines.isNotEmpty()) {
            listState.animateScrollToItem(outputLines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
    ) {
        // Mode Selector Tab Row
        val safeModeIndex = selectedModeIndex.coerceIn(0, 1)
        TabRow(
            selectedTabIndex = safeModeIndex,
            containerColor = IdeDarkHeader,
            contentColor = IdeTextPrimary,
            divider = { Box(Modifier.height(1.dp).background(IdeDarkBorder)) },
            indicator = { tabPositions ->
                val safePosIndex = safeModeIndex.coerceIn(0, (tabPositions.size - 1).coerceAtLeast(0))
                if (tabPositions.isNotEmpty() && safePosIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[safePosIndex]),
                        color = IdeAccentBlue
                    )
                }
            }
        ) {
            Tab(
                selected = safeModeIndex == 0,
                onClick = { selectedModeIndex = 0 },
                text = { Text("Beginner Controls", fontSize = 13.sp, fontWeight = if (safeModeIndex == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = safeModeIndex == 1,
                onClick = { selectedModeIndex = 1 },
                text = { Text("Advanced Terminal", fontSize = 13.sp, fontWeight = if (safeModeIndex == 1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        if (selectedModeIndex == 0) {
            // Beginner Control Panel
            Surface(
                color = IdeDarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Status Badge & Server Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val status = processInfo?.status ?: ProcessStatus.STOPPED
                        val statusColor = when (status) {
                            ProcessStatus.RUNNING -> IdeGreen
                            ProcessStatus.STARTING -> IdeOrange
                            ProcessStatus.FAILED -> IdeRed
                            else -> IdeTextMuted
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(statusColor, RoundedCornerShape(5.dp))
                            )
                            Text(
                                text = "Status: ${status.name}",
                                style = MaterialTheme.typography.labelLarge,
                                color = statusColor
                            )
                            if (processInfo?.status == ProcessStatus.RUNNING) {
                                Text(
                                    text = "• CPU: ${processInfo.cpuUsage} • RAM: ${processInfo.memoryUsage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IdeTextSecondary
                                )
                            }
                        }

                        // Copy Console Output Button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Terminal Output", outputLines.joinToString("\n"))
                                clipboard.setPrimaryClip(clip)
                            },
                            modifier = Modifier.size(32.dp).testTag("terminal_copy_output")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Output",
                                tint = IdeTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Web Server Card if detected
                    if (processInfo?.localUrl != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = IdeDarkSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Web Server Running on Port ${processInfo.localPort ?: ""}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IdeCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Local: ${processInfo.localUrl}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IdeTextPrimary
                                    )
                                    if (processInfo.lanUrl != null) {
                                        Text(
                                            text = "LAN: ${processInfo.lanUrl}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = IdeGreen
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(processInfo.localUrl))
                                        context.startActivity(browserIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Open", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isRunning = processInfo?.status == ProcessStatus.RUNNING

                        if (!isRunning) {
                            Button(
                                onClick = onRunProject,
                                colors = ButtonDefaults.buttonColors(containerColor = IdeGreen),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("terminal_run_project")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Run Project", fontSize = 12.sp, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = onStopProject,
                                colors = ButtonDefaults.buttonColors(containerColor = IdeRed),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("terminal_stop_project")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Stop", fontSize = 12.sp, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = onRestartProject,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = IdeTextPrimary)
                                Spacer(Modifier.width(4.dp))
                                Text("Restart", fontSize = 12.sp, color = IdeTextPrimary)
                            }
                        }

                        OutlinedButton(
                            onClick = onInstallDependencies,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1.2f).height(38.dp).testTag("terminal_install_dependencies")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = IdeAccentBlue)
                            Spacer(Modifier.width(4.dp))
                            Text("Install Deps", fontSize = 12.sp, color = IdeAccentBlue)
                        }

                        IconButton(
                            onClick = onClearOutput,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = IdeTextSecondary)
                        }
                    }
                }
            }
        }

        // Console Output Area (ANSI / Log Streaming)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(IdeDarkBackground)
                .padding(8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (outputLines.isEmpty()) {
                    item {
                        Text(
                            text = "Terminal ready. Press 'Run Project' or type a command below.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = IdeTextMuted
                        )
                    }
                } else {
                    items(outputLines) { line ->
                        val lineColor = when {
                            line.startsWith("[ERROR]") || line.contains("Traceback") || line.contains("Error:") -> IdeRed
                            line.startsWith("[WARNING]") || line.startsWith(" *") -> IdeOrange
                            line.startsWith("[SUCCESS]") || line.contains("✓") -> IdeGreen
                            line.startsWith("[INFO]") || line.startsWith("[PyMobile") -> IdeCyan
                            else -> IdeTextPrimary
                        }

                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = lineColor
                        )
                    }
                }
            }
        }

        // Quick Action Chips Toolbar (Visible in both Beginner & Advanced modes)
        val isBotProject = remember(project.entryPoint, project.name) {
            project.name.contains("bot", ignoreCase = true) || project.entryPoint.contains("bot", ignoreCase = true)
        }

        Surface(
            color = IdeDarkSurface,
            modifier = Modifier.fillMaxWidth().border(1.dp, IdeDarkBorder)
        ) {
            val quickCommands = if (isBotProject) {
                listOf(
                    "/start" to "/start",
                    "/status" to "/status",
                    "/ping" to "/ping",
                    "/help" to "/help",
                    "pip list" to "pip list",
                    "pip install requests" to "pip install requests",
                    "env" to "env",
                    "clear" to "clear"
                )
            } else {
                listOf(
                    "python ${project.entryPoint}" to "python ${project.entryPoint}",
                    "pip list" to "pip list",
                    "pip install requests" to "pip install requests",
                    "ls" to "ls",
                    "env" to "env",
                    "pwd" to "pwd",
                    "clear" to "clear"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick:",
                    style = MaterialTheme.typography.labelSmall,
                    color = IdeTextMuted,
                    fontSize = 11.sp
                )
                quickCommands.forEach { (label, cmd) ->
                    Box(
                        modifier = Modifier
                            .background(IdeDarkHeader, RoundedCornerShape(4.dp))
                            .border(1.dp, IdeDarkBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                onExecuteCommand(cmd)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (cmd.startsWith("/")) IdeCyan else IdeAccentBlue
                        )
                    }
                }
            }
        }

        // Advanced Mode Command Line Prompt
        if (selectedModeIndex == 1) {
            Surface(
                color = IdeDarkHeader,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${project.name}$ ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IdeGreen
                    )

                    BasicTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = IdeTextPrimary
                        ),
                        cursorBrush = SolidColor(IdeAccentBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commandInput.isNotBlank()) {
                                    val cmd = commandInput.trim()
                                    commandHistory.add(cmd)
                                    historyIndex = commandHistory.size
                                    onExecuteCommand(cmd)
                                    commandInput = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("terminal_command_input")
                    )

                    Button(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                val cmd = commandInput.trim()
                                commandHistory.add(cmd)
                                historyIndex = commandHistory.size
                                onExecuteCommand(cmd)
                                commandInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(28.dp).testTag("terminal_send_command")
                    ) {
                        Text("Send", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
