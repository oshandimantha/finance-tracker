package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.RestorePage
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryEditorDialog
import com.example.ui.components.CategoryVisuals
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.SettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    businessName: String,
    businessSubtitle: String,
    appLockEnabled: Boolean,
    privacyScreenEnabled: Boolean,
    onToggleAppLock: (Boolean) -> Unit,
    onTogglePrivacyScreen: (Boolean) -> Unit,
    onUpdateBusinessBranding: (name: String, subtitle: String) -> Unit,
    onSaveCategory: (existingId: Long?, name: String, hexColor: String, defaultType: TransactionType, icon: String?, notes: String?) -> Unit,
    onSoftDeleteCategory: (Long) -> Unit,
    onRestoreCategory: (Long) -> Unit,
    onCountCategoryTransactions: suspend (Long) -> Int,
    onDeleteCategoryPermanently: (categoryId: Long, deleteAssociatedTransactions: Boolean) -> Unit,
    onBuildCsv: suspend () -> String,
    onBuildExcelXml: suspend () -> String,
    onBuildJsonBackup: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onLoadSampleWorkbook: () -> Unit,
    onResetAppData: () -> Unit,
    onNotifyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategoryTxCount by remember { mutableStateOf(0) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val content = onBuildCsv()
                val ok = writeTextToUri(context, uri, content)
                onNotifyMessage(if (ok) "Exported transactions to CSV" else "Failed to write CSV file")
            }
        }
    }

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val content = onBuildExcelXml()
                val ok = writeTextToUri(context, uri, content)
                onNotifyMessage(if (ok) "Exported Excel workbook (.xls)" else "Failed to write Excel file")
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val content = onBuildJsonBackup()
                val ok = writeTextToUri(context, uri, content)
                onNotifyMessage(if (ok) "Saved JSON database backup" else "Failed to write backup")
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val content = readTextFromUri(context, uri)
                if (content != null) {
                    onRestoreBackupJson(content)
                } else {
                    onNotifyMessage("Unable to read selected backup file")
                }
            }
        }
    }

    if (showCategoryDialog || editingCategory != null) {
        CategoryEditorDialog(
            existingCategory = editingCategory,
            onDismiss = {
                showCategoryDialog = false
                editingCategory = null
            },
            onSave = { id, name, hex, type, icon, notes ->
                onSaveCategory(id, name, hex, type, icon, notes)
                showCategoryDialog = false
                editingCategory = null
            }
        )
    }

    deletingCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Delete \"${cat.name}\"?") },
            text = {
                Text(
                    text = if (deletingCategoryTxCount > 0) {
                        "This category is linked to $deletingCategoryTxCount transaction(s). Deleting it permanently will also remove those transactions. You can archive it instead to keep your ledger history."
                    } else {
                        "This will permanently delete the \"${cat.name}\" category and its monthly budgets. This action cannot be undone."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategoryPermanently(cat.id, deletingCategoryTxCount > 0)
                        deletingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_category_button")
                ) {
                    Text(if (deletingCategoryTxCount > 0) "Delete All" else "Delete")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (deletingCategoryTxCount > 0 && !cat.isDeleted) {
                        TextButton(
                            onClick = {
                                onSoftDeleteCategory(cat.id)
                                deletingCategory = null
                            }
                        ) {
                            Text("Archive Instead")
                        }
                    }
                    TextButton(onClick = { deletingCategory = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset App Data?") },
            text = {
                Text(
                    "This will permanently delete all transactions, monthly budgets, savings goals, custom categories, and reset your custom App Name back to defaults. Consider backing up to Google Drive first."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAppData()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_reset_app_button")
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.modern_finance_logo_1794928820205),
                    contentDescription = businessName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            },
            title = { Text(businessName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "App v1.0",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Developed by Oshan Dimantha",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "Personal & Business Finance Manager for LKR (Rs) budgets, ledger tracking, analytics, savings goals, CSV/Excel exports, and Google Drive backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Disclaimer: Financial summaries and metrics are for personal tracking and informational purposes only and do not constitute certified financial, tax, or investment advice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAppInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    when (uiState) {
        is SettingsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SettingsUiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is SettingsUiState.Success -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Business / Workspace Branding Card
                    item {
                        BusinessBrandingCard(
                            currentName = businessName,
                            currentSubtitle = businessSubtitle,
                            onSaveBranding = onUpdateBusinessBranding
                        )
                    }

                    // Security & Privacy Card
                    item {
                        SecurityPrivacyCard(
                            appLockEnabled = appLockEnabled,
                            privacyScreenEnabled = privacyScreenEnabled,
                            onToggleAppLock = onToggleAppLock,
                            onTogglePrivacyScreen = onTogglePrivacyScreen
                        )
                    }

                    // Export, Backup & Restore Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Export, Backup & Restore",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${uiState.totalTransactions} transactions • ${uiState.activeCategories.size} categories • ${uiState.totalGoals} goals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    DataActionButton(
                                        label = "Export CSV",
                                        icon = Icons.Rounded.Description,
                                        onClick = { csvLauncher.launch("${sanitizeFileName(businessName)}_transactions.csv") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_csv_button")
                                    )
                                    DataActionButton(
                                        label = "Export Excel",
                                        icon = Icons.Rounded.TableView,
                                        onClick = { excelLauncher.launch("${sanitizeFileName(businessName)}_workbook.xls") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_excel_button")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    DataActionButton(
                                        label = "Backup JSON",
                                        icon = Icons.Rounded.Backup,
                                        onClick = { backupLauncher.launch("${sanitizeFileName(businessName)}_backup.json") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("backup_json_button")
                                    )
                                    DataActionButton(
                                        label = "Restore Drive/File",
                                        icon = Icons.Rounded.RestorePage,
                                        onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("restore_json_button")
                                    )
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val content = onBuildJsonBackup()
                                            val launched = shareBackupToGoogleDrive(
                                                context = context,
                                                fileName = "${sanitizeFileName(businessName)}_backup.json",
                                                jsonContent = content
                                            )
                                            if (!launched) {
                                                onNotifyMessage("Unable to open Google Drive or share sheet")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backup_google_drive_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudUpload,
                                        contentDescription = "Backup to Google Drive",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("Backup to Your Google Drive")
                                }
                                Text(
                                    text = "Saves directly to your own Google Drive account. Use 'Restore Drive/File' to restore from Google Drive anytime.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                OutlinedButton(
                                    onClick = onLoadSampleWorkbook,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("load_sample_workbook_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("Load Sample Finance Entries")
                                }

                                OutlinedButton(
                                    onClick = { showResetConfirmDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_app_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Reset App Data",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("Reset App Data")
                                }
                            }
                        }
                    }

                    // Active Categories Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Categories (${uiState.activeCategories.size})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Add, customize colors, or archive unused categories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { showCategoryDialog = true },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("add_category_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add category",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Add")
                            }
                        }
                    }

                    items(
                        items = uiState.activeCategories,
                        key = { "active_${it.id}" }
                    ) { cat ->
                        CategoryManagerRow(
                            category = cat,
                            isArchived = false,
                            onEdit = { editingCategory = cat },
                            onArchiveToggle = { onSoftDeleteCategory(cat.id) },
                            onDelete = {
                                scope.launch {
                                    deletingCategoryTxCount = onCountCategoryTransactions(cat.id)
                                    deletingCategory = cat
                                }
                            }
                        )
                    }

                    if (uiState.archivedCategories.isNotEmpty()) {
                        item {
                            Text(
                                text = "Archived Categories (${uiState.archivedCategories.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(
                            items = uiState.archivedCategories,
                            key = { "archived_${it.id}" }
                        ) { cat ->
                            CategoryManagerRow(
                                category = cat,
                                isArchived = true,
                                onEdit = { editingCategory = cat },
                                onArchiveToggle = { onRestoreCategory(cat.id) },
                                onDelete = {
                                    scope.launch {
                                        deletingCategoryTxCount = onCountCategoryTransactions(cat.id)
                                        deletingCategory = cat
                                    }
                                }
                            )
                        }
                    }

                    item {
                        AppInfoCard(
                            appName = businessName,
                            onOpenAppInfo = { showAppInfoDialog = true }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DataActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BusinessBrandingCard(
    currentName: String,
    currentSubtitle: String,
    onSaveBranding: (String, String) -> Unit
) {
    var nameInput by remember(currentName) { mutableStateOf(currentName) }
    var subtitleInput by remember(currentSubtitle) { mutableStateOf(currentSubtitle) }
    val hasChanges = nameInput.trim() != currentName || subtitleInput.trim() != currentSubtitle

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("business_branding_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Business,
                    contentDescription = "Business Name",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Business / Workspace Name",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "Customize the app header name to reuse this ledger for another business or account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Business / Ledger Name") },
                placeholder = { Text("e.g., oshanFINANCE or My Shop") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_name_input")
            )

            OutlinedTextField(
                value = subtitleInput,
                onValueChange = { subtitleInput = it },
                label = { Text("Subtitle / Tagline") },
                placeholder = { Text("e.g., LKR Business Finance Ledger") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_subtitle_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onSaveBranding(nameInput, subtitleInput) },
                    enabled = hasChanges && nameInput.isNotBlank(),
                    modifier = Modifier.testTag("save_business_name_button")
                ) {
                    Text("Save Name")
                }
            }
        }
    }
}

@Composable
private fun CategoryManagerRow(
    category: CategoryEntity,
    isArchived: Boolean,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val catColor = CategoryVisuals.parseColor(category.hexColor, MaterialTheme.colorScheme.primary)
    val typeColor = financeColors.colorFor(category.defaultType)
    val typeBg = financeColors.containerFor(category.defaultType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_row_${category.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isArchived) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(catColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryVisuals.iconFor(category.icon),
                    contentDescription = category.name,
                    tint = catColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeBg
                    ) {
                        Text(
                            text = category.defaultType.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (!category.notes.isNullOrBlank()) {
                        Text(
                            text = category.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("category_edit_button_${category.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit ${category.name}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onArchiveToggle,
                    modifier = Modifier.testTag("category_archive_button_${category.id}")
                ) {
                    Icon(
                        imageVector = if (isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = if (isArchived) "Restore ${category.name}" else "Archive ${category.name}",
                        tint = if (isArchived) financeColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("category_delete_button_${category.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete ${category.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

private suspend fun readTextFromUri(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }
        } catch (_: Exception) {
            null
        }
    }

@Composable
private fun SecurityPrivacyCard(
    appLockEnabled: Boolean,
    privacyScreenEnabled: Boolean,
    onToggleAppLock: (Boolean) -> Unit,
    onTogglePrivacyScreen: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("security_privacy_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = "Security and Privacy",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Security & Privacy",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "App Lock (PIN / Biometrics)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Require Fingerprint, Face Unlock, or device PIN when opening the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appLockEnabled,
                    onCheckedChange = onToggleAppLock,
                    modifier = Modifier.testTag("app_lock_switch")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Hide Balances in App Switcher",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Blanks out sensitive financial numbers in Recent Apps and blocks screenshots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = privacyScreenEnabled,
                    onCheckedChange = onTogglePrivacyScreen,
                    modifier = Modifier.testTag("privacy_screen_switch")
                )
            }
        }
    }
}

@Composable
private fun AppInfoCard(
    appName: String,
    onOpenAppInfo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_info_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.modern_finance_logo_1794928820205),
                    contentDescription = appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "$appName • App v1.0",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Developed by Oshan Dimantha",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenAppInfo,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("app_info_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "App Info",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("App Info", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun sanitizeFileName(name: String): String {
    return name.trim()
        .replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        .ifBlank { "FinanceTracker" }
}

private suspend fun shareBackupToGoogleDrive(
    context: Context,
    fileName: String,
    jsonContent: String
): Boolean {
    val fileUri = withContext(Dispatchers.IO) {
        try {
            val backupDir = java.io.File(context.cacheDir, "backups").apply { mkdirs() }
            val outFile = java.io.File(backupDir, fileName)
            outFile.writeText(jsonContent, Charsets.UTF_8)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
        } catch (_: Exception) {
            null
        }
    } ?: return false

    return try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            clipData = android.content.ClipData.newRawUri(fileName, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Prefer Google Drive directly if installed on the user's device; otherwise open system chooser
        val driveIntent = Intent(sendIntent).apply {
            setPackage("com.google.android.apps.docs")
        }
        if (driveIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(driveIntent)
        } else {
            val chooser = Intent.createChooser(sendIntent, "Save backup to Google Drive").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
        true
    } catch (_: Exception) {
        false
    }
}
