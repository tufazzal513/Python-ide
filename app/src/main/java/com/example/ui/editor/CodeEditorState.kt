package com.example.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.io.File

data class EditorTab(
    val file: File,
    val content: TextFieldValue,
    val undoStack: List<TextFieldValue> = emptyList(),
    val redoStack: List<TextFieldValue> = emptyList(),
    val isModified: Boolean = false,
    val scrollOffset: Int = 0
)

data class CodeEditorUiState(
    val tabs: List<EditorTab> = emptyList(),
    val activeTabIndex: Int = 0,
    val fontSizeSp: Int = 13,
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val searchMatchCount: Int = 0,
    val currentMatchIndex: Int = 0,
    val isGoToLineOpen: Boolean = false,
    val goToLineText: String = ""
) {
    val activeTab: EditorTab?
        get() = if (activeTabIndex in tabs.indices) tabs[activeTabIndex] else null
}
