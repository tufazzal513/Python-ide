package com.example.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ProcessInfo
import com.example.core.model.ProcessStatus
import com.example.core.model.Project
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
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Termux colors
    val termuxBackground = Color.Black
    val termuxText = Color(0xFF00FF00) // Classic terminal green
    val termuxWhite = Color(0xFFE0E0E0)
    
    LaunchedEffect(outputLines.size) {
        if (outputLines.isNotEmpty()) {
            listState.animateScrollToItem(outputLines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(termuxBackground)
    ) {
        // Output Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (outputLines.isEmpty()) {
                    item {
                        Text(
                            text = "Welcome to Termux-style console.\nType a command or use quick actions below.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = termuxWhite
                        )
                    }
                } else {
                    items(outputLines) { line ->
                        val lineColor = when {
                            line.startsWith("[ERROR]") || line.contains("Error:") -> Color(0xFFFF5555)
                            line.startsWith("[WARNING]") -> Color(0xFFF1FA8C)
                            line.startsWith("[SUCCESS]") -> Color(0xFF50FA7B)
                            line.startsWith("[INFO]") -> Color(0xFF8BE9FD)
                            else -> termuxWhite
                        }

                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = lineColor
                        )
                    }
                }
            }
            
            // Floating Stop Button if running
            if (processInfo?.status == ProcessStatus.RUNNING) {
                IconButton(
                    onClick = onStopProject,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0x88FF5555), shape = androidx.compose.foundation.shape.CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Extra Keys / Quick Actions Row (Termux style)
        val quickCommands = listOf(
            "CTRL+C" to "STOP",
            "RUN" to "RUN",
            "CLEAR" to "clear",
            "PIP LIST" to "pip list",
            "/start" to "/start",
            "/help" to "/help"
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickCommands.forEach { (label, cmd) ->
                Box(
                    modifier = Modifier
                        .clickable {
                            if (cmd == "STOP") {
                                onStopProject()
                            } else if (cmd == "RUN") {
                                onRunProject()
                            } else {
                                onExecuteCommand(cmd)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = termuxWhite
                    )
                }
            }
        }

        // Command Prompt Input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(termuxBackground)
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Text(
                text = "~ $ ",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = termuxText
            )

            BasicTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = termuxWhite
                ),
                cursorBrush = SolidColor(termuxText),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandInput.isNotBlank()) {
                            onExecuteCommand(commandInput.trim())
                            commandInput = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            
            if (commandInput.isNotBlank()) {
                Text(
                    text = "ENTER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = termuxText,
                    modifier = Modifier
                        .clickable {
                            onExecuteCommand(commandInput.trim())
                            commandInput = ""
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
