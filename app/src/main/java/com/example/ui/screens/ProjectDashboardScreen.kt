package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DependencyItem
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
import com.example.ui.theme.IdePythonYellow
import com.example.ui.theme.IdeRed
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDashboardScreen(
    project: Project,
    processInfo: ProcessInfo?,
    dependencies: List<DependencyItem>,
    onUpdateProject: (Project) -> Unit,
    onRunProject: () -> Unit,
    onStopProject: () -> Unit,
    onRestartProject: () -> Unit,
    onInstallDependencies: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var entryPoint by remember(project) { mutableStateOf(project.entryPoint) }
    var runArguments by remember(project) { mutableStateOf(project.runArguments) }
    var pythonVersion by remember(project) { mutableStateOf(project.pythonVersion) }
    var versionExpanded by remember { mutableStateOf(false) }

    val envVarsMap = remember(project) {
        mutableStateMapOf<String, String>().apply { putAll(project.envVars) }
    }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }

    val isRunning = processInfo?.status == ProcessStatus.RUNNING

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Project Overview & Control Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isRunning) IdeGreen else IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = IdeTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Path: ${project.path}",
                                style = MaterialTheme.typography.bodySmall,
                                color = IdeTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            color = if (isRunning) IdeGreen.copy(alpha = 0.2f) else IdeDarkHeader,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.border(1.dp, if (isRunning) IdeGreen else IdeDarkBorder, RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = if (isRunning) "🟢 RUNNING" else "⚪ STOPPED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isRunning) IdeGreen else IdeTextSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Run / Stop / Restart Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isRunning) {
                            Button(
                                onClick = onRunProject,
                                colors = ButtonDefaults.buttonColors(containerColor = IdeGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(40.dp).testTag("dash_run_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Run Project", fontSize = 13.sp, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = onStopProject,
                                colors = ButtonDefaults.buttonColors(containerColor = IdeRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(40.dp).testTag("dash_stop_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Stop", fontSize = 13.sp, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = onRestartProject,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = IdeTextPrimary)
                                Spacer(Modifier.width(4.dp))
                                Text("Restart", fontSize = 13.sp, color = IdeTextPrimary)
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateToTerminal,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp), tint = IdeAccentBlue)
                            Spacer(Modifier.width(4.dp))
                            Text("Terminal", fontSize = 13.sp, color = IdeAccentBlue)
                        }
                    }
                }
            }
        }

        // Run Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Run Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = IdeTextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    // Entry point field
                    OutlinedTextField(
                        value = entryPoint,
                        onValueChange = { entryPoint = it },
                        label = { Text("Entry Point (.py file)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IdeAccentBlue,
                            unfocusedBorderColor = IdeDarkBorder,
                            focusedTextColor = IdeTextPrimary,
                            unfocusedTextColor = IdeTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("dash_entry_point_input")
                    )

                    // Python Version Selector
                    ExposedDropdownMenuBox(
                        expanded = versionExpanded,
                        onExpandedChange = { versionExpanded = !versionExpanded }
                    ) {
                        OutlinedTextField(
                            value = "Python $pythonVersion",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Python Version") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IdeAccentBlue,
                                unfocusedBorderColor = IdeDarkBorder,
                                focusedTextColor = IdeTextPrimary,
                                unfocusedTextColor = IdeTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = versionExpanded,
                            onDismissRequest = { versionExpanded = false },
                            modifier = Modifier.background(IdeDarkSurface)
                        ) {
                            listOf("3.11", "3.12", "3.13").forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("Python $v", color = IdeTextPrimary) },
                                    onClick = {
                                        pythonVersion = v
                                        versionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Arguments field
                    OutlinedTextField(
                        value = runArguments,
                        onValueChange = { runArguments = it },
                        label = { Text("Command Line Arguments (Optional)") },
                        placeholder = { Text("--port 8000 --debug") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IdeAccentBlue,
                            unfocusedBorderColor = IdeDarkBorder,
                            focusedTextColor = IdeTextPrimary,
                            unfocusedTextColor = IdeTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val updated = project.copy(
                                entryPoint = entryPoint.trim(),
                                pythonVersion = pythonVersion,
                                runArguments = runArguments.trim(),
                                envVars = envVarsMap.toMap()
                            )
                            onUpdateProject(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End).testTag("dash_save_config_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Configuration")
                    }
                }
            }
        }

        // Dependencies & Packages Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Dependencies (${dependencies.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = onInstallDependencies,
                            colors = ButtonDefaults.buttonColors(containerColor = IdePythonYellow),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp).testTag("dash_install_deps_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Install All", fontSize = 11.sp, color = Color.Black)
                        }
                    }

                    if (dependencies.isEmpty()) {
                        Text(
                            text = "No packages detected in requirements.txt or pyproject.toml",
                            style = MaterialTheme.typography.bodySmall,
                            color = IdeTextMuted
                        )
                    } else {
                        dependencies.forEach { dep ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(IdeDarkSurfaceVariant, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${dep.name} ${dep.versionSpec}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IdeTextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (dep.isPurePython) "Pure Python Package" else "Requires native ABI wheel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (dep.isPurePython) IdeGreen else IdeOrange,
                                        fontSize = 10.sp
                                    )
                                }

                                Surface(
                                    color = if (dep.isInstalled) IdeGreen.copy(alpha = 0.2f) else IdeDarkHeader,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = dep.statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (dep.isInstalled) IdeGreen else IdeTextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Environment Variables (.env) Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Environment Variables (.env)",
                        style = MaterialTheme.typography.titleMedium,
                        color = IdeTextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    envVarsMap.forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(IdeDarkHeader, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$key = $value",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = IdeTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { envVarsMap.remove(key) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete key", tint = IdeRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Quick Presets
                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = IdeTextMuted,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            "BOT_TOKEN" to "",
                            "PORT" to "5000",
                            "DEBUG" to "True",
                            "DATABASE_URL" to "sqlite:///app.db"
                        )

                        presets.forEach { (presetKey, presetVal) ->
                            Surface(
                                color = IdeDarkHeader,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(4.dp))
                                    .clickable {
                                        envVarsMap[presetKey] = presetVal
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+ $presetKey",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = IdeAccentBlue,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }

                    // Add new env var row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newEnvKey,
                            onValueChange = { newEnvKey = it },
                            placeholder = { Text("KEY", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IdeAccentBlue,
                                unfocusedBorderColor = IdeDarkBorder,
                                focusedTextColor = IdeTextPrimary,
                                unfocusedTextColor = IdeTextPrimary
                            ),
                            modifier = Modifier.weight(1f).height(44.dp)
                        )
                        OutlinedTextField(
                            value = newEnvValue,
                            onValueChange = { newEnvValue = it },
                            placeholder = { Text("VALUE", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IdeAccentBlue,
                                unfocusedBorderColor = IdeDarkBorder,
                                focusedTextColor = IdeTextPrimary,
                                unfocusedTextColor = IdeTextPrimary
                            ),
                            modifier = Modifier.weight(1f).height(44.dp)
                        )
                        Button(
                            onClick = {
                                if (newEnvKey.isNotBlank()) {
                                    envVarsMap[newEnvKey.trim()] = newEnvValue.trim()
                                    newEnvKey = ""
                                    newEnvValue = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
