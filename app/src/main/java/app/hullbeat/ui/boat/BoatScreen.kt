package app.hullbeat.ui.boat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.hullbeat.data.storage.AttachmentStorage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hullbeat.R
import app.hullbeat.data.catalog.CoreComponents
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.categoryDisplayName
import app.hullbeat.data.db.displayName
import app.hullbeat.ui.components.AddFromCatalogSheet
import app.hullbeat.ui.components.rememberSampledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The boat, showing only core components, components selected by the owner,
 * and components with service history or passport data.
 */
@Composable
fun BoatScreen(
    database: AppDatabase,
    onOpenComponent: (Long) -> Unit,
    onAddCustom: () -> Unit,
    modifier: Modifier = Modifier,
    vesselId: Long? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var vessel by remember { mutableStateOf<Vessel?>(null) }
    var components by remember { mutableStateOf<List<Component>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddCatalogSheet by rememberSaveable { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload, vesselId) {
        withContext(Dispatchers.IO) {
            val target = if (vesselId != null) {
                database.vesselDao().byId(vesselId)
            } else {
                database.vesselDao().allActive().firstOrNull()
            }
            if (target != null) {
                // If existing vessel has > 50 active components, optimize once
                val currentRows = database.componentDao().forVessel(target.id)
                if (currentRows.size > 50) {
                    val servicedIds = database.componentDao().componentIdsWithService().toSet()
                    val toArchive = mutableListOf<Long>()
                    for (c in currentRows) {
                        val isCore = CoreComponents.isCore(c.catalogCode)
                        val isServiced = c.id in servicedIds
                        val isCustom = c.isCustom
                        val hasPassport = !c.make.isNullOrBlank() ||
                            !c.model.isNullOrBlank() ||
                            !c.serial.isNullOrBlank() ||
                            !c.customName.isNullOrBlank() ||
                            !c.notes.isNullOrBlank()
                        if (!isCore && !isServiced && !isCustom && !hasPassport) {
                            toArchive.add(c.id)
                        }
                    }
                    if (toArchive.isNotEmpty()) {
                        database.componentDao().archiveMany(toArchive)
                    }
                }

                val activeRows = database.componentDao().forVessel(target.id)
                vessel = target
                components = activeRows
            } else {
                vessel = null
                components = emptyList()
            }
        }
    }

    var showPhotoDialog by remember { mutableStateOf(false) }
    // A copy that fails leaves the boat with its old photo and the owner
    // with no idea why the new one did not stick. This screen has no
    // snackbar host, so it says it in a dialog.
    var photoError by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val target = vessel
        if (uri != null && target != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val savedUri = AttachmentStorage.copyToInternalStorage(context, uri, "vessels")
                    if (savedUri != null) {
                        target.photoUri?.let { old ->
                            AttachmentStorage.deleteInternalFile(context, old)
                        }
                        database.vesselDao().update(target.copy(photoUri = savedUri.toString()))
                    } else {
                        photoError = true
                    }
                }
                reload++
            }
        }
    }

    // Filter components by search query
    val filteredComponents = remember(searchQuery, components) {
        if (searchQuery.isBlank()) {
            components
        } else {
            val q = searchQuery.trim().lowercase()
            components.filter { c ->
                c.displayName(context).lowercase().contains(q) ||
                    categoryDisplayName(context, c.categoryCode).lowercase().contains(q)
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                VesselHeader(
                    vessel = vessel,
                    componentCount = components.size,
                    onPickPhoto = {
                        if (vessel?.photoUri != null) {
                            showPhotoDialog = true
                        } else {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                )
            }

            // Search Bar & Add Button
            item(key = "search_and_actions") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.boat_search_hint)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = stringResource(R.string.action_cancel),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Button(
                        onClick = { showAddCatalogSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.boat_add_component),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (filteredComponents.isEmpty()) {
                item(key = "empty_result") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.search_empty)
                                } else {
                                    stringResource(R.string.boat_empty_components)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FilledTonalButton(
                                onClick = { showAddCatalogSheet = true },
                            ) {
                                Text(stringResource(R.string.catalog_add_title))
                            }
                        }
                    }
                }
            } else {
                val grouped = filteredComponents.groupBy { it.categoryCode }
                grouped.forEach { (catCode, rows) ->
                    item(key = "cat_$catCode") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = categoryDisplayName(context, catCode) + "  ·  " + rows.size,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(rows, key = { "c" + it.id }) { component ->
                        ComponentRow(
                            label = component.displayName(context),
                            onClick = { onOpenComponent(component.id) },
                        )
                    }
                }
            }
        }
    }

    // Add From Catalog Bottom Sheet
    if (showAddCatalogSheet && vessel != null) {
        AddFromCatalogSheet(
            vesselId = vessel!!.id,
            database = database,
            onDismiss = { showAddCatalogSheet = false },
            onComponentAdded = { _ ->
                reload++
            },
            onCreateCustom = {
                showAddCatalogSheet = false
                onAddCustom()
            },
        )
    }
    if (photoError) {
        AlertDialog(
            onDismissRequest = { photoError = false },
            text = { Text(stringResource(R.string.error_photo_save_failed)) },
            confirmButton = {
                TextButton(
                    onClick = { photoError = false },
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }

    if (showPhotoDialog && vessel != null) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text(stringResource(R.string.vessel_photo_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showPhotoDialog = false
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_change_photo))
                    }
                    if (vessel?.photoUri != null) {
                        OutlinedButton(
                            onClick = {
                                showPhotoDialog = false
                                val target = vessel
                                if (target != null) {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            target.photoUri?.let { old ->
                                                AttachmentStorage.deleteInternalFile(context, old)
                                            }
                                            database.vesselDao().update(target.copy(photoUri = null))
                                        }
                                        reload++
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_delete_photo), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showPhotoDialog = false },
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun VesselHeader(
    vessel: Vessel?,
    componentCount: Int,
    onPickPhoto: () -> Unit,
) {
    val context = LocalContext.current
    // Bigger than the old 72 dp thumbnail needed: this one is scaled to the
    // full card width, so a 200 px sample would show its own pixels.
    val photo = rememberSampledBitmap(context, vessel?.photoUri, maxDimension = 1200)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clickable { onPickPhoto() },
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = vessel?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // The name sits ON the photo, so it cannot rely on the theme
                // for contrast - the owner's picture might be a white hull
                // against a bright sky. A scrim over the lower half carries
                // the text instead, and it works identically in all four
                // colour schemes because it is part of the image, not the
                // palette.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.70f),
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_boat),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val name = vessel?.name ?: stringResource(R.string.setup_default_name)
            val countLine = "$componentCount " +
                stringResource(R.string.boat_component_count_suffix)
            // Stroke width is in PIXELS, so it has to come from the density
            // or the outline is hairline on one phone and heavy on another.
            val outline = with(LocalDensity.current) { 2.dp.toPx() }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (photo != null) {
                    // A scrim alone is not enough: a white hull against a
                    // bright sky fills the whole frame, and the gradient only
                    // darkens the bottom. The name is drawn twice - once as
                    // an outline, once filled on top - so it holds its shape
                    // whatever the owner photographed.
                    Box {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                drawStyle = Stroke(width = outline, join = StrokeJoin.Round),
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.Black.copy(alpha = 0.75f),
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Text(
                        text = countLine,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            // A shadow here, not a stroke: at this size an
                            // outline closes the counters of о, в and а and
                            // the line turns into a smudge.
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.85f),
                                offset = Offset(0f, 1f),
                                blurRadius = 3f,
                            ),
                        ),
                        color = Color.White,
                    )
                } else {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = countLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentRow(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}
