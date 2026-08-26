package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.IdeAccentBlue
import com.example.ui.theme.IdeCyan
import com.example.ui.theme.IdeDarkBackground
import com.example.ui.theme.IdeDarkBorder
import com.example.ui.theme.IdeDarkSurface
import com.example.ui.theme.IdeGreen
import com.example.ui.theme.IdePythonYellow
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    fontSize: Int,
    wordWrap: Boolean,
    showLineNumbers: Boolean,
    defaultPythonVersion: String,
    onFontSizeChange: (Int) -> Unit,
    onWordWrapChange: (Boolean) -> Unit,
    onShowLineNumbersChange: (Boolean) -> Unit,
    onDefaultPythonVersionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var versionExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Preferences & System Settings",
                style = MaterialTheme.typography.titleLarge,
                color = IdeTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure Python runtime defaults, code editor style, and battery keep-alive.",
                style = MaterialTheme.typography.bodySmall,
                color = IdeTextSecondary
            )
        }

        // Code Editor Settings Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = IdeAccentBlue, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Code Editor",
                            style = MaterialTheme.typography.titleMedium,
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Font Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Size", style = MaterialTheme.typography.bodyMedium, color = IdeTextPrimary)
                            Text("${fontSize}sp", fontFamily = FontFamily.Monospace, color = IdeCyan)
                        }
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = { onFontSizeChange(it.toInt()) },
                            valueRange = 10f..22f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = IdeAccentBlue,
                                activeTrackColor = IdeAccentBlue
                            ),
                            modifier = Modifier.testTag("settings_font_size_slider")
                        )
                    }

                    // Word Wrap Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Soft Word Wrap", style = MaterialTheme.typography.bodyMedium, color = IdeTextPrimary)
                            Text("Wrap long lines horizontally", style = MaterialTheme.typography.bodySmall, color = IdeTextSecondary)
                        }
                        Switch(
                            checked = wordWrap,
                            onCheckedChange = onWordWrapChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = IdeAccentBlue)
                        )
                    }

                    // Line Numbers Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Show Line Numbers", style = MaterialTheme.typography.bodyMedium, color = IdeTextPrimary)
                            Text("Display gutter row numbers", style = MaterialTheme.typography.bodySmall, color = IdeTextSecondary)
                        }
                        Switch(
                            checked = showLineNumbers,
                            onCheckedChange = onShowLineNumbersChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = IdeAccentBlue)
                        )
                    }
                }
            }
        }

        // Python Environment Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = IdePythonYellow, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Python Environment",
                            style = MaterialTheme.typography.titleMedium,
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = versionExpanded,
                        onExpandedChange = { versionExpanded = !versionExpanded }
                    ) {
                        OutlinedTextField(
                            value = "Default: Python $defaultPythonVersion",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Default Runtime Version") },
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
                                        onDefaultPythonVersionChange(v)
                                        versionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Android Background & Battery Keep-Alive Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = IdeGreen, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Background Execution Guide",
                            style = MaterialTheme.typography.titleMedium,
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "To allow Flask, FastAPI, and Telegram bots to keep running continuously when the screen is locked or when you switch apps, disable Android Battery Optimization for PyMobile IDE.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IdeTextSecondary
                    )

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Battery Optimization Settings", fontSize = 12.sp, color = IdeGreen)
                    }
                }
            }
        }

        // System & Architecture Details Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IdeDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, IdeDarkBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = IdeCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "System Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text("Device ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = IdeTextSecondary)
                    Text("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = IdeTextSecondary)
                    Text("PyMobile IDE Version: 1.0.0 (Native Android Build)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = IdeTextSecondary)
                }
            }
        }
    }
}
