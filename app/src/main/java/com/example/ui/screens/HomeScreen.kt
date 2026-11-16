package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.AiParseUiState
import com.example.data.local.AiQueueEntity
import com.example.data.local.AllowanceFrequency
import com.example.data.local.ThemeMode
import com.example.data.local.TransactionEntity
import com.example.domain.EpochDayUtils
import com.example.domain.ForecastEngine
import com.example.domain.ForecastStatus
import com.example.ui.components.EmptyTransactionsCard
import com.example.ui.components.ForecastStatusPill
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.heroCardGradientBrush
import com.example.ui.components.softCardGradientBrush
import com.example.ui.components.statusColors
import com.example.ui.components.walletIconFor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalBaonDarkTheme
import com.example.ui.viewmodel.BaonUiState

@Composable
fun HomeScreen(
    uiState: BaonUiState,
    aiParseState: AiParseUiState,
    onEditTransaction: (TransactionEntity) -> Unit,
    onParseAiText: (String) -> Unit,
    onPickReceiptUri: (android.net.Uri) -> Unit,
    onProcessQueueItem: (AiQueueEntity) -> Unit,
    onRemoveQueueItem: (AiQueueEntity) -> Unit,
    onDismissAiBanner: () -> Unit,
    onToggleTheme: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var aiInputText by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickReceiptUri(uri)
        }
    }

    val forecast = uiState.forecast
    val animatedSafePesos by animateIntAsState(
        targetValue = (forecast.safeToSpendTodayCentavos / 100L).toInt(),
        animationSpec = spring(),
        label = "safe_to_spend_anim"
    )
    val (_, statusFg) = statusColors(forecast.status)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Greeting header
            item {
                val freqLabel = uiState.activeCycle?.frequency?.let {
                    runCatching { AllowanceFrequency.valueOf(it).label }.getOrNull()
                } ?: "Weekly"
                val nextDateStr = uiState.activeCycle?.nextBaonEpochDay?.let {
                    EpochDayUtils.formatShortDate(it)
                } ?: ""

                val isDarkNow = LocalBaonDarkTheme.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hi ${uiState.preferences.studentName}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (nextDateStr.isNotEmpty()) {
                                "$freqLabel baon • Next on $nextDateStr"
                            } else {
                                "Ready to track your baon"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            onToggleTheme(if (isDarkNow) ThemeMode.LIGHT else ThemeMode.DARK)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkNow) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDarkNow) "Switch to Light Mode" else "Switch to Dark Mode"
                        )
                    }
                }
            }

            // Hero Baon Meter Card (Solid uniform fill, no gradient)
            item {
                val heroAccentColor = if (forecast.status == ForecastStatus.ON_TRACK) {
                    MaterialTheme.colorScheme.primary
                } else {
                    statusFg
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("baon_meter_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(heroCardGradientBrush())
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.safe_to_spend_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            ForecastStatusPill(status = forecast.status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "₱${"%,d".format(animatedSafePesos)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp,
                            color = heroAccentColor,
                            modifier = Modifier.testTag("safe_to_spend_amount")
                        )

                        Text(
                            text = "Daily budget: ${ForecastEngine.formatPeso(forecast.dailyTargetCentavos)} • Spent today: ${ForecastEngine.formatPeso(forecast.spentTodayCentavos)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { forecast.progressRemainingRatio },
                            color = heroAccentColor,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ForecastEngine.formatPeso(forecast.remainingCentavos)} left of ${ForecastEngine.formatPeso(forecast.baonAmountCentavos)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (forecast.daysLeft == 1) "1 day until baon" else "${forecast.daysLeft} days until baon",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = heroAccentColor
                            )
                        }
                    }
                }
            }

            // Wallet Strip (Cash / GCash / Maya)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.wallets.forEach { summary ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wallet_summary_${summary.wallet.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(softCardGradientBrush())
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = walletIconFor(summary.wallet.kind),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = summary.wallet.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ForecastEngine.formatPeso(summary.currentBalanceCentavos),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // AI Quick Entry Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(softCardGradientBrush())
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Quick Entry (Taglish or Receipt)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = aiInputText,
                                onValueChange = { aiInputText = it },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.ai_quick_hint),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (aiInputText.isNotBlank()) {
                                            onParseAiText(aiInputText)
                                            aiInputText = ""
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_quick_entry_input")
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("ai_pick_receipt_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = "Scan receipt or GCash screenshot"
                                )
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    if (aiInputText.isNotBlank()) {
                                        onParseAiText(aiInputText)
                                        aiInputText = ""
                                    }
                                },
                                enabled = aiInputText.isNotBlank() && aiParseState !is AiParseUiState.Loading,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("ai_send_text_button")
                            ) {
                                if (aiParseState is AiParseUiState.Loading) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Parse Taglish gastos"
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = aiParseState is AiParseUiState.QueuedOffline || aiParseState is AiParseUiState.Error) {
                            val isError = aiParseState is AiParseUiState.Error
                            val msg = when (aiParseState) {
                                is AiParseUiState.Error -> aiParseState.message
                                is AiParseUiState.QueuedOffline -> aiParseState.message
                                else -> ""
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isError) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onDismissAiBanner,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Dismiss message",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Offline AI Queue (if pending)
            if (uiState.pendingAiQueue.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Offline AI Queue (${uiState.pendingAiQueue.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.pendingAiQueue.forEach { qItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = qItem.rawText,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { onProcessQueueItem(qItem) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Parse")
                                    }
                                    IconButton(
                                        onClick = { onRemoveQueueItem(qItem) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove queued item",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Today's transactions header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Gastos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ForecastEngine.formatPeso(forecast.spentTodayCentavos),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.todayTransactions.isEmpty()) {
                item {
                    EmptyTransactionsCard(
                        title = "No gastos logged today yet",
                        subtitle = "Tap + Add below for 3-tap logging, or type e.g. \"jeep 13, lunch 85\" above."
                    )
                }
            } else {
                items(
                    items = uiState.todayTransactions,
                    key = { it.transaction.id }
                ) { item ->
                    TransactionRowCard(
                        item = item,
                        onClick = { onEditTransaction(item.transaction) }
                    )
                }
            }
        }
    }
}
