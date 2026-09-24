package app.hullbeat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hullbeat.R
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.db.displayName
import app.hullbeat.ui.theme.LocalExtendedColors

/**
 * Bottom sheet for switching between vessels or adding a new vessel.
 *
 * Touch targets meet the >= 56 dp rule for wet/gloved hands (ui-spec.md §1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesselSelectSheet(
    vessels: List<Vessel>,
    selectedVesselId: Long?,
    overdueCounts: Map<Long, Int> = emptyMap(),
    soonCounts: Map<Long, Int> = emptyMap(),
    onSelectVessel: (Long?) -> Unit,
    onAddVessel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val extended = LocalExtendedColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.vessel_select_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (vessels.size > 1) {
                    item(key = "fleet_all") {
                        val isSelected = selectedVesselId == null
                        val totalOverdue = overdueCounts.values.sum()
                        val totalSoon = soonCounts.values.sum()

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable {
                                    onSelectVessel(null)
                                    onDismiss()
                                }
                                .semantics(mergeDescendants = true) {},
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_nav_boat),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.fleet_all),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    val statusSummary = when {
                                        totalOverdue > 0 -> "${stringResource(R.string.status_overdue)}: $totalOverdue"
                                        totalSoon > 0 -> "${stringResource(R.string.status_soon)}: $totalSoon"
                                        else -> stringResource(R.string.vessel_status_all_clear)
                                    }
                                    Text(
                                        text = statusSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            totalOverdue > 0 -> extended.status.overdue
                                            totalSoon > 0 -> extended.status.dueSoon
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                items(
                    items = vessels,
                    key = { it.id },
                ) { vessel ->
                    val isSelected = selectedVesselId == vessel.id
                    val overdue = overdueCounts[vessel.id] ?: 0
                    val soon = soonCounts[vessel.id] ?: 0
                    val photoBitmap = rememberSampledBitmap(context, vessel.photoUri, 120)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable {
                                onSelectVessel(vessel.id)
                                onDismiss()
                            }
                            .semantics(mergeDescendants = true) {},
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (photoBitmap != null) {
                                    Image(
                                        bitmap = photoBitmap,
                                        contentDescription = vessel.displayName(context),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_nav_boat),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vessel.displayName(context),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val subtitle = listOfNotNull(
                                    vessel.make.takeIf { !it.isNullOrBlank() },
                                    vessel.model.takeIf { !it.isNullOrBlank() },
                                ).joinToString(" ").ifBlank {
                                    vessel.hullType.ifBlank { null }
                                }
                                val statusText = when {
                                    overdue > 0 -> "${stringResource(R.string.status_overdue)}: $overdue"
                                    soon > 0 -> "${stringResource(R.string.status_soon)}: $soon"
                                    else -> stringResource(R.string.vessel_status_all_clear)
                                }
                                val detailLine = if (subtitle != null) "$subtitle · $statusText" else statusText
                                Text(
                                    text = detailLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        overdue > 0 -> extended.status.overdue
                                        soon > 0 -> extended.status.dueSoon
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onAddVessel()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_add_vessel),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
