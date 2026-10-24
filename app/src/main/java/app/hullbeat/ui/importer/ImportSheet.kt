package app.hullbeat.ui.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import app.hullbeat.ui.theme.SheetSystemBars
import app.hullbeat.ui.theme.appFlingBehavior
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hullbeat.R
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.domain.importer.ImportEngine
import app.hullbeat.domain.importer.TargetField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(
    vesselId: Long,
    database: AppDatabase,
    onDismiss: () -> Unit,
    onImportFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var preview by remember { mutableStateOf<ImportEngine.ImportPreview?>(null) }
    var mapping by remember { mutableStateOf<Map<Int, TargetField>>(emptyMap()) }
    var analysis by remember { mutableStateOf<ImportEngine.ImportAnalysis?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }

    /** Builds the component name set needed by analyzeRows() without touching Room. */
    suspend fun loadComponentNames(): Set<String> {
        val comps = database.componentDao().forVessel(vesselId)
        return comps.flatMap { c ->
            listOfNotNull(c.name, c.customName, c.catalogCode)
        }.toSet()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val fileName = queryFileName(context, uri)
                        val p = ImportEngine.createPreview(stream, fileName)
                        val knownNames = loadComponentNames()
                        val a = ImportEngine.analyzeRows(p, p.suggestedMapping, knownNames)
                        withContext(Dispatchers.Main) {
                            preview = p
                            mapping = p.suggestedMapping
                            analysis = a
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, e.localizedMessage ?: "Read error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Re-run analysis whenever the user changes column mapping
    fun refreshAnalysis(newMapping: Map<Int, TargetField>) {
        val p = preview ?: return
        scope.launch(Dispatchers.IO) {
            val knownNames = loadComponentNames()
            val a = ImportEngine.analyzeRows(p, newMapping, knownNames)
            withContext(Dispatchers.Main) {
                analysis = a
            }
        }
    }

    // Confirmation dialog shown when there are problematic rows
    if (showSkipDialog) {
        val p = preview!!
        val a = analysis!!
        val skipIndices = a.issues.map { it.rowIndex }.toSet()
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.import_skip_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = context.resources.getQuantityString(
                            R.plurals.import_skip_dialog_subtitle,
                            a.issues.size,
                            a.issues.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    a.issues.forEach { issue ->
                        val detail = buildString {
                            if (issue.hasDateError) append(
                                stringResource(R.string.import_issue_date, issue.rawDate)
                            )
                            if (issue.hasDateError && issue.hasComponentError) append(", ")
                            if (issue.hasComponentError) append(
                                stringResource(R.string.import_issue_component, issue.rawComponent)
                            )
                        }
                        Text(
                            text = stringResource(R.string.import_issue_row, issue.rowIndex, detail),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSkipDialog = false
                        isImporting = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val result = ImportEngine.commitImport(
                                    vesselId = vesselId,
                                    preview = p,
                                    mapping = mapping,
                                    db = database,
                                    currency = AppPreferences.getCurrencyCode(context),
                                    skipRows = skipIndices,
                                )
                                withContext(Dispatchers.Main) {
                                    isImporting = false
                                    val msg = buildString {
                                        append(
                                            context.resources.getQuantityString(
                                                R.plurals.import_success_toast,
                                                result.importedRecords,
                                                result.importedRecords,
                                            )
                                        )
                                        if (result.skippedRows > 0) {
                                            append(" (")
                                            append(
                                                context.resources.getQuantityString(
                                                    R.plurals.import_skipped_rows,
                                                    result.skippedRows,
                                                    result.skippedRows,
                                                )
                                            )
                                            append(")")
                                        }
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    onImportFinished()
                                    onDismiss()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isImporting = false
                                    Toast.makeText(context, e.localizedMessage ?: "Import failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    // Two numbers, one plural: the noun follows the total
                    // ("з 21 рядка", "of 1 row"), so the total picks the form.
                    Text(
                        context.resources.getQuantityString(
                            R.plurals.import_skip_dialog_confirm,
                            a.totalRows,
                            a.validCount,
                            a.totalRows,
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text(stringResource(R.string.import_skip_dialog_cancel))
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        SheetSystemBars()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            flingBehavior = appFlingBehavior(),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.import_sheet_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.import_preview_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (preview == null) {
                item(key = "pick_file") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_log),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Button(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf("text/*", "text/csv", "text/comma-separated-values", "text/tab-separated-values")
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.settings_import_btn))
                        }
                    }
                }
            } else {
                val p = preview!!
                item(key = "file_info") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = p.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.import_rows_count,
                                        p.totalRowCount,
                                        p.totalRowCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf("text/*", "text/csv", "text/comma-separated-values", "text/tab-separated-values")
                                    )
                                },
                            ) {
                                Text(stringResource(R.string.import_file_label))
                            }
                        }
                    }
                }

                // Warning banner when some rows have unrecognised data
                val a = analysis
                if (a != null && a.hasAnyIssue) {
                    item(key = "issues_banner") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = context.resources.getQuantityString(
                                        R.plurals.import_issues_banner_title,
                                        a.issues.size,
                                        a.issues.size,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    text = stringResource(R.string.import_issues_banner_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }

                item(key = "mapping_title") {
                    Text(
                        text = stringResource(R.string.import_preview_subtitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Columns mapping selector
                p.headers.forEachIndexed { colIndex, headerName ->
                    item(key = "col_$colIndex") {
                        val currentTarget = mapping[colIndex] ?: TargetField.IGNORE
                        ColumnMappingRow(
                            columnIndex = colIndex,
                            headerName = headerName.ifEmpty { "Col ${colIndex + 1}" },
                            selectedField = currentTarget,
                            onFieldSelected = { newField ->
                                val newMapping = mapping.toMutableMap().apply { put(colIndex, newField) }
                                mapping = newMapping
                                refreshAnalysis(newMapping)
                            },
                        )
                    }
                }

                item(key = "commit_action") {
                    Spacer(Modifier.height(8.dp))
                    if (isImporting) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val currentAnalysis = analysis
                        val hasIssues = currentAnalysis?.hasAnyIssue == true
                        val canCommit = currentAnalysis?.canCommit != false

                        Button(
                            onClick = {
                                if (hasIssues) {
                                    // Show the skip-rows confirmation dialog first
                                    showSkipDialog = true
                                } else {
                                    // All rows are clean — commit immediately
                                    isImporting = true
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val result = ImportEngine.commitImport(
                                                vesselId = vesselId,
                                                preview = p,
                                                mapping = mapping,
                                                db = database,
                                                currency = AppPreferences.getCurrencyCode(context),
                                            )
                                            withContext(Dispatchers.Main) {
                                                isImporting = false
                                                Toast.makeText(
                                                    context,
                                                    context.resources.getQuantityString(
                                                        R.plurals.import_success_toast,
                                                        result.importedRecords,
                                                        result.importedRecords,
                                                    ),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onImportFinished()
                                                onDismiss()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isImporting = false
                                                Toast.makeText(context, e.localizedMessage ?: "Import failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = canCommit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                if (hasIssues) {
                                    stringResource(R.string.import_btn_commit_with_issues)
                                } else {
                                    stringResource(R.string.import_btn_commit)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ColumnMappingRow(
    columnIndex: Int,
    headerName: String,
    selectedField: TargetField,
    onFieldSelected: (TargetField) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = headerName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(selectedField.titleRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedField == TargetField.IGNORE) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "\u25BE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                TargetField.entries.forEach { field ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(field.titleRes),
                                fontWeight = if (field == selectedField) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onFieldSelected(field)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun queryFileName(context: Context, uri: Uri): String {
    var name = "imported.csv"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex) ?: name
            }
        }
    }
    return name
}