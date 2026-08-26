package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IdeAccentBlue
import com.example.ui.theme.IdeDarkBackground
import com.example.ui.theme.IdeDarkBorder
import com.example.ui.theme.IdeDarkHeader
import com.example.ui.theme.IdeDarkSurface
import com.example.ui.theme.IdeDarkSurfaceVariant
import com.example.ui.theme.IdeGreen
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import com.example.ui.theme.SyntaxGutter
import com.example.ui.theme.SyntaxGutterText

@Composable
fun CodeEditorView(
    state: CodeEditorUiState,
    onTabSelect: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onSave: () -> Unit,
    onRun: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit,
    onGoToLine: (Int) -> Unit,
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var targetLineInput by remember { mutableStateOf("") }

    val activeTab = state.activeTab

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
    ) {
        // Tab Bar
        if (state.tabs.isNotEmpty()) {
            val safeTabIndex = state.activeTabIndex.coerceIn(0, (state.tabs.size - 1).coerceAtLeast(0))
            ScrollableTabRow(
                selectedTabIndex = safeTabIndex,
                containerColor = IdeDarkHeader,
                contentColor = IdeTextPrimary,
                edgePadding = 0.dp,
                divider = { Box(Modifier.height(1.dp).background(IdeDarkBorder)) },
                indicator = { tabPositions ->
                    val safePosIndex = safeTabIndex.coerceIn(0, (tabPositions.size - 1).coerceAtLeast(0))
                    if (tabPositions.isNotEmpty() && safePosIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[safePosIndex]),
                            color = IdeAccentBlue
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                state.tabs.forEachIndexed { index, tab ->
                    val isSelected = index == state.activeTabIndex
                    Tab(
                        selected = isSelected,
                        onClick = { onTabSelect(index) },
                        modifier = Modifier
                            .background(if (isSelected) IdeDarkBackground else IdeDarkHeader)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${tab.file.name}${if (tab.isModified) " *" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) IdeAccentBlue else IdeTextSecondary,
                                maxLines = 1
                            )
                            IconButton(
                                onClick = { onTabClose(index) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    tint = IdeTextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Editor Toolbar (Undo, Redo, Save, Search, GoToLine, Run)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IdeDarkSurface)
                .border(1.dp, IdeDarkBorder)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onUndo,
                    enabled = activeTab?.undoStack?.isNotEmpty() == true,
                    modifier = Modifier.size(36.dp).testTag("editor_undo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (activeTab?.undoStack?.isNotEmpty() == true) IdeTextPrimary else IdeTextMuted
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = activeTab?.redoStack?.isNotEmpty() == true,
                    modifier = Modifier.size(36.dp).testTag("editor_redo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (activeTab?.redoStack?.isNotEmpty() == true) IdeTextPrimary else IdeTextMuted
                    )
                }

                IconButton(
                    onClick = onSave,
                    enabled = activeTab != null,
                    modifier = Modifier.size(36.dp).testTag("editor_save")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save file",
                        tint = if (activeTab?.isModified == true) IdeGreen else IdeTextSecondary
                    )
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.size(36.dp).testTag("editor_search_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search and Replace",
                        tint = if (state.isSearching) IdeAccentBlue else IdeTextSecondary
                    )
                }

                IconButton(
                    onClick = { showGoToLineDialog = true },
                    modifier = Modifier.size(36.dp).testTag("editor_goto_line")
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = "Go to line",
                        tint = IdeTextSecondary
                    )
                }
            }

            // Run Button
            Button(
                onClick = onRun,
                colors = ButtonDefaults.buttonColors(containerColor = IdeGreen),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(32.dp).testTag("editor_run_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Run", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }

        // Find & Replace Floating Panel
        if (state.isSearching) {
            Surface(
                color = IdeDarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth().border(1.dp, IdeDarkBorder)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Find...", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, color = IdeTextPrimary),
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IdeAccentBlue,
                                unfocusedBorderColor = IdeDarkBorder
                            )
                        )
                        OutlinedTextField(
                            value = state.replaceQuery,
                            onValueChange = onReplaceQueryChange,
                            placeholder = { Text("Replace with...", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, color = IdeTextPrimary),
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IdeAccentBlue,
                                unfocusedBorderColor = IdeDarkBorder
                            )
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.searchMatchCount > 0) "${state.searchMatchCount} matches" else "No matches",
                            style = MaterialTheme.typography.labelSmall,
                            color = IdeTextMuted,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        TextButton(onClick = onReplaceOne, enabled = state.searchMatchCount > 0) {
                            Text("Replace", fontSize = 11.sp, color = IdeAccentBlue)
                        }
                        TextButton(onClick = onReplaceAll, enabled = state.searchMatchCount > 0) {
                            Text("Replace All", fontSize = 11.sp, color = IdeAccentBlue)
                        }
                    }
                }
            }
        }

        // Code Editor Area with Line Numbers Gutter
        if (activeTab != null) {
            val contentText = activeTab.content.text
            val lines = remember(contentText) { contentText.lines() }
            val lineCount = remember(lines) { lines.size.coerceAtLeast(1) }

            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(IdeDarkBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                ) {
                    // Line numbers gutter
                    if (state.showLineNumbers) {
                        Column(
                            modifier = Modifier
                                .background(SyntaxGutter)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = state.fontSizeSp.sp,
                                    lineHeight = (state.fontSizeSp + 6).sp,
                                    color = SyntaxGutterText
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(IdeDarkBorder)
                        )
                    }

                    // Code Editor Text Area with Syntax Highlighting
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(if (!state.wordWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
                            .padding(8.dp)
                    ) {
                        val visualTransformation = remember(activeTab.file.name) {
                            VisualTransformation { text ->
                                val highlighted = CodeHighlighter.highlight(text.text, activeTab.file.name)
                                TransformedText(highlighted, OffsetMapping.Identity)
                            }
                        }

                        BasicTextField(
                            value = activeTab.content,
                            onValueChange = onContentChange,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = state.fontSizeSp.sp,
                                lineHeight = (state.fontSizeSp + 6).sp,
                                color = IdeTextPrimary
                            ),
                            cursorBrush = SolidColor(IdeAccentBlue),
                            visualTransformation = visualTransformation,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("code_editor_text_field")
                        )
                    }
                }
            }
        } else {
            // Empty state when no file is open
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No File Open",
                        style = MaterialTheme.typography.titleMedium,
                        color = IdeTextSecondary
                    )
                    Text(
                        text = "Select a file from the Files tab to begin editing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IdeTextMuted
                    )
                }
            }
        }

        // Quick Symbol Accessory Bar (Above Software Keyboard)
        Surface(
            color = IdeDarkHeader,
            modifier = Modifier.fillMaxWidth().border(1.dp, IdeDarkBorder)
        ) {
            val symbols = listOf(
                "Tab" to "    ",
                ":" to ":",
                "=" to "=",
                "(" to "()",
                ")" to ")",
                "[" to "[]",
                "]" to "]",
                "{" to "{}",
                "}" to "}",
                "\"" to "\"\"",
                "'" to "''",
                "#" to "# ",
                "_" to "_",
                "+" to "+",
                "-" to "-",
                "*" to "*",
                "/" to "/",
                "<" to "<",
                ">" to ">",
                "." to ".",
                "def" to "def ",
                "return" to "return ",
                "import" to "import "
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                symbols.forEach { (label, insertValue) ->
                    Box(
                        modifier = Modifier
                            .background(IdeDarkSurface, RoundedCornerShape(4.dp))
                            .border(1.dp, IdeDarkBorder, RoundedCornerShape(4.dp))
                            .clickable { onInsertSymbol(insertValue) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = IdeTextPrimary
                        )
                    }
                }
            }
        }
    }

    // Go To Line Dialog
    if (showGoToLineDialog) {
        AlertDialog(
            onDismissRequest = { showGoToLineDialog = false },
            title = { Text("Go to Line", color = IdeTextPrimary) },
            text = {
                OutlinedTextField(
                    value = targetLineInput,
                    onValueChange = { targetLineInput = it.filter { ch -> ch.isDigit() } },
                    placeholder = { Text("Line number (e.g. 24)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IdeAccentBlue,
                        unfocusedBorderColor = IdeDarkBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lineNum = targetLineInput.toIntOrNull()
                        if (lineNum != null && lineNum > 0) {
                            onGoToLine(lineNum)
                        }
                        showGoToLineDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue)
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToLineDialog = false }) {
                    Text("Cancel", color = IdeTextSecondary)
                }
            },
            containerColor = IdeDarkSurface
        )
    }
}
