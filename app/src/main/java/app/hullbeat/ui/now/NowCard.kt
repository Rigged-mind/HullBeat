package app.hullbeat.ui.now

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hullbeat.R
import app.hullbeat.data.db.categoryDisplayName
import app.hullbeat.data.db.displayName
import app.hullbeat.domain.DueCalculator
import app.hullbeat.ui.theme.LocalExtendedColors

/**
 * Single maintenance card on the Зараз screen.
 *
 * Meets cockpit ergonomics (ui-spec.md §1):
 * - Primary action target >= 56dp in the thumb zone.
 * - Minimum 12dp separation between tap targets.
 * - Semantic glyphs and border colors from LocalExtendedColors.
 */
@Composable
fun NowCard(
    item: NowItem,
    showVesselBadge: Boolean,
    onDone: () -> Unit,
    onDefer: () -> Unit,
    onUndefer: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val extended = LocalExtendedColors.current
    val isOverdue = item.state == DueCalculator.State.OVERDUE
    val isSoon = item.state == DueCalculator.State.DUE_SOON
    val isDeferred = item.isDeferred

    val glyph = when {
        isOverdue -> "●"
        isSoon -> "◐"
        isDeferred -> "◌"
        else -> "○"
    }

    val glyphColor = when {
        isOverdue -> extended.status.overdue
        isSoon -> extended.status.dueSoon
        isDeferred -> extended.status.deferred
        else -> extended.status.ok
    }

    val glyphSemantic = when {
        isOverdue -> stringResource(R.string.status_overdue)
        isSoon -> stringResource(R.string.status_soon)
        isDeferred -> stringResource(R.string.deferred_snackbar)
        else -> ""
    }

    val borderColor = if (isOverdue) extended.status.overdue else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isOverdue && extended.isHighContrast) 2.dp else extended.borderWidth.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = glyph,
                        color = glyphColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics {
                            if (glyphSemantic.isNotBlank()) {
                                contentDescription = glyphSemantic
                            }
                        },
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.component.displayName(context),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val categoryText = categoryDisplayName(context, item.component.categoryCode)
                        val subtitleText = if (showVesselBadge) {
                            "${item.vessel.displayName(context)} · $categoryText"
                        } else {
                            categoryText
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val urgencyText = remember(item, context) {
                    app.hullbeat.data.db.formatUrgency(context, item.result, item.schedule)
                }
                Text(
                    text = urgencyText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isOverdue) extended.status.overdue else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))

            // Primary action: 56dp height in thumb zone
            val verb = if (item.needsNewExpiry) {
                stringResource(R.string.action_renew)
            } else {
                stringResource(R.string.action_mark_done)
            }
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = verb,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Alt action: Defer or Cancel Deferral
            if (isDeferred) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = onUndefer,
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel_deferral),
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (isOverdue || isSoon) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = onDefer,
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.action_cannot_now_defer),
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
