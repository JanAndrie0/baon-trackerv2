package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.AllowanceFrequency
import com.example.data.local.AppPalette
import com.example.data.local.ThemeMode
import com.example.domain.EpochDayUtils
import com.example.domain.ForecastEngine
import com.example.ui.components.softCardGradientBrush
import com.example.ui.viewmodel.BaonUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: BaonUiState,
    onUpdateStudentName: (String) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateAppPalette: (AppPalette) -> Unit,
    onUpdateCycle: (amountCentavos: Long, frequency: AllowanceFrequency, nextBaonEpochDay: Long, startFresh: Boolean) -> Unit,
    onUpdateEnvelope: (categoryId: Long, envelopeCentavos: Long?) -> Unit,
    onUpdateReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onGenerateCsv: suspend () -> String,
    onClearAllData: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { EpochDayUtils.todayEpochDay() }

    var nameInput by remember(uiState.preferences.studentName) {
        mutableStateOf(uiState.preferences.studentName)
    }
    var frequency by remember(uiState.activeCycle?.frequency) {
        mutableStateOf(
            uiState.activeCycle?.frequency?.let {
                runCatching { AllowanceFrequency.valueOf(it) }.getOrNull()
            } ?: AllowanceFrequency.WEEKLY
        )
    }
    var amountInput by remember(uiState.activeCycle?.amountCentavos) {
        val centavos = uiState.activeCycle?.amountCentavos ?: 1000_00L
        mutableStateOf((centavos / 100L).toString())
    }
    var nextBaonEpochDay by remember(uiState.activeCycle?.nextBaonEpochDay) {
        mutableLongStateOf(uiState.activeCycle?.nextBaonEpochDay ?: (today + 7L))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editingEnvelopeCategory by remember { mutableStateOf<com.example.data.local.CategoryEntity?>(null) }
    var envelopeAmountInput by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdateReminder(true, uiState.preferences.reminderHour, uiState.preferences.reminderMinute)
            onShowMessage("Daily reminder enabled!")
        } else {
            onUpdateReminder(false, uiState.preferences.reminderHour, uiState.preferences.reminderMinute)
            onShowMessage("Notification permission denied. You can allow it in Android Settings.")
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = onGenerateCsv()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        }
                    }
                }.onSuccess {
                    onShowMessage("Exported CSV successfully!")
                }.onFailure { err ->
                    onShowMessage("Export failed: ${err.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

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
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Appearance Card (System / Light / Dark)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(softCardGradientBrush())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Switch between Light, Dark, or follow your phone's system theme.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.preferences.themeMode == mode,
                                    onClick = { onUpdateThemeMode(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_mode_${mode.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Color Palette",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPalette.entries.forEach { palette ->
                                FilterChip(
                                    selected = uiState.preferences.appPalette == palette,
                                    onClick = { onUpdateAppPalette(palette) },
                                    label = { Text(palette.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("app_palette_${palette.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Profile & Baon Cycle Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(softCardGradientBrush())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Profile & Baon Cycle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Nickname") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    onUpdateStudentName(nameInput)
                                    onShowMessage("Updated nickname!")
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Save")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AllowanceFrequency.entries.forEach { freq ->
                                FilterChip(
                                    selected = frequency == freq,
                                    onClick = { frequency = freq },
                                    label = { Text(freq.label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Cycle Baon Amount (₱)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Next Baon: ${EpochDayUtils.formatFullDate(nextBaonEpochDay)}")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val parsedBaon = ForecastEngine.parsePesoInputToCentavos(amountInput) ?: 0L
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (parsedBaon > 0L) {
                                        onUpdateCycle(parsedBaon, frequency, nextBaonEpochDay, false)
                                        onShowMessage("Current baon cycle updated!")
                                    }
                                },
                                enabled = parsedBaon > 0L,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Update Cycle")
                            }
                            Button(
                                onClick = {
                                    if (parsedBaon > 0L) {
                                        onUpdateCycle(parsedBaon, frequency, nextBaonEpochDay, true)
                                        onShowMessage("Started fresh baon cycle today!")
                                    }
                                },
                                enabled = parsedBaon > 0L,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("New Cycle")
                            }
                        }
                    }
                }
            }

            // Category Envelopes Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(softCardGradientBrush())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Category Envelopes (Optional Caps)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set soft spending caps per category for this baon cycle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.categories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(
                                    onClick = {
                                        editingEnvelopeCategory = cat
                                        envelopeAmountInput = cat.envelopeCentavos?.let { (it / 100L).toString() } ?: ""
                                    }
                                ) {
                                    Text(
                                        text = cat.envelopeCentavos?.let { ForecastEngine.formatPeso(it) } ?: "Set cap"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily Local Reminder Card
            item {
                val reminderHour = uiState.preferences.reminderHour
                val reminderMinute = uiState.preferences.reminderMinute
                val formattedTime = remember(reminderHour, reminderMinute) {
                    val amPm = if (reminderHour >= 12) "PM" else "AM"
                    val h12 = when {
                        reminderHour == 0 -> 12
                        reminderHour > 12 -> reminderHour - 12
                        else -> reminderHour
                    }
                    "$h12:${"%02d".format(reminderMinute)} $amPm"
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(softCardGradientBrush())
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Daily Gastos Reminder",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Remind me at $formattedTime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.preferences.reminderEnabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            onUpdateReminder(true, reminderHour, reminderMinute)
                                            onShowMessage("Daily reminder enabled!")
                                        } else {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        onUpdateReminder(enable, reminderHour, reminderMinute)
                                        onShowMessage(if (enable) "Daily reminder enabled!" else "Reminder paused")
                                    }
                                },
                                modifier = Modifier.testTag("reminder_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, pickedH, pickedM ->
                                        onUpdateReminder(uiState.preferences.reminderEnabled, pickedH, pickedM)
                                        onShowMessage("Reminder time updated")
                                    },
                                    reminderHour,
                                    reminderMinute,
                                    false
                                ).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Reminder Time ($formattedTime)")
                        }
                    }
                }
            }

            // Data & Privacy (Export CSV & Delete Local Data)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(softCardGradientBrush())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Data & Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All data lives locally on your phone. Export a CSV anytime or reset your local records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                csvExportLauncher.launch("baon-tracker-export.csv")
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_csv_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export My Data (CSV)")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("clear_all_data_button")
                        ) {
                            Icon(imageVector = Icons.Filled.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Local Data")
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = EpochDayUtils.epochDayNoonMillis(nextBaonEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            nextBaonEpochDay = EpochDayUtils.millisToEpochDay(millis).coerceAtLeast(today + 1L)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    editingEnvelopeCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { editingEnvelopeCategory = null },
            title = { Text("${cat.name} Envelope Cap") },
            text = {
                OutlinedTextField(
                    value = envelopeAmountInput,
                    onValueChange = { envelopeAmountInput = it },
                    label = { Text("Cycle cap in ₱ (leave blank to clear)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cap = if (envelopeAmountInput.isBlank()) null
                        else ForecastEngine.parsePesoInputToCentavos(envelopeAmountInput)
                        onUpdateEnvelope(cat.id, cap)
                        editingEnvelopeCategory = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEnvelopeCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Clear all local data?") },
            text = { Text("This permanently removes your cycles, transactions, and preferences from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
