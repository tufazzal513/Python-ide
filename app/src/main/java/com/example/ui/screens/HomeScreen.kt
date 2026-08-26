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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.core.model.ProcessInfo
import com.example.core.model.ProcessStatus
import com.example.core.model.Project
import com.example.core.model.ProjectTemplate
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

@Composable
fun HomeScreen(
    projects: List<Project>,
    runningProcesses: List<ProcessInfo>,
    onSelectProject: (Project) -> Unit,
    onOpenNewProjectDialog: () -> Unit,
    onOpenCloneGitHubDialog: () -> Unit,
    onOpenImportZip: () -> Unit,
    onRunProject: (Project) -> Unit,
    onStopProject: (String) -> Unit,
    onBackupProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else projects.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(IdeAccentBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Py",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text(
                                text = "PyMobile IDE",
                                style = MaterialTheme.typography.titleLarge,
                                color = IdeTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Native Android Python Workspace & Server Runner",
                                style = MaterialTheme.typography.bodySmall,
                                color = IdeTextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Primary Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenNewProjectDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(42.dp).testTag("home_new_project_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Project", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenCloneGitHubDialog,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IdeTextPrimary),
                            modifier = Modifier.weight(1f).height(42.dp).testTag("home_clone_github_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = IdeCyan)
                            Spacer(Modifier.width(4.dp))
                            Text("Clone Git", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Active Processes Banner (if any running)
        val activeRunning = runningProcesses.filter { it.status == ProcessStatus.RUNNING }
        if (activeRunning.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IdeDarkSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, IdeGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(8.dp).background(IdeGreen, RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "Active Running Background Processes (${activeRunning.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = IdeGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        activeRunning.forEach { proc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(IdeDarkHeader, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = proc.projectName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IdeTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${proc.entryPoint} • CPU: ${proc.cpuUsage} • RAM: ${proc.memoryUsage}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IdeTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    if (proc.localUrl != null) {
                                        Text(
                                            text = "Server: ${proc.localUrl}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = IdeCyan,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onStopProject(proc.projectId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = IdeRed),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Stop", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Bar & Projects Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "My Projects (${filteredProjects.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = IdeTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedButton(
                    onClick = onOpenImportZip,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp).testTag("home_import_zip_button")
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(14.dp), tint = IdePythonYellow)
                    Spacer(Modifier.width(4.dp))
                    Text("Import ZIP", fontSize = 11.sp, color = IdeTextPrimary)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search projects...", color = IdeTextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IdeAccentBlue,
                    unfocusedBorderColor = IdeDarkBorder,
                    focusedTextColor = IdeTextPrimary,
                    unfocusedTextColor = IdeTextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("home_search_projects_input")
            )
        }

        // Projects List
        if (filteredProjects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No Python Projects Found",
                            style = MaterialTheme.typography.titleMedium,
                            color = IdeTextSecondary
                        )
                        Text(
                            text = "Tap '+ New Project' or 'Clone Git' to create your first project.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IdeTextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredProjects, key = { it.id }) { project ->
                val isRunning = runningProcesses.any { it.projectId == project.id && it.status == ProcessStatus.RUNNING }
                ProjectCard(
                    project = project,
                    isRunning = isRunning,
                    onOpen = { onSelectProject(project) },
                    onRun = { onRunProject(project) },
                    onStop = { onStopProject(project.id) },
                    onBackup = { onBackupProject(project) },
                    onDelete = { onDeleteProject(project) }
                )
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isRunning: Boolean,
    onOpen: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onBackup: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isRunning) IdeGreen else IdeDarkBorder, RoundedCornerShape(10.dp))
            .clickable { onOpen() }
            .testTag("project_card_${project.name}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val templateIcon = when (project.template) {
                        ProjectTemplate.BASIC_PYTHON -> Icons.Default.Code
                        ProjectTemplate.FLASK_WEB -> Icons.Default.Language
                        ProjectTemplate.FASTAPI_WEB -> Icons.Default.ElectricBolt
                        ProjectTemplate.TELEGRAM_BOT -> Icons.Default.SmartToy
                        ProjectTemplate.AUTOMATION_SCRIPT -> Icons.Default.Schedule
                    }
                    Icon(
                        imageVector = templateIcon,
                        contentDescription = null,
                        tint = if (isRunning) IdeGreen else IdePythonYellow,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Entry: ${project.entryPoint} • Git: ${project.gitBranch}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IdeTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Options Menu (Backup, Delete)
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp).testTag("project_options_${project.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = IdeTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(IdeDarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Backup ZIP", color = IdeTextPrimary) },
                            onClick = {
                                menuExpanded = false
                                onBackup()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Project", color = IdeRed) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Status and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Python Version Badge
                Surface(
                    color = IdeDarkHeader,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(1.dp, IdeDarkBorder, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "Python ${project.pythonVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IdeCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRunning) {
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = IdeRed),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onRun,
                            colors = ButtonDefaults.buttonColors(containerColor = IdeGreen),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp).testTag("project_run_${project.name}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Run", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onOpen,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).testTag("project_open_${project.name}")
                    ) {
                        Text("Open", fontSize = 12.sp, color = IdeAccentBlue)
                    }
                }
            }
        }
    }
}
