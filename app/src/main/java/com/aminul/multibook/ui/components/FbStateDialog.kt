package com.aminul.multibook.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminul.multibook.R
import com.aminul.multibook.data.extension.FbStateHelper
import com.aminul.multibook.ui.theme.FacebookBlue
import com.aminul.multibook.ui.theme.GreenActive
import com.aminul.multibook.ui.theme.RedDanger

@Composable
fun FbStateDialog(
    accountId: String,
    accountName: String,
    onDismiss: () -> Unit,
    onCookiesInjected: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Import

    // Export State
    var exportedState by remember { mutableStateOf("") }
    var exportKey by remember { mutableStateOf("") }
    var isEncryptedExport by remember { mutableStateOf(false) }

    // Import State
    var importText by remember { mutableStateOf("") }
    var importKey by remember { mutableStateOf("") }
    var isEncryptedImport by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().use { r -> r.readText() }
                    importText = content
                    Toast.makeText(context, "Loaded file content", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-generate export on open
    LaunchedEffect(accountId) {
        exportedState = FbStateHelper.exportFbState(accountId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_c3c_logo),
                    contentDescription = "c3c-fbstate logo",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("c3c-fbstate Extension", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Account: $accountName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Export") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Import") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // EXPORT TAB
                    Text(
                        "Export current session cookies (fbstate.json) to use with bots or restore later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isEncryptedExport = false
                                exportedState = FbStateHelper.exportFbState(accountId)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isEncryptedExport) Color(0xFFE67E22) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isEncryptedExport) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Plain JSON", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                isEncryptedExport = true
                                val plain = FbStateHelper.exportFbState(accountId)
                                exportedState = if (exportKey.isNotBlank()) {
                                    FbStateHelper.encryptFbState(plain, exportKey)
                                } else {
                                    "Enter encryption key below and tap 'Encrypt'"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEncryptedExport) Color(0xFFE67E22) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isEncryptedExport) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Encrypted", fontSize = 12.sp)
                        }
                    }

                    if (isEncryptedExport) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = exportKey,
                                onValueChange = {
                                    exportKey = it
                                    val plain = FbStateHelper.exportFbState(accountId)
                                    exportedState = if (it.isNotBlank()) {
                                        try {
                                            FbStateHelper.encryptFbState(plain, it)
                                        } catch (e: Exception) {
                                            "Encryption error: ${e.message}"
                                        }
                                    } else {
                                        "Enter password above"
                                    }
                                },
                                label = { Text("AES-256 Key") },
                                placeholder = { Text("Secret password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = exportedState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isEncryptedExport) "Encrypted Hex" else "fbstate.json") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("fbstate", exportedState)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied FBState to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, exportedState)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share FBState"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenActive)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share/Save", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Safe Logout Button
                    Button(
                        onClick = {
                            FbStateHelper.safeLogout(accountId) {
                                Toast.makeText(context, "Safe logout completed (session kept alive)", Toast.LENGTH_LONG).show()
                                onCookiesInjected()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Logout (not die fbstate)")
                    }
                } else {
                    // IMPORT TAB
                    Text(
                        "Paste a fbstate.json or select a file to instantly log into this account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("application/json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select File", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { isEncryptedImport = !isEncryptedImport },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEncryptedImport) Color(0xFFE67E22) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isEncryptedImport) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(if (isEncryptedImport) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEncryptedImport) "Encrypted: ON" else "Encrypted: OFF", fontSize = 12.sp)
                        }
                    }

                    if (isEncryptedImport) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = importKey,
                            onValueChange = { importKey = it },
                            label = { Text("Decryption Key") },
                            placeholder = { Text("Password used when exporting") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Paste FBState Content Here") },
                        placeholder = { Text("[{\"key\":\"c_user\", ...}] or encrypted hex") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            var jsonToImport = importText.trim()
                            if (isEncryptedImport) {
                                try {
                                    jsonToImport = FbStateHelper.decryptFbState(jsonToImport, importKey)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Decryption failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                            }

                            FbStateHelper.importFbState(accountId, jsonToImport) { success, count, error ->
                                if (success) {
                                    Toast.makeText(context, "Successfully injected $count cookies! Reloading...", Toast.LENGTH_LONG).show()
                                    onCookiesInjected()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Import failed: $error", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenActive),
                        enabled = importText.isNotBlank()
                    ) {
                        Text("Import & Log In Instantly", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
