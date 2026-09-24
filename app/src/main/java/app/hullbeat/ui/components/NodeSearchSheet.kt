package app.hullbeat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hullbeat.R
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.search.SearchRepository
import app.hullbeat.data.search.SearchSuggestion
import app.hullbeat.domain.search.MatchSource
import kotlinx.coroutines.delay

/**
 * Bottom sheet for component search with 150ms debounce and suggestions (docs/search-spec.md §5).
 * Shows canonical title, small category name, and "via" attribute.
 * If empty: presents "Створити вузол «{query}»".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeSearchSheet(
    vesselId: Long,
    database: AppDatabase,
    onDismiss: () -> Unit,
    onSelectComponent: (componentId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val searchRepo = remember { SearchRepository(database, context) }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(150) // 150ms debounce per search-spec.md §5
        results = searchRepo.search(vesselId, trimmed, limit = 8)
        isSearching = false
    }

    if (showCreateSheet) {
        CreateComponentSheet(
            vesselId = vesselId,
            database = database,
            initialName = query.trim(),
            onDismiss = { showCreateSheet = false },
            onCreated = { newId ->
                showCreateSheet = false
                onSelectComponent(newId)
            },
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            } else if (query.trim().length >= 2 && results.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { showCreateSheet = true },
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(stringResource(R.string.create_component_empty_search, query.trim()))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(results, key = { it.component.id }) { item ->
                        SearchSuggestionRow(
                            item = item,
                            onClick = { onSelectComponent(item.component.id) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SearchSuggestionRow(
    item: SearchSuggestion,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val viaText = when (item.source) {
                    MatchSource.SPEC_OR_NOTES -> {
                        if (item.via == "spec") stringResource(R.string.search_found_in_spec)
                        else stringResource(R.string.search_found_in_notes)
                    }
                    MatchSource.EXACT_SYNONYM, MatchSource.PREFIX_SYNONYM, MatchSource.CONTAINS_SYNONYM -> {
                        item.via?.let { stringResource(R.string.search_found_via, it) }
                    }
                    MatchSource.EXACT_EN_TITLE, MatchSource.PREFIX_EN_TITLE, MatchSource.WORD_PREFIX_EN_TITLE -> {
                        item.via?.let { stringResource(R.string.search_found_via, it) }
                    }
                    else -> null
                }
                val subText = if (viaText != null) "${item.categoryName} · $viaText" else item.categoryName
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "→",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
