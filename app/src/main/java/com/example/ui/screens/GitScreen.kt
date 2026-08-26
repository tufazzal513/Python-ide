package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.GitStatusInfo
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
fun GitScreen(
    project: Project,
    gitStatus: GitStatusInfo,
    onRefreshStatus: () -> Unit,
    onCommit: (message: String) -> Unit,
    onSwitchBranch: (branch: String) -> Unit,
    onCreateBranch: (branch: String) -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commitMessage by remember { mutableStateOf("") }
    var branchExpanded by remember { mutableStateOf(false) }
    var showCreateBranchDialog by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Git Repository Info Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, tint = IdePythonYellow, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = "Git: ${project.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IdeTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = gitStatus.remoteUrl ?: "Local repository",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IdeTextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRefreshStatus,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp).testTag("git_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = IdeTextPrimary)
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh", fontSize = 11.sp, color = IdeTextPrimary)
                        }
                    }

                    // Branch Switcher & Create Branch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = branchExpanded,
                            onExpandedChange = { branchExpanded = !branchExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Branch: ${gitStatus.currentBranch}",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IdeAccentBlue,
                                    unfocusedBorderColor = IdeDarkBorder,
                                    focusedTextColor = IdeTextPrimary,
                                    unfocusedTextColor = IdeTextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = branchExpanded,
                                onDismissRequest = { branchExpanded = false },
                                modifier = Modifier.background(IdeDarkSurface)
                            ) {
                                gitStatus.branches.forEach { branch ->
                                    DropdownMenuItem(
                                        text = { Text(branch, color = IdeTextPrimary) },
                                        onClick = {
                                            onSwitchBranch(branch)
                                            branchExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showCreateBranchDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Branch", fontSize = 12.sp)
                        }
                    }

                    // Pull / Push Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPull,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = IdeCyan)
                            Spacer(Modifier.width(4.dp))
                            Text("Pull", fontSize = 12.sp, color = IdeTextPrimary)
                        }

                        OutlinedButton(
                            onClick = onPush,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = IdeGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("Push", fontSize = 12.sp, color = IdeTextPrimary)
                        }
                    }
                }
            }
        }

        // Commit Section
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
                        text = "Commit Changes",
                        style = MaterialTheme.typography.titleMedium,
                        color = IdeTextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        placeholder = { Text("Commit message (e.g. Add REST endpoints)", color = IdeTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IdeAccentBlue,
                            unfocusedBorderColor = IdeDarkBorder,
                            focusedTextColor = IdeTextPrimary,
                            unfocusedTextColor = IdeTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("git_commit_message_input")
                    )

                    Button(
                        onClick = {
                            if (commitMessage.isNotBlank()) {
                                onCommit(commitMessage.trim())
                                commitMessage = ""
                            }
                        },
                        enabled = commitMessage.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = IdeGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End).testTag("git_commit_button")
                    ) {
                        Icon(Icons.Default.Commit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Commit to ${gitStatus.currentBranch}")
                    }
                }
            }
        }

        // Changed Files List
        item {
            Text(
                text = "Working Tree Status",
                style = MaterialTheme.typography.titleMedium,
                color = IdeTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (gitStatus.isClean && gitStatus.modifiedFiles.isEmpty()) {
            item {
                Surface(
                    color = IdeDarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, IdeDarkBorder, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IdeGreen, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Working tree is clean. Nothing to commit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IdeTextSecondary
                        )
                    }
                }
            }
        } else {
            items(gitStatus.modifiedFiles) { file ->
                Surface(
                    color = IdeDarkSurface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, IdeDarkBorder, RoundedCornerShape(6.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = IdeOrange, modifier = Modifier.size(16.dp))
                            Text(
                                text = file,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = IdeTextPrimary
                            )
                        }
                        Text(
                            text = "Modified",
                            fontSize = 11.sp,
                            color = IdeOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showCreateBranchDialog) {
        AlertDialog(
            onDismissRequest = { showCreateBranchDialog = false },
            title = { Text("Create New Branch", color = IdeTextPrimary) },
            text = {
                OutlinedTextField(
                    value = newBranchName,
                    onValueChange = { newBranchName = it },
                    label = { Text("Branch Name") },
                    placeholder = { Text("e.g. feature/api-routes") },
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
                    onClick = {
                        if (newBranchName.isNotBlank()) {
                            onCreateBranch(newBranchName.trim())
                            showCreateBranchDialog = false
                            newBranchName = ""
                        }
                    },
                    enabled = newBranchName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBranchDialog = false }) {
                    Text("Cancel", color = IdeTextSecondary)
                }
            },
            containerColor = IdeDarkSurface
        )
    }
}
