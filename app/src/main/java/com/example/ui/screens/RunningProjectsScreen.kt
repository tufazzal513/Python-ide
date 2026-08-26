package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.IdeRed
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import kotlinx.coroutines.delay

@Composable
fun RunningProjectsScreen(
    processes: List<ProcessInfo>,
    projects: List<Project>,
    onSelectProject: (Project) -> Unit,
    onStopProcess: (String) -> Unit,
    onRestartProject: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

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
                text = "Running Python Processes (${processes.count { it.status == ProcessStatus.RUNNING }})",
                style = MaterialTheme.typography.titleLarge,
                color = IdeTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Live telemetry of background scripts, HTTP servers, and bots.",
                style = MaterialTheme.typography.bodySmall,
                color = IdeTextSecondary
            )
        }

        if (processes.isEmpty() || processes.none { it.status == ProcessStatus.RUNNING || it.status == ProcessStatus.STARTING }) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No Active Processes Running",
                            style = MaterialTheme.typography.titleMedium,
                            color = IdeTextSecondary
                        )
                        Text(
                            text = "Start a project from the Home screen or Project Dashboard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IdeTextMuted
                        )
                    }
                }
            }
        } else {
            items(processes) { proc ->
                val project = projects.firstOrNull { it.id == proc.projectId }
                val isRunning = proc.status == ProcessStatus.RUNNING
                val durationSec = ((currentTime - proc.startTime) / 1000).coerceAtLeast(0)
                val durationText = String.format("%02d:%02d:%02d", durationSec / 3600, (durationSec % 3600) / 60, durationSec % 60)

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
                                    text = proc.projectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IdeTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Entry: ${proc.entryPoint}",
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
                                    text = proc.status.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRunning) IdeGreen else IdeTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Resource Telemetry Metrics
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(IdeDarkHeader, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = IdeCyan, modifier = Modifier.size(16.dp))
                                Text(durationText, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = IdeTextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = IdeGreen, modifier = Modifier.size(16.dp))
                                Text("CPU: ${proc.cpuUsage}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = IdeTextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = IdeOrange, modifier = Modifier.size(16.dp))
                                Text("RAM: ${proc.memoryUsage}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = IdeTextPrimary)
                            }
                        }

                        // Web Server Endpoint Box
                        if (proc.localUrl != null) {
                            val serverUrl = proc.localUrl
                            val lanAddress = proc.lanUrl
                            Surface(
                                color = IdeDarkSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, IdeCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "HTTP Web Server (Port ${proc.localPort ?: 8000})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IdeCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Local: $serverUrl",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = IdeTextPrimary
                                        )
                                        if (lanAddress != null) {
                                            Text(
                                                text = "LAN: $lanAddress",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = IdeGreen
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                    putExtra(Intent.EXTRA_TEXT, lanAddress ?: serverUrl)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Share Server URL"))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = IdeTextSecondary, modifier = Modifier.size(16.dp))
                                        }

                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = IdeAccentBlue),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Open", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Process Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isRunning) {
                                Button(
                                    onClick = { onStopProcess(proc.projectId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = IdeRed),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Stop Process", fontSize = 12.sp)
                                }
                            }

                            if (project != null) {
                                OutlinedButton(
                                    onClick = { onRestartProject(project) },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = IdeTextPrimary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Restart", fontSize = 12.sp, color = IdeTextPrimary)
                                }

                                OutlinedButton(
                                    onClick = { onSelectProject(project) },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Text("Open Project", fontSize = 12.sp, color = IdeAccentBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
