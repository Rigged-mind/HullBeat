package app.hullbeat.ui.setup

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.hullbeat.R
import app.hullbeat.data.catalog.CatalogLoader
import app.hullbeat.data.catalog.VesselSetup
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.profileOptionLabel
import kotlinx.coroutines.launch

/**
 * Labels for a profile axis and its values.
 *
 * ⚠️ Resolved by name, so `res/raw/keep.xml` lists `@string/axis_*` and
 * `@string/opt_*`. `tools/profile_labels.py` generates both locales and
 * fails if the catalog's vocabulary and the label table disagree, so a raw
 * code like `saildrive` can never reach a person.
 */
private fun labelFor(context: Context, name: String, fallback: String): String {
    val id = context.resources.getIdentifier(name, "string", context.packageName)
    return if (id != 0) context.getString(id) else fallback
}

private fun axisLabel(context: Context, axis: String) =
    labelFor(context, "axis_$axis", axis)

// Delegates rather than repeats: two copies of this lookup drift, and the
// copy that drifts is always the one nobody is looking at.
private fun optionLabel(context: Context, axis: String, value: String) =
    profileOptionLabel(context, axis, value)

private val SAIL_HULLS = setOf("monohull_sail", "catamaran_sail", "trimaran_sail")
private val INBOARD = setOf("shaft", "saildrive", "sterndrive")

/**
 * The first thing an owner sees, and the reason the catalog is worth having.
 *
 * Asked in the order the answers depend on each other: the hull decides
 * whether there is a rig and a keel to ask about, and the drive decides
 * whether cooling is a question at all. An axis that cannot apply is not
 * shown with a "none" option to pick - it is not shown.
 */
@Composable
fun VesselSetupScreen(
    database: AppDatabase,
    onReady: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vocabulary = remember { CatalogLoader.get(context).profileVocabulary }

    var name by remember { mutableStateOf("") }
    var hull by remember { mutableStateOf("monohull_sail") }
    var engine by remember { mutableStateOf("diesel") }
    var drive by remember { mutableStateOf("saildrive") }
    var cooling by remember { mutableStateOf("raw_water") }
    var rig by remember { mutableStateOf("sloop") }
    var keel by remember { mutableStateOf("fin") }
    var storage by remember { mutableStateOf("hauled_winter") }
    var water by remember { mutableStateOf("salt") }
    var extras by remember { mutableStateOf(setOf("cabin", "fresh_water")) }
    var saving by remember { mutableStateOf(false) }

    val isSail = hull in SAIL_HULLS
    val hasEngine = engine != "none"
    val isInboard = drive in INBOARD

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            if (onDismiss != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.setup_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Axis(context, "hull", vocabulary["hull"], hull) { hull = it }
            Axis(context, "engine", vocabulary["engine"], engine) { engine = it }
            if (hasEngine) {
                Axis(context, "drive", vocabulary["drive"], drive) { drive = it }
                if (isInboard) {
                    Axis(context, "cooling", vocabulary["cooling"], cooling) { cooling = it }
                }
            }
            if (isSail) {
                Axis(context, "rig", vocabulary["rig"], rig) { rig = it }
                Axis(context, "keel", vocabulary["keel"], keel) { keel = it }
            }
            Axis(context, "storage", vocabulary["storage"], storage) { storage = it }
            Axis(context, "water", vocabulary["water"], water) { water = it }

            MultiAxis(context, "extras", vocabulary["extras"], extras) { value ->
                extras = if (value in extras) extras - value else extras + value
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    val vessel = Vessel(
                        name = name.trim().ifEmpty {
                            context.getString(R.string.setup_default_name)
                        },
                        hullType = hull,
                        engine = engine,
                        drive = if (hasEngine) drive else "none",
                        cooling = if (hasEngine && isInboard) cooling else "raw_water",
                        rigType = if (isSail) rig else "none",
                        keelType = if (isSail) keel else "none",
                        extras = extras.sorted().joinToString(","),
                        storage = storage,
                        water = water,
                    )
                    scope.launch {
                        onReady(VesselSetup.create(context, database, vessel))
                    }
                },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(R.string.setup_action))
            }
            if (onDismiss != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** One axis, one answer. Full-width rows: this is read at a pontoon, in gloves. */
@Composable
private fun Axis(
    context: Context,
    axis: String,
    values: List<String>?,
    selected: String,
    onSelect: (String) -> Unit,
) {
    if (values.isNullOrEmpty()) return
    Spacer(Modifier.height(20.dp))
    Text(
        text = axisLabel(context, axis),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(optionLabel(context, axis, value)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            )
        }
    }
}

/** The extras axis: a set, not a choice. */
@Composable
private fun MultiAxis(
    context: Context,
    axis: String,
    values: List<String>?,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    if (values.isNullOrEmpty()) return
    Spacer(Modifier.height(20.dp))
    Text(
        text = axisLabel(context, axis),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = value in selected,
                onClick = { onToggle(value) },
                label = { Text(optionLabel(context, axis, value)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            )
        }
    }
}
