package app.hullbeat.ui.boat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import app.hullbeat.ui.theme.appFlingBehavior
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.hullbeat.R
import app.hullbeat.data.catalog.CoreComponents
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.categoryDisplayName
import app.hullbeat.data.db.categoryIconRes
import app.hullbeat.data.db.displayName
import androidx.compose.foundation.background
import app.hullbeat.ui.components.AddFromCatalogSheet
import app.hullbeat.ui.components.rememberSampledBitmap
import app.hullbeat.ui.theme.LocalScheme
import app.hullbeat.ui.theme.Scheme
import app.hullbeat.ui.theme.categorySectionColor
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
    var collapsedCategories by remember {
        mutableStateOf(AppPreferences.getCollapsedCategories(context))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        collapsedCategories = AppPreferences.getCollapsedCategories(context)
        reload++
    }

    LaunchedEffect(reload, vesselId) {
        collapsedCategories = AppPreferences.getCollapsedCategories(context)
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

    val strCollapsed = stringResource(R.string.state_collapsed)
    val strExpanded = stringResource(R.string.state_expanded)

    var showAllIconsOverview by rememberSaveable { mutableStateOf(false) }

    if (showAllIconsOverview || searchQuery.trim().lowercase() == "icons") {
        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Розділи застосунку",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(onClick = { showAllIconsOverview = false }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Закрити",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val navItems = listOf(
                    R.drawable.ic_nav_now to stringResource(R.string.nav_now),
                    R.drawable.ic_nav_boat to stringResource(R.string.nav_boat),
                    R.drawable.ic_nav_log to stringResource(R.string.nav_log),
                    R.drawable.ic_nav_store to stringResource(R.string.nav_store),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    navItems.forEach { (iconRes, name) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Категорії вузлів (21)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                val allCategoryCodes = listOf(
                    "hull", "drivetrain", "engine_main",
                    "genset", "fuel", "elec_dc",
                    "elec_ac", "plumbing", "gas",
                    "standing_rig", "running_rig", "sails",
                    "ground_tackle", "safety", "nav",
                    "hvac", "galley", "tender",
                    "trailer", "documents", "other"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(allCategoryCodes) { code ->
                        val secColor = categorySectionColor(code)
                        val isColorful = LocalScheme.current == Scheme.COLORFUL
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = secColor.container,
                            border = BorderStroke(
                                1.dp,
                                if (isColorful) secColor.accent.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(categoryIconRes(code)),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = secColor.accent,
                                )
                                Text(
                                    text = categoryDisplayName(context, code),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isColorful) secColor.onContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            flingBehavior = appFlingBehavior(),
        ) {
            item(key = "header") {
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
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
            }

            // Search Bar & Add Button
            item(key = "search_and_actions") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
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
                            .padding(top = 16.dp, bottom = 32.dp),
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
                val isSearching = searchQuery.isNotBlank()
                val grouped = filteredComponents.groupBy { it.categoryCode }
                grouped.entries.forEachIndexed { groupIndex, (catCode, rows) ->
                    val isCollapsed = !isSearching && catCode in collapsedCategories
                    val topPadding = if (groupIndex > 0) 16.dp else 8.dp

                    item(key = "cat_header_$catCode") {
                        val headerShape = if (isCollapsed || rows.isEmpty()) {
                            RoundedCornerShape(12.dp)
                        } else {
                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        }
                        val secColor = categorySectionColor(catCode)
                        val isColorful = LocalScheme.current == Scheme.COLORFUL

                        Surface(
                            shape = headerShape,
                            color = if (isColorful) secColor.container else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isColorful) BorderStroke(1.dp, secColor.accent.copy(alpha = 0.35f)) else null,
                            onClick = {
                                if (!isSearching) {
                                    val next = if (catCode in collapsedCategories) {
                                        collapsedCategories - catCode
                                    } else {
                                        collapsedCategories + catCode
                                    }
                                    collapsedCategories = next
                                    AppPreferences.setCollapsedCategories(context, next)
                                }
                            },
                            enabled = !isSearching,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = topPadding)
                                .semantics(mergeDescendants = true) {
                                    heading()
                                    if (!isSearching) {
                                        role = Role.Button
                                        stateDescription = if (isCollapsed) strCollapsed else strExpanded
                                    }
                                },
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    color = if (isColorful) secColor.accent else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(categoryIconRes(catCode)),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (isColorful) Color.White else secColor.accent,
                                            )
                                        }
                                        Text(
                                            text = categoryDisplayName(context, catCode),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isColorful) secColor.onContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isColorful) secColor.accent else MaterialTheme.colorScheme.surface,
                                        ) {
                                            Text(
                                                text = rows.size.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isColorful) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    if (!isSearching) {
                                        Icon(
                                            painter = painterResource(
                                                if (isCollapsed) R.drawable.ic_chevron_right else R.drawable.ic_chevron_down
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isColorful) secColor.onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (!isCollapsed && rows.isNotEmpty()) {
                                    HorizontalDivider(
                                        color = if (isColorful) secColor.accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (!isCollapsed) {
                        itemsIndexed(rows, key = { _, c -> "c" + c.id }) { index, component ->
                            val secColor = categorySectionColor(catCode)
                            val isColorful = LocalScheme.current == Scheme.COLORFUL
                            val isLast = index == rows.lastIndex
                            val shape = if (isLast) {
                                RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            } else {
                                RectangleShape
                            }
                            ComponentRow(
                                label = component.displayName(context),
                                onClick = { onOpenComponent(component.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = shape,
                                showDivider = !isLast,
                                accentColor = if (isColorful) secColor.accent else null,
                            )
                        }
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
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    showDivider: Boolean = false,
    accentColor: Color? = null,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    if (accentColor != null) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(16.dp)
                                .background(accentColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

