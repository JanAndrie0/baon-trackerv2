package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ai.AiParsedDraft
import com.example.data.local.CategoryEntity
import com.example.data.local.WalletEntity
import com.example.domain.ForecastEngine
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiDraftConfirmSheet(
    initialDrafts: List<AiParsedDraft>,
    queueItemId: Long?,
    categories: List<CategoryEntity>,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirmSave: (List<AiParsedDraft>, Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editableDrafts = remember(initialDrafts) {
        mutableStateListOf<AiParsedDraft>().apply { addAll(initialDrafts) }
    }

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
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Review AI Drafts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Verify amounts before saving to your baon log",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_ai_drafts_button")
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close drafts")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            editableDrafts.forEachIndexed { index, draft ->
                val amountString = remember(draft.draftId) {
                    val pesos = draft.amountCentavos / 100L
                    val cents = draft.amountCentavos % 100L
                    if (cents == 0L) pesos.toString() else "$pesos.${"%02d".format(cents)}"
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Item #${index + 1} • ${ForecastEngine.formatPeso(draft.amountCentavos)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { editableDrafts.removeAt(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Discard draft item",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = if (draft.amountCentavos > 0L) {
                                    val p = draft.amountCentavos / 100L
                                    val c = draft.amountCentavos % 100L
                                    if (c == 0L) p.toString() else "$p.${"%02d".format(c)}"
                                } else {
                                    amountString
                                },
                                onValueChange = { raw ->
                                    val parsed = ForecastEngine.parsePesoInputToCentavos(raw)
                                    if (parsed != null) {
                                        editableDrafts[index] = draft.copy(amountCentavos = parsed)
                                    } else if (raw.isEmpty()) {
                                        editableDrafts[index] = draft.copy(amountCentavos = 0L)
                                    }
                                },
                                label = { Text("₱ Amount") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.42f)
                            )

                            OutlinedTextField(
                                value = draft.note,
                                onValueChange = { newNote ->
                                    editableDrafts[index] = draft.copy(note = newNote)
                                },
                                label = { Text("Note") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.58f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = draft.categoryName.equals(cat.name, ignoreCase = true),
                                    onClick = {
                                        editableDrafts[index] = draft.copy(categoryName = cat.name)
                                    },
                                    label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            wallets.forEach { w ->
                                FilterChip(
                                    selected = draft.walletName.equals(w.name, ignoreCase = true),
                                    onClick = {
                                        editableDrafts[index] = draft.copy(walletName = w.name)
                                    },
                                    label = { Text(w.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            val validDrafts = editableDrafts.filter { it.amountCentavos > 0L }
            val totalCentavos = validDrafts.sumOf { it.amountCentavos }

            Button(
                onClick = { onConfirmSave(validDrafts, queueItemId) },
                enabled = validDrafts.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_ai_drafts_button")
            ) {
                Text(
                    text = "Confirm & Save (${validDrafts.size} • ${ForecastEngine.formatPeso(totalCentavos)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
