package app.hullbeat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hullbeat.HullBeatApp
import app.hullbeat.R
import app.hullbeat.data.catalog.ChecklistsLoader
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.preferences.AppPreferences
import app.hullbeat.ui.boat.BoatScreen
import app.hullbeat.ui.components.CreateComponentSheet
import app.hullbeat.ui.components.NodeSearchSheet
import app.hullbeat.ui.components.VesselSelectSheet
import app.hullbeat.ui.detail.ComponentDetailScreen
import app.hullbeat.ui.detail.ComponentDetailViewModel
import app.hullbeat.ui.journal.JournalScreen
import app.hullbeat.ui.journal.JournalViewModel
import app.hullbeat.ui.now.DepartureChecklistSheet
import app.hullbeat.ui.now.NowScreen
import app.hullbeat.ui.now.NowViewModel
import app.hullbeat.ui.settings.SettingsSheet
import app.hullbeat.ui.setup.VesselSetupScreen
import app.hullbeat.ui.store.StoreScreen
import app.hullbeat.ui.store.StoreViewModel
import app.hullbeat.ui.theme.Scheme

enum class NavTab(val titleRes: Int, val iconRes: Int) {
    NOW(R.string.nav_now, R.drawable.ic_nav_now),
    BOAT(R.string.nav_boat, R.drawable.ic_nav_boat),
    LOG(R.string.nav_log, R.drawable.ic_nav_log),
    STORE(R.string.nav_store, R.drawable.ic_nav_store),
}

/**
 * Root app scaffold providing the 4-destination bottom bar (ui-spec.md §2).
 *
 * [ Пульс ]   [ Човен ]   [ Журнал ]   [ Склад ]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HullBeatMainScaffold(
    nowViewModel: NowViewModel,
    currentScheme: Scheme,
    onCycleScheme: () -> Unit,
    modifier: Modifier = Modifier,
    onSchemeChanged: ((Scheme) -> Unit)? = null,
    pendingComponentId: Long? = null,
    pendingRenew: Boolean = false,
    database: AppDatabase? = null,
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavTab.NOW) }
    val nowState by nowViewModel.uiState.collectAsStateWithLifecycle()
    val setupContext = LocalContext.current
    var currentCurrency by remember { mutableStateOf(AppPreferences.getCurrency(setupContext)) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showVesselSheet by rememberSaveable { mutableStateOf(false) }
    var showAddVesselScreen by rememberSaveable { mutableStateOf(false) }

    // No boat, nothing to show. Every catalog rule is judged against a
    // profile, so until one exists the app has literally nothing to say -
    // which is exactly what the first build on a phone showed: an empty
    // screen and a button to type a node in by hand.
    val setupDb = database ?: (setupContext.applicationContext as HullBeatApp).database
    var vesselCount by remember { mutableStateOf<Int?>(null) }
    var justCreated by remember { mutableLongStateOf(0L) }
    LaunchedEffect(justCreated) {
        vesselCount = setupDb.vesselDao().allActive().size
    }
    if (vesselCount == 0) {
        VesselSetupScreen(
            database = setupDb,
            onReady = {
                justCreated = it
                nowViewModel.refresh()
            },
            modifier = modifier,
        )
        return
    }
    if (vesselCount == null) return

    if (showAddVesselScreen) {
        VesselSetupScreen(
            database = setupDb,
            onReady = { newVesselId ->
                showAddVesselScreen = false
                justCreated = newVesselId
                nowViewModel.selectVessel(newVesselId)
                nowViewModel.refresh()
            },
            onDismiss = {
                showAddVesselScreen = false
            },
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    var activeComponentId by rememberSaveable { mutableStateOf<Long?>(null) }
    var activeComponentInitialRenew by rememberSaveable { mutableStateOf(false) }
    var activeComponentOriginTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var showSearchSheet by rememberSaveable { mutableStateOf(false) }
    var pendingOpenLogOnSelect by rememberSaveable { mutableStateOf(false) }
    var showCreateComponentSheet by rememberSaveable { mutableStateOf(false) }
    var showDepartureChecklistSheet by rememberSaveable { mutableStateOf(false) }
    var departureVesselId by rememberSaveable { mutableStateOf<Long?>(null) }
    val navNowTitle = stringResource(R.string.nav_now)
    val navBoatTitle = stringResource(R.string.nav_boat)
    val navLogTitle = stringResource(R.string.nav_log)
    val navStoreTitle = stringResource(R.string.nav_store)

    LaunchedEffect(pendingComponentId, pendingRenew) {
        if (pendingComponentId != null && pendingComponentId > 0L) {
            activeComponentId = pendingComponentId
            activeComponentInitialRenew = pendingRenew
            activeComponentOriginTitle = navNowTitle
            selectedTab = NavTab.NOW
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        nowViewModel.refreshIfDateChanged()
    }

    if (activeComponentId != null) {
        val targetId = activeComponentId!!
        val context = LocalContext.current
        val db = database ?: (context.applicationContext as HullBeatApp).database
        val detailViewModel: ComponentDetailViewModel = viewModel(
            key = "component_$targetId",
            factory = ComponentDetailViewModel.provideFactory(targetId, db, context),
        )

        BackHandler {
            activeComponentId = null
        }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            ComponentDetailScreen(
                viewModel = detailViewModel,
                onBack = { activeComponentId = null },
                initialOpenLog = activeComponentInitialRenew,
                backDestinationTitle = activeComponentOriginTitle ?: stringResource(R.string.detail_back),
                onLogSaved = {
                    detailViewModel.loadData()
                },
                onSaveQuickLog = { compId, vesselId, scheduleId, date, workType, meterHours, cost, notes, newExpiry, photoUris, onSaved ->
                    nowViewModel.saveQuickLog(
                        componentId = compId,
                        vesselId = vesselId,
                        scheduleId = scheduleId,
                        date = date,
                        workType = workType,
                        meterHours = meterHours,
                        cost = cost,
                        notes = notes,
                        newExpiry = newExpiry,
                        photoUris = photoUris,
                        onSaved = { _ ->
                            onSaved()
                            detailViewModel.loadData()
                        },
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
            )
        }
        return
    }

    Scaffold(
        topBar = {
            val activeVessels = nowState.vessels.filter { !it.archived }
            val currentVessel = nowState.activeVessel
                ?: activeVessels.firstOrNull { it.id == nowState.selectedVesselId }
                ?: activeVessels.firstOrNull()

            val titleText = if (nowState.selectedVesselId == null && activeVessels.size > 1) {
                stringResource(R.string.fleet_all)
            } else {
                currentVessel?.name ?: stringResource(R.string.app_name)
            }
            val cdSelect = stringResource(R.string.cd_select_vessel, titleText)

            TopAppBar(
                title = {
                    Surface(
                        onClick = { showVesselSheet = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .clearAndSetSemantics {
                                contentDescription = cdSelect
                            },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_nav_boat),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "\u25BE",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            pendingOpenLogOnSelect = false
                            showSearchSheet = true
                        },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.search_hint),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_title),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                NavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = stringResource(tab.titleRes),
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = { Text(stringResource(tab.titleRes)) },
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
            ) {
                when (selectedTab) {
                    NavTab.NOW -> {
                        NowScreen(
                            viewModel = nowViewModel,
                            uiState = nowState,
                            pendingComponentId = pendingComponentId,
                            pendingRenew = pendingRenew,
                            onOpenComponent = { compId ->
                                activeComponentId = compId
                                activeComponentInitialRenew = false
                                activeComponentOriginTitle = navNowTitle
                            },
                            onOpenChecklist = {
                                departureVesselId = null
                                showDepartureChecklistSheet = true
                            },
                            onOpenSearch = {
                                pendingOpenLogOnSelect = true
                                showSearchSheet = true
                            },
                            onNavigateToLog = { selectedTab = NavTab.LOG },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    NavTab.BOAT -> {
                        BoatScreen(
                            database = setupDb,
                            vesselId = nowState.activeVessel?.id,
                            onOpenComponent = { compId ->
                                activeComponentId = compId
                                activeComponentInitialRenew = false
                                activeComponentOriginTitle = navBoatTitle
                            },
                            onAddCustom = { showCreateComponentSheet = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    NavTab.LOG -> {
                        val context = LocalContext.current
                        val logDb = database ?: (context.applicationContext as HullBeatApp).database
                        val journalViewModel: JournalViewModel = viewModel(
                            factory = JournalViewModel.provideFactory(logDb, context),
                        )
                        LaunchedEffect(nowState.selectedVesselId) {
                            journalViewModel.selectVessel(nowState.selectedVesselId)
                        }
                        JournalScreen(
                            viewModel = journalViewModel,
                            onSelectVessel = { nowViewModel.selectVessel(it) },
                            onOpenComponent = { compId ->
                                activeComponentId = compId
                                activeComponentInitialRenew = false
                                activeComponentOriginTitle = navLogTitle
                            },
                            currency = currentCurrency,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    NavTab.STORE -> {
                        val context = LocalContext.current
                        val storeDb = database ?: (context.applicationContext as HullBeatApp).database
                        val vessel = nowState.activeVessel
                        if (vessel != null) {
                            val storeViewModel: StoreViewModel = viewModel(
                                key = "store_${vessel.id}",
                                factory = StoreViewModel.provideFactory(storeDb, vessel.id),
                            )
                            StoreScreen(
                                viewModel = storeViewModel,
                                onOpenComponent = { compId ->
                                    activeComponentId = compId
                                    activeComponentInitialRenew = false
                                    activeComponentOriginTitle = navStoreTitle
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }

        if (showSearchSheet) {
            val context = LocalContext.current
            val targetDb = database ?: (context.applicationContext as HullBeatApp).database
            val vessel = nowState.activeVessel
            if (vessel != null) {
                NodeSearchSheet(
                    vesselId = vessel.id,
                    database = targetDb,
                    onDismiss = {
                        showSearchSheet = false
                        pendingOpenLogOnSelect = false
                    },
                    onSelectComponent = { compId ->
                        val openLog = pendingOpenLogOnSelect
                        showSearchSheet = false
                        pendingOpenLogOnSelect = false
                        activeComponentId = compId
                        activeComponentInitialRenew = openLog
                        activeComponentOriginTitle = navNowTitle
                    },
                )
            }
        }

        if (showCreateComponentSheet) {
            val context = LocalContext.current
            val targetDb = database ?: (context.applicationContext as HullBeatApp).database
            val vessel = nowState.activeVessel
            if (vessel != null) {
                CreateComponentSheet(
                    vesselId = vessel.id,
                    database = targetDb,
                    initialName = "",
                    onDismiss = { showCreateComponentSheet = false },
                    onCreated = { newId ->
                        showCreateComponentSheet = false
                        activeComponentId = newId
                        activeComponentInitialRenew = false
                        activeComponentOriginTitle = navBoatTitle
                    },
                )
            }
        }

        if (showDepartureChecklistSheet) {
            val context = LocalContext.current
            // A boat switched to inside the sheet wins; otherwise the default
            // the state picks - the selected boat, or the one that left last.
            val vessel = nowState.vessels.firstOrNull { it.id == departureVesselId }
                ?: nowState.departureDefaultVessel
            if (vessel != null) {
                val items = remember(vessel.id) {
                    ChecklistsLoader.predepartureItemsFor(context, vessel)
                }
                DepartureChecklistSheet(
                    vessel = vessel,
                    items = items,
                    isDoneToday = vessel.id in nowState.departureDoneVesselIds,
                    vessels = if (nowState.isFleetView) nowState.vessels else emptyList(),
                    doneVesselIds = nowState.departureDoneVesselIds,
                    onSelectVessel = { departureVesselId = it },
                    onDismiss = { showDepartureChecklistSheet = false },
                    onConfirm = {
                        showDepartureChecklistSheet = false
                        nowViewModel.markDepartureCheckDone(vessel.id)
                    },
                    onReset = {
                        showDepartureChecklistSheet = false
                        nowViewModel.resetDepartureCheck(vessel.id)
                    },
                )
            }
        }

        if (showSettingsSheet) {
            SettingsSheet(
                currentCurrency = currentCurrency,
                onCurrencyChanged = { newCurrency ->
                    currentCurrency = newCurrency
                },
                currentScheme = currentScheme,
                onSchemeChanged = { newScheme ->
                    onSchemeChanged?.invoke(newScheme) ?: onCycleScheme()
                },
                activeVesselId = nowState.activeVessel?.id,
                onDismiss = { showSettingsSheet = false },
            )
        }

        if (showVesselSheet) {
            val overdueCounts = remember(nowState.items) {
                nowState.items.filter { it.isOverdue }.groupBy { it.vessel.id }.mapValues { it.value.size }
            }
            val soonCounts = remember(nowState.items) {
                nowState.items.filter { it.isDueSoon }.groupBy { it.vessel.id }.mapValues { it.value.size }
            }
            val activeVessels = nowState.vessels.filter { !it.archived }

            VesselSelectSheet(
                vessels = activeVessels,
                selectedVesselId = nowState.selectedVesselId,
                overdueCounts = overdueCounts,
                soonCounts = soonCounts,
                onSelectVessel = { vesselId ->
                    nowViewModel.selectVessel(vesselId)
                },
                onAddVessel = {
                    showAddVesselScreen = true
                },
                onDismiss = {
                    showVesselSheet = false
                },
            )
        }
    }
}

@Composable
private fun TabPlaceholder(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.Spacer(Modifier.heightIn(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionText != null && onAction != null) {
                androidx.compose.foundation.layout.Spacer(Modifier.heightIn(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

