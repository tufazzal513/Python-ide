package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.LogCategory
import com.example.core.model.LogEntry
import com.example.core.model.LogLevel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    logs: List<LogEntry>,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf("All") + LogCategory.entries.map { it.name }
    }

    val safeCategoryIndex = selectedCategoryIndex.coerceIn(0, (categories.size - 1).coerceAtLeast(0))

    val filteredLogs = remember(logs, safeCategoryIndex, searchQuery) {
        logs.filter { entry ->
            val matchesCategory = if (safeCategoryIndex == 0) true
            else entry.category.name.equals(categories.getOrNull(safeCategoryIndex) ?: "", ignoreCase = true)

            val matchesSearch = if (searchQuery.isBlank()) true
            else entry.message.contains(searchQuery, ignoreCase = true) || entry.projectName.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
    ) {
        // Log Category Tabs
        ScrollableTabRow(
            selectedTabIndex = safeCategoryIndex,
            containerColor = IdeDarkHeader,
            contentColor = IdeTextPrimary,
            edgePadding = 8.dp,
            divider = { Box(Modifier.height(1.dp).background(IdeDarkBorder)) },
            indicator = { tabPositions ->
                val safePosIndex = safeCategoryIndex.coerceIn(0, (tabPositions.size - 1).coerceAtLeast(0))
                if (tabPositions.isNotEmpty() && safePosIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[safePosIndex]),
                        color = IdeAccentBlue
                    )
                }
            }
        ) {
            categories.forEachIndexed { index, catName ->
                val isSelected = index == safeCategoryIndex
                Tab(
                    selected = isSelected,
                    onClick = { selectedCategoryIndex = index },
                    text = {
                        Text(
                            text = catName.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) IdeAccentBlue else IdeTextSecondary
                        )
                    }
                )
            }
        }

        // Search and Actions Toolbar
        Surface(
            color = IdeDarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, IdeDarkBorder)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter logs...", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IdeAccentBlue,
                        unfocusedBorderColor = IdeDarkBorder,
                        focusedTextColor = IdeTextPrimary,
                        unfocusedTextColor = IdeTextPrimary
                    ),
                    modifier = Modifier.weight(1f).height(44.dp).testTag("logs_filter_input")
                )

                IconButton(
                    onClick = {
                        val text = filteredLogs.joinToString("\n") { "[${timeFormatter.format(Date(it.timestamp))}] [${it.category}] [${it.level}] ${it.message}" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Logs", text))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy logs", tint = IdeTextSecondary)
                }

                IconButton(
                    onClick = onExportLogs,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export logs", tint = IdeCyan)
                }

                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear logs", tint = IdeRed)
                }
            }
        }

        // Log Entries List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No logs recorded yet.", color = IdeTextMuted)
                    }
                }
            } else {
                items(filteredLogs) { log ->
                    val levelColor = when (log.level) {
                        LogLevel.ERROR -> IdeRed
                        LogLevel.WARN -> IdeOrange
                        LogLevel.DEBUG -> IdeTextMuted
                        LogLevel.INFO -> IdeCyan
                    }

                    Surface(
                        color = IdeDarkSurfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, IdeDarkBorder, RoundedCornerShape(4.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = timeFormatter.format(Date(log.timestamp)),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = IdeTextMuted
                            )

                            Text(
                                text = "[${log.category.name}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = IdeAccentBlue
                            )

                            Text(
                                text = log.level.name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = levelColor,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = log.message,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = IdeTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
