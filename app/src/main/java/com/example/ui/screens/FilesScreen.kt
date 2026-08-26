package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.FileNode
import com.example.core.model.Project
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CreateFileDialog
import com.example.ui.components.RenameDialog
import com.example.ui.theme.IdeAccentBlue
import com.example.ui.theme.IdeCyan
import com.example.ui.theme.IdeDarkBackground
import com.example.ui.theme.IdeDarkBorder
import com.example.ui.theme.IdeDarkHeader
import com.example.ui.theme.IdeDarkSurface
import com.example.ui.theme.IdeOrange
import com.example.ui.theme.IdePythonYellow
import com.example.ui.theme.IdeRed
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import java.io.File

@Composable
fun FilesScreen(
    project: Project,
    fileTree: List<FileNode>,
    onOpenFile: (File) -> Unit,
    onCreateFile: (parentDir: File, fileName: String) -> Unit,
    onCreateDirectory: (parentDir: File, dirName: String) -> Unit,
    onDeleteFile: (file: File) -> Unit,
    onRenameFile: (file: File, newName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }

    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
    ) {
        // Files Toolbar
        Surface(
            color = IdeDarkHeader,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, IdeDarkBorder)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Project: ${project.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = IdeTextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { showCreateFileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).testTag("files_create_file_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("File", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showCreateFolderDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).testTag("files_create_folder_button")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(14.dp), tint = IdePythonYellow)
                        Spacer(Modifier.width(4.dp))
                        Text("Folder", fontSize = 11.sp, color = IdeTextPrimary)
                    }
                }
            }
        }

        // File Tree List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (fileTree.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No files in project folder", color = IdeTextMuted)
                    }
                }
            } else {
                items(fileTree) { node ->
                    FileNodeItem(
                        node = node,
                        depth = 0,
                        expandedFolders = expandedFolders,
                        onToggleExpand = { path ->
                            expandedFolders[path] = !(expandedFolders[path] ?: false)
                        },
                        onOpenFile = onOpenFile,
                        onRequestRename = { fileToRename = File(it.path) },
                        onRequestDelete = { fileToDelete = File(it.path) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateFileDialog) {
        CreateFileDialog(
            isFolder = false,
            onDismiss = { showCreateFileDialog = false },
            onCreate = { name ->
                onCreateFile(File(project.path), name)
                showCreateFileDialog = false
            }
        )
    }

    if (showCreateFolderDialog) {
        CreateFileDialog(
            isFolder = true,
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name ->
                onCreateDirectory(File(project.path), name)
                showCreateFolderDialog = false
            }
        )
    }

    fileToDelete?.let { target ->
        ConfirmDeleteDialog(
            itemName = target.name,
            onDismiss = { fileToDelete = null },
            onConfirmDelete = {
                onDeleteFile(target)
                fileToDelete = null
            }
        )
    }

    fileToRename?.let { target ->
        RenameDialog(
            currentName = target.name,
            onDismiss = { fileToRename = null },
            onRename = { newName ->
                onRenameFile(target, newName)
                fileToRename = null
            }
        )
    }
}

@Composable
fun FileNodeItem(
    node: FileNode,
    depth: Int,
    expandedFolders: Map<String, Boolean>,
    onToggleExpand: (String) -> Unit,
    onOpenFile: (File) -> Unit,
    onRequestRename: (FileNode) -> Unit,
    onRequestDelete: (FileNode) -> Unit
) {
    val isExpanded = expandedFolders[node.path] ?: false
    var menuExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDirectory) {
                        onToggleExpand(node.path)
                    } else {
                        onOpenFile(File(node.path))
                    }
                }
                .padding(start = (depth * 18).dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (node.isDirectory) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = IdeTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                        contentDescription = null,
                        tint = IdePythonYellow,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Spacer(Modifier.width(16.dp))
                    val fileIcon = when {
                        node.name.endsWith(".py") -> Icons.Default.Code
                        node.name.startsWith(".env") -> Icons.Default.Key
                        node.name.endsWith(".json") || node.name.endsWith(".xml") -> Icons.Default.Code
                        else -> Icons.Default.Description
                    }
                    val iconTint = when {
                        node.name.endsWith(".py") -> IdeCyan
                        node.name.startsWith(".env") -> IdeOrange
                        else -> IdeTextSecondary
                    }
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = node.name,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = if (node.isDirectory) IdeTextPrimary else IdeTextSecondary,
                    maxLines = 1
                )
            }

            // Options menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = IdeTextMuted, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(IdeDarkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = IdeTextPrimary) },
                        onClick = {
                            menuExpanded = false
                            onRequestRename(node)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = IdeRed) },
                        onClick = {
                            menuExpanded = false
                            onRequestDelete(node)
                        }
                    )
                }
            }
        }

        // Recursive children if directory is expanded
        if (node.isDirectory && isExpanded) {
            node.children.forEach { child ->
                FileNodeItem(
                    node = child,
                    depth = depth + 1,
                    expandedFolders = expandedFolders,
                    onToggleExpand = onToggleExpand,
                    onOpenFile = onOpenFile,
                    onRequestRename = onRequestRename,
                    onRequestDelete = onRequestDelete
                )
            }
        }
    }
}
