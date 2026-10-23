package com.aminul.multibook.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminul.multibook.data.model.AccountEntity
import com.aminul.multibook.data.profile.ProfileManager
import com.aminul.multibook.ui.components.AccountCard
import com.aminul.multibook.ui.components.AddAccountDialog
import com.aminul.multibook.ui.components.ConfirmClearDataDialog
import com.aminul.multibook.ui.components.ConfirmDeleteDialog
import com.aminul.multibook.ui.components.FbStateDialog
import com.aminul.multibook.ui.components.RenameAccountDialog
import com.aminul.multibook.ui.theme.FacebookBlue
import com.aminul.multibook.ui.theme.GreenActive
import com.aminul.multibook.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accounts: List<AccountEntity>,
    activeAccountId: String?,
    isolationStatus: ProfileManager.IsolationStatus,
    themeMode: ThemeMode,
    onOpenAccount: (String) -> Unit,
    onAddAccount: (String, String) -> Unit,
    onRenameAccount: (String, String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onClearAccountData: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToRename by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToClearData by remember { mutableStateOf<AccountEntity?>(null) }
    var accountForFbState by remember { mutableStateOf<AccountEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Aminul MultiBook",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = FacebookBlue
                        )
                        Text(
                            text = "Multiple Accounts, One App",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = FacebookBlue,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Account"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Isolation Status Pill Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isolationStatus.isHardwareMultiProfileActive) {
                    FacebookBlue.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isolationStatus.isHardwareMultiProfileActive) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = if (isolationStatus.isHardwareMultiProfileActive) GreenActive else FacebookBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isolationStatus.isHardwareMultiProfileActive) {
                                "Isolated Multi-Profile Active"
                            } else {
                                "Session-Isolated Container Active"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isolationStatus.isHardwareMultiProfileActive) {
                                "Independent cookies and storage per account"
                            } else {
                                "Isolated per-account cache partition"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Account Switcher Horizontal Bar (when there are 2 or more accounts)
            if (accounts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        val isSelected = acc.id == activeAccountId
                        val accColor = try {
                            Color(android.graphics.Color.parseColor(acc.colorHex))
                        } catch (_: Exception) {
                            FacebookBlue
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { onOpenAccount(acc.id) },
                            label = { Text(acc.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(accColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FacebookBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Account List
            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No accounts saved yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button below to add your first Facebook account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            isActive = account.id == activeAccountId,
                            onOpen = { onOpenAccount(account.id) },
                            onRename = { accountToRename = account },
                            onClearData = { accountToClearData = account },
                            onRemove = { accountToDelete = account },
                            onOpenFbState = { accountForFbState = account }
                        )
                    }

                    // Spacer at the bottom to avoid FAB overlap
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, colorHex ->
                showAddDialog = false
                onAddAccount(name, colorHex)
            }
        )
    }

    accountToRename?.let { acc ->
        RenameAccountDialog(
            currentName = acc.name,
            onDismiss = { accountToRename = null },
            onConfirm = { newName ->
                accountToRename = null
                onRenameAccount(acc.id, newName)
            }
        )
    }

    accountToDelete?.let { acc ->
        ConfirmDeleteDialog(
            accountName = acc.name,
            onDismiss = { accountToDelete = null },
            onConfirm = {
                accountToDelete = null
                onRemoveAccount(acc.id)
            }
        )
    }

    accountToClearData?.let { acc ->
        ConfirmClearDataDialog(
            accountName = acc.name,
            onDismiss = { accountToClearData = null },
            onConfirm = {
                accountToClearData = null
                onClearAccountData(acc.id)
            }
        )
    }

    accountForFbState?.let { acc ->
        FbStateDialog(
            accountId = acc.id,
            accountName = acc.name,
            onDismiss = { accountForFbState = null },
            onCookiesInjected = {
                // If this is currently active or opening, touch
            }
        )
    }
}
