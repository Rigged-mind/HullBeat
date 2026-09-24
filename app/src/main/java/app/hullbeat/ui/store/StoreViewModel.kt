package app.hullbeat.ui.store

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import app.hullbeat.R
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.ComponentPart
import app.hullbeat.data.db.ComponentPartLink
import app.hullbeat.data.db.InventoryItem
import app.hullbeat.data.db.InventoryItemDetail
import app.hullbeat.data.db.Part
import app.hullbeat.data.db.StorageLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StoreTab {
    ALL,
    TO_BUY,
    LOCATIONS,
}

data class StoreItemUiModel(
    val detail: InventoryItemDetail,
    val linkedComponents: List<ComponentPartLink> = emptyList(),
) {
    val isOutOfStock: Boolean
        get() = detail.quantity <= 0.0

    /**
     * Low once half the norm is gone.
     *
     * `minQuantity` is the column name, kept so no migration is needed, but the
     * number the owner types is a norm - how many to hold aboard - not a floor.
     * Read as a floor with `quantity <= minQuantity` it lit МАЛО on a full
     * locker: 13 of a norm of 13 is nothing missing, and no amount of buying
     * could clear the badge, because holding exactly the stated number always
     * satisfied `<=`. Only a part the owner gave a norm can be low at all.
     */
    val isLowStock: Boolean
        get() = !isOutOfStock && detail.minQuantity != null &&
            detail.quantity <= detail.minQuantity * LOW_STOCK_FRACTION
}

/**
 * Share of the norm at or below which a part reads as low.
 *
 * Half. With a norm of 1 this leaves no low state at all, which is right: a
 * single spare impeller is either aboard or gone, and "half an impeller left"
 * is not a thing to warn about.
 */
const val LOW_STOCK_FRACTION = 0.5

data class StoreUiState(
    val items: List<StoreItemUiModel> = emptyList(),
    val locations: List<StorageLocation> = emptyList(),
    val components: List<Component> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: StoreTab = StoreTab.ALL,
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val lockerCount: Int = 0,
    val outOfStockCount: Int = 0,
    val lowStockCount: Int = 0,
)

private data class DbStoreData(
    val rawItems: List<InventoryItemDetail>,
    val locations: List<StorageLocation>,
    val links: List<ComponentPartLink>,
    val components: List<Component>,
)

class StoreViewModel(
    private val db: AppDatabase,
    private val vesselId: Long,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTab = MutableStateFlow(StoreTab.ALL)
    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val dbFlow = combine(
                db.inventoryDao().observeAllForVessel(vesselId),
                db.inventoryDao().observeLocationsForVessel(vesselId),
                db.inventoryDao().observePartComponentLinks(vesselId),
                db.componentDao().observeForVessel(vesselId),
            ) { rawItems, locations, links, components ->
                DbStoreData(rawItems, locations, links, components)
            }

            combine(
                dbFlow,
                _searchQuery,
                _selectedTab,
            ) { dbData, query, tab ->
                val linksByPart = dbData.links.groupBy { it.partId }
                val models = dbData.rawItems.map { detail ->
                    StoreItemUiModel(
                        detail = detail,
                        linkedComponents = linksByPart[detail.partId].orEmpty(),
                    )
                }

                val totalPartsCount = models.size
                val lockersCount = dbData.locations.size
                val outOfStock = models.count { it.isOutOfStock }
                val lowStock = models.count { it.isLowStock }

                val cleanQuery = query.trim().lowercase()
                val filteredByQuery = if (cleanQuery.isEmpty()) {
                    models
                } else {
                    models.filter { model ->
                        val d = model.detail
                        d.partName.lowercase().contains(cleanQuery) ||
                            (d.partNumber?.lowercase()?.contains(cleanQuery) == true) ||
                            (d.manufacturer?.lowercase()?.contains(cleanQuery) == true) ||
                            (d.locationName?.lowercase()?.contains(cleanQuery) == true) ||
                            (d.partNote?.lowercase()?.contains(cleanQuery) == true) ||
                            model.linkedComponents.any { comp ->
                                comp.componentName.lowercase().contains(cleanQuery) ||
                                    (comp.componentCustomName?.lowercase()?.contains(cleanQuery) == true)
                            }
                    }
                }

                val finalItems = when (tab) {
                    StoreTab.ALL -> filteredByQuery
                    StoreTab.TO_BUY -> filteredByQuery.filter { it.isOutOfStock || it.isLowStock }
                    StoreTab.LOCATIONS -> filteredByQuery
                }

                StoreUiState(
                    items = finalItems,
                    locations = dbData.locations,
                    components = dbData.components,
                    searchQuery = query,
                    selectedTab = tab,
                    isLoading = false,
                    totalCount = totalPartsCount,
                    lockerCount = lockersCount,
                    outOfStockCount = outOfStock,
                    lowStockCount = lowStock,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTab(tab: StoreTab) {
        _selectedTab.value = tab
    }

    /**
     * Only the delta, never a quantity computed from what the screen shows:
     * two quick taps both read the same not-yet-refreshed number, and "+1, +1"
     * would be saved as +1. The database adds it to its own current value.
     */
    fun quickAdjustQuantity(itemId: Long, delta: Double) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.inventoryDao().adjustQuantity(itemId, delta)
            }
        }
    }

    fun savePart(
        itemId: Long?,
        partId: Long?,
        name: String,
        partNumber: String?,
        manufacturer: String?,
        note: String?,
        locationId: Long?,
        quantity: Double,
        minQuantity: Double?,
        linkedComponentIds: List<Long>,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.withTransaction {
                    val cleanName = name.trim()
                    val cleanNumber = partNumber?.trim()?.ifBlank { null }
                    val cleanMfr = manufacturer?.trim()?.ifBlank { null }
                    val cleanNote = note?.trim()?.ifBlank { null }

                    val targetPartId = if (partId == null || partId == 0L) {
                        db.inventoryDao().insertPart(
                            Part(
                                name = cleanName,
                                partNumber = cleanNumber,
                                manufacturer = cleanMfr,
                                note = cleanNote,
                            )
                        )
                    } else {
                        db.inventoryDao().updatePart(
                            Part(
                                id = partId,
                                name = cleanName,
                                partNumber = cleanNumber,
                                manufacturer = cleanMfr,
                                note = cleanNote,
                            )
                        )
                        partId
                    }

                    if (itemId == null || itemId == 0L) {
                        db.inventoryDao().insertItem(
                            InventoryItem(
                                vesselId = vesselId,
                                partId = targetPartId,
                                locationId = locationId,
                                quantity = maxOf(0.0, quantity),
                                minQuantity = minQuantity?.let { maxOf(0.0, it) },
                            )
                        )
                    } else {
                        db.inventoryDao().updateItem(
                            InventoryItem(
                                id = itemId,
                                vesselId = vesselId,
                                partId = targetPartId,
                                locationId = locationId,
                                quantity = maxOf(0.0, quantity),
                                minQuantity = minQuantity?.let { maxOf(0.0, it) },
                            )
                        )
                    }

                    db.inventoryDao().unlinkPartFromAllComponents(targetPartId)
                    for (compId in linkedComponentIds) {
                        db.inventoryDao().linkPartToComponent(
                            ComponentPart(
                                componentId = compId,
                                partId = targetPartId,
                            )
                        )
                    }
                }
            }
            onComplete()
        }
    }

    fun deletePart(itemId: Long, partId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.withTransaction {
                    db.inventoryDao().deleteItem(itemId)
                    // If no other inventory items on any vessel use this part, clean up the global part and links
                    if (db.inventoryDao().countItemsForPart(partId) == 0) {
                        db.inventoryDao().unlinkPartFromAllComponents(partId)
                        db.inventoryDao().deletePart(partId)
                    }
                }
            }
            onComplete()
        }
    }

    fun saveLocation(
        locationId: Long?,
        name: String,
        qrCode: String?,
        note: String?,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val cleanName = name.trim()
                val cleanQr = qrCode?.trim()?.ifBlank { null }
                val cleanNote = note?.trim()?.ifBlank { null }

                if (locationId == null || locationId == 0L) {
                    db.inventoryDao().insertLocation(
                        StorageLocation(
                            vesselId = vesselId,
                            name = cleanName,
                            qrCode = cleanQr,
                            note = cleanNote,
                        )
                    )
                } else {
                    db.inventoryDao().updateLocation(
                        StorageLocation(
                            id = locationId,
                            vesselId = vesselId,
                            name = cleanName,
                            qrCode = cleanQr,
                            note = cleanNote,
                        )
                    )
                }
            }
            onComplete()
        }
    }

    fun deleteLocation(locationId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.inventoryDao().deleteLocation(locationId)
            }
            onComplete()
        }
    }

    fun seedDefaultLocations(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = db.inventoryDao().allLocationsForVessel(vesselId)
                val existingNames = existing.map { it.name.trim().lowercase() }.toSet()
                val defaults = listOf(
                    context.getString(R.string.store_default_loc_engine),
                    context.getString(R.string.store_default_loc_cockpit),
                    context.getString(R.string.store_default_loc_chart),
                    context.getString(R.string.store_default_loc_forepeak),
                    context.getString(R.string.store_default_loc_galley),
                )
                for (name in defaults) {
                    if (!existingNames.contains(name.trim().lowercase())) {
                        db.inventoryDao().insertLocation(
                            StorageLocation(
                                vesselId = vesselId,
                                name = name,
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(db: AppDatabase, vesselId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StoreViewModel(db, vesselId) as T
                }
            }
    }
}
