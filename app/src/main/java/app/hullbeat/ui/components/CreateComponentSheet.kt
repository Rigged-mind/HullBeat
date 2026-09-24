package app.hullbeat.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.hullbeat.R
import app.hullbeat.data.catalog.CatalogLoader
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.search.SearchRepository
import kotlinx.coroutines.launch

/**
 * Bottom sheet for adding a custom component (docs/search-spec.md §5).
 *
 * Triggered from:
 * 1. Search empty state: "Створити вузол «{query}»"
 * 2. Component tree: "+ Власний вузол"
 *
 * Persists with catalogCode = null, isCustom = true and re-indexes into search_index.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateComponentSheet(
    vesselId: Long,
    database: AppDatabase,
    initialName: String = "",
    onDismiss: () -> Unit,
    onCreated: (componentId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val searchRepo = remember { SearchRepository(database, context) }

    var name by rememberSaveable { mutableStateOf(initialName) }
    var selectedCategoryCode by rememberSaveable { mutableStateOf("other") }
    var criticalityName by rememberSaveable { mutableStateOf(Criticality.MED.name) }
    val criticality = runCatching { Criticality.valueOf(criticalityName) }.getOrDefault(Criticality.MED)
    var notes by rememberSaveable { mutableStateOf("") }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    val catalog = remember { CatalogLoader.get(context) }
    val categories = catalog.categories

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.create_component_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.create_component_name)) },
                placeholder = { Text(stringResource(R.string.create_component_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                text = stringResource(R.string.create_component_criticality),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Criticality.entries.forEach { crit ->
                    val labelRes = when (crit) {
                        Criticality.HIGH -> R.string.crit_high
                        Criticality.MED -> R.string.crit_med
                        Criticality.LOW -> R.string.crit_low
                    }
                    FilterChip(
                        selected = criticality == crit,
                        onClick = { criticalityName = crit.name },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.create_component_category),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // Category selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val primaryCats = listOf("engine_main", "elec_dc", "hull", "other")
                primaryCats.forEach { code ->
                    val catName = searchRepo.categoryNameFor(code)
                    FilterChip(
                        selected = selectedCategoryCode == code,
                        onClick = { selectedCategoryCode = code },
                        label = { Text(catName) },
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.create_component_notes)) },
                placeholder = { Text(stringResource(R.string.create_component_notes_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val canSave = name.trim().length >= 2 && !isSaving

            Button(
                onClick = {
                    if (canSave) {
                        isSaving = true
                        scope.launch {
                            val comp = Component(
                                vesselId = vesselId,
                                categoryCode = selectedCategoryCode,
                                catalogCode = null,
                                name = name.trim(),
                                customName = name.trim(),
                                isCustom = true,
                                criticality = criticality,
                                notes = notes.trim().ifEmpty { null },
                            )
                            val newId = database.componentDao().insert(comp)
                            val inserted = comp.copy(id = newId)
                            searchRepo.indexComponent(inserted)
                            onCreated(newId)
                        }
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(R.string.create_component_action))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
