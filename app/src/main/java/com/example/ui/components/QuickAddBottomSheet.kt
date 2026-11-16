package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionKind
import com.example.data.local.WalletEntity
import com.example.domain.ForecastEngine
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddBottomSheet(
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    editingTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        editingId: Long?,
        walletId: Long,
        categoryId: Long,
        kind: TransactionKind,
        amountCentavos: Long,
        note: String,
        occurredAtMillis: Long
    ) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialAmountText = remember(editingTransaction) {
        if (editingTransaction != null) {
            val pesos = editingTransaction.amountCentavos / 100L
            val cents = editingTransaction.amountCentavos % 100L
            if (cents == 0L) pesos.toString() else "$pesos.${"%02d".format(cents)}"
        } else {
            ""
        }
    }

    var amountText by remember(editingTransaction) { mutableStateOf(initialAmountText) }
    var selectedKind by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.let { runCatching { TransactionKind.valueOf(it.kind) }.getOrNull() }
                ?: TransactionKind.EXPENSE
        )
    }
    var selectedWalletId by remember(editingTransaction, wallets) {
        mutableStateOf(editingTransaction?.walletId ?: wallets.firstOrNull()?.id ?: 1L)
    }
    var selectedCategoryId by remember(editingTransaction, categories) {
        mutableStateOf(editingTransaction?.categoryId ?: categories.firstOrNull()?.id ?: 1L)
    }
    var note by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }

    val parsedCentavos = ForecastEngine.parsePesoInputToCentavos(amountText) ?: 0L
    val isSaveEnabled = parsedCentavos > 0L && wallets.isNotEmpty() && categories.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (editingTransaction != null) "Edit Entry" else "Quick Log",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editingTransaction != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(editingTransaction.id)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("delete_tx_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete transaction",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_quick_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close quick add"
                        )
                    }
                }
            }

            // Transaction kind pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionKind.entries.forEach { kind ->
                    val selected = selectedKind == kind
                    FilterChip(
                        selected = selected,
                        onClick = { selectedKind = kind },
                        label = { Text(kind.label) },
                        modifier = Modifier.testTag("kind_chip_${kind.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Large live peso readout
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (amountText.isEmpty()) "₱0" else "₱$amountText",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp,
                    color = if (parsedCentavos > 0L) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                        .testTag("quick_add_amount_display")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Student categories (shown for expenses)
            if (selectedKind == TransactionKind.EXPENSE) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = selectedCategoryId == cat.id
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategoryId = cat.id },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconFor(cat.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selected) catColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.16f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("category_chip_${cat.iconKey}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Wallet selector
            Text(
                text = "Wallet",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wallets.forEach { wallet ->
                    val selected = selectedWalletId == wallet.id
                    FilterChip(
                        selected = selected,
                        onClick = { selectedWalletId = wallet.id },
                        leadingIcon = {
                            Icon(
                                imageVector = walletIconFor(wallet.kind),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(wallet.name) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wallet_chip_${wallet.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Optional note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional, e.g. Jeep sa España)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_note_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Thumb-zone Numpad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "BACK")
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            NumpadKeyButton(
                                label = key,
                                onClick = {
                                    amountText = applyNumpadKey(amountText, key)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val chosenCatId = if (selectedKind == TransactionKind.EXPENSE) {
                        selectedCategoryId
                    } else {
                        categories.lastOrNull()?.id ?: selectedCategoryId
                    }
                    onSave(
                        editingTransaction?.id,
                        selectedWalletId,
                        chosenCatId,
                        selectedKind,
                        parsedCentavos,
                        note,
                        editingTransaction?.occurredAtMillis ?: System.currentTimeMillis()
                    )
                },
                enabled = isSaveEnabled,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button")
            ) {
                Text(
                    text = if (editingTransaction != null) "Update Entry" else "Save ${selectedKind.label}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NumpadKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("numpad_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label == "BACK") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = label,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

internal fun applyNumpadKey(current: String, key: String): String {
    return when (key) {
        "BACK" -> if (current.isNotEmpty()) current.dropLast(1) else ""
        "." -> {
            if (current.contains(".")) current
            else if (current.isEmpty()) "0."
            else "$current."
        }
        else -> {
            val dotIndex = current.indexOf('.')
            if (dotIndex != -1 && current.length - dotIndex > 2) {
                current
            } else if (current == "0") {
                key
            } else if (current.length >= 9) {
                current
            } else {
                current + key
            }
        }
    }
}
