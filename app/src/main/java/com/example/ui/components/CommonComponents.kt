package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppPalette
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionKind
import com.example.data.local.TransactionSource
import com.example.domain.EpochDayUtils
import com.example.domain.ForecastEngine
import com.example.domain.ForecastStatus
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalBaonAppPalette
import com.example.ui.theme.LocalBaonDarkTheme
import com.example.ui.theme.StatusOnTrackBgDark
import com.example.ui.theme.StatusOnTrackBgLight
import com.example.ui.theme.StatusOnTrackFgDark
import com.example.ui.theme.StatusOnTrackFgLight
import com.example.ui.theme.StatusOnTrackPinkBgDark
import com.example.ui.theme.StatusOnTrackPinkBgLight
import com.example.ui.theme.StatusOnTrackPinkFgDark
import com.example.ui.theme.StatusOnTrackPinkFgLight
import com.example.ui.theme.StatusOverBgDark
import com.example.ui.theme.StatusOverBgLight
import com.example.ui.theme.StatusOverFgDark
import com.example.ui.theme.StatusOverFgLight
import com.example.ui.theme.StatusTightBgDark
import com.example.ui.theme.StatusTightBgLight
import com.example.ui.theme.StatusTightFgDark
import com.example.ui.theme.StatusTightFgLight
import com.example.ui.viewmodel.TransactionItemUi

fun categoryIconFor(iconKey: String): ImageVector {
    return when (iconKey.lowercase()) {
        "pagkain" -> Icons.Filled.Restaurant
        "pamasahe" -> Icons.Filled.DirectionsBus
        "load" -> Icons.Filled.Wifi
        "print" -> Icons.Filled.Print
        "school" -> Icons.Filled.MenuBook
        "gala" -> Icons.Filled.Celebration
        else -> Icons.Filled.MoreHoriz
    }
}

fun walletIconFor(walletKindOrName: String): ImageVector {
    return when (walletKindOrName.uppercase()) {
        "GCASH", "MAYA" -> Icons.Filled.Smartphone
        "CASH" -> Icons.Filled.Payments
        else -> Icons.Filled.AccountBalanceWallet
    }
}

@Composable
fun statusColors(status: ForecastStatus): Pair<Color, Color> {
    val dark = LocalBaonDarkTheme.current
    val palette = LocalBaonAppPalette.current
    return when (status) {
        ForecastStatus.ON_TRACK -> {
            if (palette == AppPalette.PINK_PASTEL) {
                if (dark) StatusOnTrackPinkBgDark to StatusOnTrackPinkFgDark
                else StatusOnTrackPinkBgLight to StatusOnTrackPinkFgLight
            } else {
                if (dark) StatusOnTrackBgDark to StatusOnTrackFgDark
                else StatusOnTrackBgLight to StatusOnTrackFgLight
            }
        }
        ForecastStatus.TIGHT -> if (dark) StatusTightBgDark to StatusTightFgDark else StatusTightBgLight to StatusTightFgLight
        ForecastStatus.OVER -> if (dark) StatusOverBgDark to StatusOverFgDark else StatusOverBgLight to StatusOverFgLight
    }
}

fun statusLabel(status: ForecastStatus): String {
    return when (status) {
        ForecastStatus.ON_TRACK -> "On Track"
        ForecastStatus.TIGHT -> "Tight"
        ForecastStatus.OVER -> "Over Budget"
    }
}

@Composable
fun ForecastStatusPill(
    status: ForecastStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = statusColors(status)
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Text(
            text = statusLabel(status),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun heroCardGradientBrush(): Brush {
    val dark = LocalBaonDarkTheme.current
    val palette = LocalBaonAppPalette.current
    val colors = when (palette) {
        AppPalette.MINT -> if (dark) {
            listOf(Color(0xFF1F5D37), Color(0xFF174729), Color(0xFF10341E))
        } else {
            listOf(Color(0xFFC2F2C7), Color(0xFFADEBB3), Color(0xFF96E09F))
        }
        AppPalette.PINK_PASTEL -> if (dark) {
            listOf(Color(0xFF6B2534), Color(0xFF551C28), Color(0xFF40141E))
        } else {
            listOf(Color(0xFFFCD3D7), Color(0xFFFCC0C5), Color(0xFFF7A6B7))
        }
    }
    return Brush.linearGradient(colors = colors)
}

@Composable
fun softCardGradientBrush(): Brush {
    val dark = LocalBaonDarkTheme.current
    val palette = LocalBaonAppPalette.current
    val colors = when (palette) {
        AppPalette.MINT -> if (dark) {
            listOf(Color(0xFF16281D), Color(0xFF111E16))
        } else {
            listOf(Color(0xFFFFFFFF), Color(0xFFE7F7EA))
        }
        AppPalette.PINK_PASTEL -> if (dark) {
            listOf(Color(0xFF28161C), Color(0xFF1E1015))
        } else {
            listOf(Color(0xFFFFFFFF), Color(0xFFFCE8EB))
        }
    }
    return Brush.linearGradient(colors = colors)
}

@Composable
fun TransactionRowCard(
    item: TransactionItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tx = item.transaction
    val cat = item.category
    val wallet = item.wallet
    val isExpense = tx.kind == TransactionKind.EXPENSE.name
    val catColor = Color(cat?.colorHex ?: 0xFF64748B)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_row_${tx.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(softCardGradientBrush())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isExpense) catColor.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpense) {
                        categoryIconFor(cat?.iconKey ?: "others")
                    } else {
                        Icons.Filled.Payments
                    },
                    contentDescription = cat?.name ?: tx.kind,
                    tint = if (isExpense) catColor else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.note,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (tx.source != TransactionSource.MANUAL.name) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Added with AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(if (isExpense) cat?.name ?: "Others" else TransactionKind.valueOf(tx.kind).label)
                        append(" • ")
                        append(wallet?.name ?: "Cash")
                        append(" • ")
                        append(EpochDayUtils.formatTime(tx.occurredAtMillis))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = if (isExpense) {
                    "-${ForecastEngine.formatPeso(tx.amountCentavos)}"
                } else {
                    "+${ForecastEngine.formatPeso(tx.amountCentavos)}"
                },
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = if (isExpense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptyTransactionsCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(softCardGradientBrush())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
