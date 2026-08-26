package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ProjectTemplate
import com.example.ui.theme.IdeAccentBlue
import com.example.ui.theme.IdeDarkBackground
import com.example.ui.theme.IdeDarkBorder
import com.example.ui.theme.IdeDarkSurface
import com.example.ui.theme.IdeDarkSurfaceVariant
import com.example.ui.theme.IdeGreen
import com.example.ui.theme.IdePythonYellow
import com.example.ui.theme.IdeRed
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, template: ProjectTemplate, pythonVersion: String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(ProjectTemplate.BASIC_PYTHON) }
    var selectedVersion by remember { mutableStateOf("3.12") }
    var versionExpanded by remember { mutableStateOf(false) }

    val pythonVersions = listOf("3.11", "3.12", "3.13")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Python Project", style = MaterialTheme.typography.titleLarge, color = IdeTextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g. MyFastApiServer") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IdeAccentBlue,
                        unfocusedBorderColor = IdeDarkBorder,
                        focusedTextColor = IdeTextPrimary,
                        unfocusedTextColor = IdeTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_project_name_input")
                )

                // Python Version Selector
                ExposedDropdownMenuBox(
                    expanded = versionExpanded,
                    onExpandedChange = { versionExpanded = !versionExpanded }
                ) {
                    OutlinedTextField(
                        value = "Python $selectedVersion",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Python Runtime") },
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
                        pythonVersions.forEach { version ->
                            DropdownMenuItem(
                                text = { Text("Python $version", color = IdeTextPrimary) },
                                onClick = {
                                    selectedVersion = version
                                    versionExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Select Template:",
                    style = MaterialTheme.typography.labelMedium,
                    color = IdeTextSecondary
                )

                ProjectTemplate.entries.forEach { template ->
                    val isSelected = selectedTemplate == template
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) IdeDarkSurfaceVariant else IdeDarkBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) IdeAccentBlue else IdeDarkBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTemplate = template }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val icon = when (template) {
                                ProjectTemplate.BASIC_PYTHON -> Icons.Default.Code
                                ProjectTemplate.FLASK_WEB -> Icons.Default.Language
                                ProjectTemplate.FASTAPI_WEB -> Icons.Default.ElectricBolt
                                ProjectTemplate.TELEGRAM_BOT -> Icons.Default.SmartToy
                                ProjectTemplate.AUTOMATION_SCRIPT -> Icons.Default.Schedule
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) IdeAccentBlue else IdePythonYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = template.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) IdeAccentBlue else IdeTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IdeTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isNotBlank()) {
                        onCreate(projectName.trim(), selectedTemplate, selectedVersion)
                    }
                },
                enabled = projectName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                modifier = Modifier.testTag("dialog_confirm_create_project")
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IdeTextSecondary)
            }
        },
        containerColor = IdeDarkSurface
    )
}

@Composable
fun CloneGitHubDialog(
    onDismiss: () -> Unit,
    onClone: (url: String, targetName: String) -> Unit
) {
    var repoUrl by remember { mutableStateOf("") }
    var targetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clone Public GitHub Repository", color = IdeTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the URL of any public GitHub repository. No credentials required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = IdeTextSecondary
                )
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = {
                        repoUrl = it
                        if (targetName.isBlank() && it.contains("/")) {
                            val repo = it.trimEnd('/').substringAfterLast('/')
                            if (repo.isNotBlank()) targetName = repo.removeSuffix(".git")
                        }
                    },
                    label = { Text("GitHub URL") },
                    placeholder = { Text("https://github.com/psf/requests") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IdeAccentBlue,
                        unfocusedBorderColor = IdeDarkBorder,
                        focusedTextColor = IdeTextPrimary,
                        unfocusedTextColor = IdeTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_clone_url_input")
                )

                OutlinedTextField(
                    value = targetName,
                    onValueChange = { targetName = it },
                    label = { Text("Local Project Name (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IdeAccentBlue,
                        unfocusedBorderColor = IdeDarkBorder,
                        focusedTextColor = IdeTextPrimary,
                        unfocusedTextColor = IdeTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (repoUrl.isNotBlank()) {
                        onClone(repoUrl.trim(), targetName.trim())
                    }
                },
                enabled = repoUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                modifier = Modifier.testTag("dialog_confirm_clone_button")
            ) {
                Text("Clone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IdeTextSecondary)
            }
        },
        containerColor = IdeDarkSurface
    )
}

@Composable
fun CreateFileDialog(
    isFolder: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFolder) "New Directory" else "New File", color = IdeTextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isFolder) "Directory Name" else "File Name") },
                placeholder = { Text(if (isFolder) "e.g. utils" else "e.g. helper.py") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IdeAccentBlue,
                    unfocusedBorderColor = IdeDarkBorder,
                    focusedTextColor = IdeTextPrimary,
                    unfocusedTextColor = IdeTextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("dialog_create_file_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IdeTextSecondary)
            }
        },
        containerColor = IdeDarkSurface
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename", color = IdeTextPrimary) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IdeAccentBlue,
                    unfocusedBorderColor = IdeDarkBorder,
                    focusedTextColor = IdeTextPrimary,
                    unfocusedTextColor = IdeTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (newName.isNotBlank()) onRename(newName.trim()) },
                enabled = newName.isNotBlank() && newName != currentName,
                colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue)
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IdeTextSecondary)
            }
        },
        containerColor = IdeDarkSurface
    )
}

@Composable
fun ConfirmDeleteDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete '$itemName'?", color = IdeTextPrimary) },
        text = {
            Text(
                "Are you sure you want to permanently delete '$itemName'? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = IdeTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = IdeRed),
                modifier = Modifier.testTag("dialog_confirm_delete_button")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IdeTextSecondary)
            }
        },
        containerColor = IdeDarkSurface
    )
}
