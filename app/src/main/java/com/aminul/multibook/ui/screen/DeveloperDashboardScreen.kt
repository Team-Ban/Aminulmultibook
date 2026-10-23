package com.aminul.multibook.ui.screen

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aminul.multibook.data.profile.ProfileManager
import com.aminul.multibook.data.util.DiagnosticLogger
import com.aminul.multibook.ui.theme.FacebookBlue
import com.aminul.multibook.ui.theme.GreenActive
import com.aminul.multibook.ui.theme.RedDanger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDashboardScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by DiagnosticLogger.logsFlow.collectAsStateWithLifecycle()

    var isolationTestResults by remember { mutableStateOf<List<ProfileManager.IsolationTestResult>?>(null) }
    var isRunningTests by remember { mutableStateOf(false) }

    // Memory info
    var memoryTrigger by remember { mutableStateOf(0) }
    val (ramInfo, heapInfo) = remember(memoryTrigger) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val lowMem = memInfo.lowMemory

        val runtime = Runtime.getRuntime()
        val heapAllocatedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val heapMaxMb = runtime.maxMemory() / (1024 * 1024)

        Pair(
            "Total: ${totalRamMb} MB | Avail: ${availRamMb} MB | LowMem: $lowMem",
            "Allocated: ${heapAllocatedMb} MB / Max: ${heapMaxMb} MB"
        )
    }

    val isolationStatus = remember { ProfileManager.getIsolationDiagnostics(context) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Developer Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { memoryTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh telemetry")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FacebookBlue)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Developer Mode Active",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Full access to memory telemetry, WebView engine diagnostics, live logs, and automated isolation tests.",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 1. App & Android Build Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(icon = Icons.Default.Computer, title = "App & Android Platform")
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(label = "Application Name", value = "Aminul MultiBook")
                        InfoRow(label = "Developer", value = "Aminulsordar")
                        InfoRow(label = "Package Name", value = "com.aminul.multibook")
                        InfoRow(label = "Version", value = "1.0 (Code: 1)")
                        InfoRow(label = "Android Version", value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        InfoRow(label = "Device Model", value = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
                        InfoRow(label = "Board / Hardware", value = "${Build.BOARD} / ${Build.HARDWARE}")
                        InfoRow(label = "Supported ABIs", value = Build.SUPPORTED_ABIS.joinToString(", "))
                        InfoRow(label = "System Uptime", value = formatUptime(SystemClock.elapsedRealtime()))
                    }
                }
            }

            // 2. RAM & Process Memory Metrics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(icon = Icons.Default.Memory, title = "Live RAM & Memory Telemetry")
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(label = "System RAM", value = ramInfo)
                        InfoRow(label = "JVM Process Heap", value = heapInfo)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                System.gc()
                                memoryTrigger++
                                Toast.makeText(context, "System.gc() invoked", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Trigger JVM Garbage Collector")
                        }
                    }
                }
            }

            // 3. WebView Engine Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(icon = Icons.Default.Security, title = "WebView Engine & ProfileStore")
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(label = "Provider Package", value = isolationStatus.webViewPackage)
                        InfoRow(label = "Provider Version", value = isolationStatus.webViewVersion)
                        InfoRow(label = "AndroidX Multi-Profile", value = if (isolationStatus.isHardwareMultiProfileActive) "SUPPORTED (Active)" else "UNSUPPORTED (Fallback)")
                        InfoRow(label = "App Data Directory", value = context.dataDir.absolutePath)
                        InfoRow(label = "App Cache Directory", value = context.cacheDir.absolutePath)
                    }
                }
            }

            // 4. Interactive Browser Session Isolation Test Runner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(icon = Icons.Default.Security, title = "Session Isolation Test Suite")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Verifies that cookies written to Profile Alpha cannot be read by Profile Beta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isRunningTests = true
                                scope.launch {
                                    val results = withContext(Dispatchers.Default) {
                                        ProfileManager.runIsolationTestSuite()
                                    }
                                    isolationTestResults = results
                                    isRunningTests = false
                                }
                            },
                            enabled = !isRunningTests,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRunningTests) "Running Tests..." else "Run Isolation Test Suite")
                        }

                        isolationTestResults?.let { results ->
                            Spacer(modifier = Modifier.height(12.dp))
                            results.forEach { res ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (res.passed) GreenActive.copy(alpha = 0.1f) else RedDanger.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (res.passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (res.passed) GreenActive else RedDanger,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                res.testName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Text(
                                                res.detail,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Cache & Storage Clearing Tools
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(icon = Icons.Default.CleaningServices, title = "Cache & Storage Reset")
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            context.cacheDir.deleteRecursively()
                                        }
                                        Toast.makeText(context, "App cache cleared", Toast.LENGTH_SHORT).show()
                                        DiagnosticLogger.info("DevDashboard", "Disk cache wiped")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear Disk Cache")
                            }

                            OutlinedButton(
                                onClick = {
                                    WebStorage.getInstance().deleteAllData()
                                    Toast.makeText(context, "WebStorage erased", Toast.LENGTH_SHORT).show()
                                    DiagnosticLogger.info("DevDashboard", "WebStorage wiped")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Wipe WebStorage")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                CookieManager.getInstance().removeAllCookies {
                                    CookieManager.getInstance().flush()
                                    Toast.makeText(context, "Global cookies flushed", Toast.LENGTH_SHORT).show()
                                    DiagnosticLogger.info("DevDashboard", "Global cookie jar flushed")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Global Cookie Jar")
                        }
                    }
                }
            }

            // 6. Live Diagnostic Logs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SectionHeader(icon = Icons.Default.BugReport, title = "Live Diagnostic Logs (${logs.size})")
                            OutlinedButton(onClick = { DiagnosticLogger.clear() }) {
                                Text("Clear")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(8.dp)
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    "No logs recorded yet.",
                                    color = Color(0xFF94A3B8),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn {
                                    items(logs) { entry ->
                                        val levelColor = when (entry.level) {
                                            "ERROR" -> Color(0xFFEF4444)
                                            "WARN" -> Color(0xFFF59E0B)
                                            "DEBUG" -> Color(0xFF38BDF8)
                                            else -> Color(0xFF4ADE80)
                                        }
                                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                            Text(
                                                text = "${entry.timestamp} [${entry.level}] ",
                                                color = levelColor,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${entry.tag}: ${entry.message}",
                                                color = Color(0xFFE2E8F0),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun formatUptime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return "${hours}h ${minutes}m ${seconds}s"
}
