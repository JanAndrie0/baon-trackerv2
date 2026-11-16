package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiParseUiState
import com.example.ai.AiParsedDraft
import com.example.ai.WeeklyRecapUiState
import com.example.data.local.AiQueueEntity
import com.example.data.local.AllowanceCycleEntity
import com.example.data.local.AllowanceFrequency
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionKind
import com.example.data.local.TransactionSource
import com.example.data.local.UserPreferences
import com.example.data.local.WalletEntity
import com.example.data.repository.BaonRepository
import com.example.domain.EpochDayUtils
import com.example.domain.ForecastEngine
import com.example.domain.ForecastResult
import com.example.domain.ForecastStatus
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "BaonViewModel"

data class WalletSummary(
    val wallet: WalletEntity,
    val currentBalanceCentavos: Long
)

data class CategoryBreakdownItem(
    val category: CategoryEntity,
    val spentCentavos: Long,
    val percentage: Float
)

data class DailySpendPoint(
    val epochDay: Long,
    val label: String,
    val spentCentavos: Long
)

data class TransactionItemUi(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val wallet: WalletEntity?
)

data class DayTransactionGroup(
    val epochDay: Long,
    val headerLabel: String,
    val totalExpenseCentavos: Long,
    val items: List<TransactionItemUi>
)

data class BaonUiState(
    val isLoaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val activeCycle: AllowanceCycleEntity? = null,
    val wallets: List<WalletSummary> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val forecast: ForecastResult = ForecastEngine.calculate(0L, 0L, 0L, 0L, 1L),
    val todayTransactions: List<TransactionItemUi> = emptyList(),
    val allTransactions: List<TransactionItemUi> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val last7DaysSpend: List<DailySpendPoint> = emptyList(),
    val dailySpendByEpochDay: Map<Long, Long> = emptyMap(),
    val pendingAiQueue: List<AiQueueEntity> = emptyList(),
    val totalCycleExpensesCentavos: Long = 0L
)

class BaonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BaonRepository(application.applicationContext)

    private val _aiParseState = MutableStateFlow<AiParseUiState>(AiParseUiState.Idle)
    val aiParseState: StateFlow<AiParseUiState> = _aiParseState.asStateFlow()

    private val _recapState = MutableStateFlow<WeeklyRecapUiState>(WeeklyRecapUiState.Idle)
    val recapState: StateFlow<WeeklyRecapUiState> = _recapState.asStateFlow()

    private val _lastDeletedTxId = MutableStateFlow<Long?>(null)
    val lastDeletedTxId: StateFlow<Long?> = _lastDeletedTxId.asStateFlow()

    private val refreshTick = MutableStateFlow(0L)

    val uiState: StateFlow<BaonUiState> = combine(
        repository.activeCycle,
        repository.wallets,
        repository.categories,
        repository.transactions,
        combine(repository.pendingAiQueue, repository.userPreferences, refreshTick) { q, p, _ -> q to p }
    ) { cycle, wallets, categories, transactions, (queue, prefs) ->
        val today = EpochDayUtils.todayEpochDay()
        val catsById = categories.associateBy { it.id }
        val walletsById = wallets.associateBy { it.id }

        val uiTransactions = transactions.map { tx ->
            TransactionItemUi(
                transaction = tx,
                category = catsById[tx.categoryId],
                wallet = walletsById[tx.walletId]
            )
        }

        // Compute live wallet balances across all non-deleted transactions
        val walletSummaries = wallets.map { wallet ->
            var balance = wallet.initialBalanceCentavos
            for (tx in transactions) {
                if (tx.walletId == wallet.id) {
                    when (tx.kind) {
                        TransactionKind.EXPENSE.name -> balance -= tx.amountCentavos
                        TransactionKind.INCOME.name, TransactionKind.BAON.name -> balance += tx.amountCentavos
                    }
                }
            }
            WalletSummary(wallet = wallet, currentBalanceCentavos = balance)
        }

        val cycleStart = cycle?.cycleStartEpochDay ?: today
        val nextBaon = cycle?.nextBaonEpochDay ?: (today + 1L)
        val baonAmount = cycle?.amountCentavos ?: 0L

        var spentBeforeToday = 0L
        var spentToday = 0L
        val todayItems = mutableListOf<TransactionItemUi>()
        val dailySpendMap = mutableMapOf<Long, Long>()
        val categoryTotals = mutableMapOf<Long, Long>()

        for (item in uiTransactions) {
            val tx = item.transaction
            val txDay = EpochDayUtils.millisToEpochDay(tx.occurredAtMillis)
            if (tx.kind == TransactionKind.EXPENSE.name) {
                dailySpendMap[txDay] = (dailySpendMap[txDay] ?: 0L) + tx.amountCentavos
                if (txDay >= cycleStart) {
                    if (txDay < today) {
                        spentBeforeToday += tx.amountCentavos
                    } else if (txDay == today) {
                        spentToday += tx.amountCentavos
                    }
                    categoryTotals[tx.categoryId] = (categoryTotals[tx.categoryId] ?: 0L) + tx.amountCentavos
                }
            }
            if (txDay == today) {
                todayItems.add(item)
            }
        }

        val forecast = ForecastEngine.calculate(
            baonAmountCentavos = baonAmount,
            spentBeforeTodayCentavos = spentBeforeToday,
            spentTodayCentavos = spentToday,
            todayEpochDay = today,
            nextBaonEpochDay = nextBaon
        )

        val totalCycleSpent = spentBeforeToday + spentToday
        val breakdown = categories.mapNotNull { cat ->
            val spent = categoryTotals[cat.id] ?: 0L
            if (spent <= 0L && cat.envelopeCentavos == null) {
                null
            } else {
                val pct = if (totalCycleSpent > 0L) (spent.toFloat() / totalCycleSpent.toFloat()) else 0f
                CategoryBreakdownItem(
                    category = cat,
                    spentCentavos = spent,
                    percentage = pct
                )
            }
        }.sortedByDescending { it.spentCentavos }

        val last7Days = (6 downTo 0).map { offset ->
            val day = today - offset.toLong()
            DailySpendPoint(
                epochDay = day,
                label = if (offset == 0) "Today" else EpochDayUtils.formatWeekdayShort(day),
                spentCentavos = dailySpendMap[day] ?: 0L
            )
        }

        // Hydrate cached recap if idle
        if (_recapState.value is WeeklyRecapUiState.Idle && !prefs.cachedWeeklyRecap.isNullOrBlank()) {
            _recapState.value = WeeklyRecapUiState.Ready(
                text = prefs.cachedWeeklyRecap,
                generatedAtMillis = prefs.cachedWeeklyRecapMillis
            )
        }

        BaonUiState(
            isLoaded = true,
            preferences = prefs,
            activeCycle = cycle,
            wallets = walletSummaries,
            categories = categories,
            forecast = forecast,
            todayTransactions = todayItems,
            allTransactions = uiTransactions,
            categoryBreakdown = breakdown,
            last7DaysSpend = last7Days,
            dailySpendByEpochDay = dailySpendMap,
            pendingAiQueue = queue,
            totalCycleExpensesCentavos = totalCycleSpent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BaonUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
        }
    }

    fun completeOnboarding(
        studentName: String,
        frequency: AllowanceFrequency,
        baonAmountCentavos: Long,
        nextBaonEpochDay: Long,
        walletStartingCentavos: Map<String, Long>
    ) {
        viewModelScope.launch {
            repository.completeOnboarding(
                studentName = studentName,
                frequency = frequency,
                baonAmountCentavos = baonAmountCentavos,
                nextBaonEpochDay = nextBaonEpochDay,
                walletStartingCentavos = walletStartingCentavos
            )
        }
    }

    fun updateCycle(
        amountCentavos: Long,
        frequency: AllowanceFrequency,
        nextBaonEpochDay: Long,
        startFreshCycle: Boolean
    ) {
        viewModelScope.launch {
            repository.updateOrStartCycle(amountCentavos, frequency, nextBaonEpochDay, startFreshCycle)
        }
    }

    fun saveTransaction(
        editingId: Long? = null,
        walletId: Long,
        categoryId: Long,
        kind: TransactionKind,
        amountCentavos: Long,
        note: String,
        occurredAtMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.saveTransaction(
                editingId = editingId,
                walletId = walletId,
                categoryId = categoryId,
                kind = kind,
                amountCentavos = amountCentavos,
                note = note,
                occurredAtMillis = occurredAtMillis,
                source = TransactionSource.MANUAL
            )
        }
    }

    fun softDeleteTransaction(id: Long) {
        viewModelScope.launch {
            _lastDeletedTxId.value = id
            repository.softDeleteTransaction(id)
        }
    }

    fun undoLastDelete() {
        val id = _lastDeletedTxId.value ?: return
        viewModelScope.launch {
            repository.restoreTransaction(id)
            _lastDeletedTxId.value = null
        }
    }

    fun clearUndoMarker() {
        _lastDeletedTxId.value = null
    }

    fun parseQuickEntryText(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            if (!repository.aiService.isOnline()) {
                repository.queueOfflineText(trimmed)
                _aiParseState.value = AiParseUiState.QueuedOffline(
                    "No internet right now—saved \"$trimmed\" to your offline AI queue."
                )
                return@launch
            }

            _aiParseState.value = AiParseUiState.Loading
            val result = repository.aiService.parseTaglishText(trimmed)
            result.fold(
                onSuccess = { drafts ->
                    _aiParseState.value = AiParseUiState.DraftsReady(drafts = drafts)
                },
                onFailure = { error ->
                    _aiParseState.value = AiParseUiState.Error(
                        error.message ?: "Unable to parse your gastos note."
                    )
                }
            )
        }
    }

    fun parseReceiptImageUri(uri: Uri) {
        viewModelScope.launch {
            _aiParseState.value = AiParseUiState.Loading
            val bitmap = repository.decodeImageFromUri(uri)
            if (bitmap == null) {
                _aiParseState.value = AiParseUiState.Error("Couldn't read that image. Try another photo or screenshot.")
                return@launch
            }

            if (!repository.aiService.isOnline()) {
                repository.queueOfflineImage(bitmap)
                _aiParseState.value = AiParseUiState.QueuedOffline(
                    "Offline right now—compressed and queued your receipt/screenshot for later."
                )
                return@launch
            }

            val result = repository.aiService.parseReceiptBitmap(bitmap)
            result.fold(
                onSuccess = { drafts ->
                    _aiParseState.value = AiParseUiState.DraftsReady(drafts = drafts)
                },
                onFailure = { error ->
                    _aiParseState.value = AiParseUiState.Error(
                        error.message ?: "Could not parse receipt image."
                    )
                }
            )
        }
    }

    fun processQueuedItem(item: AiQueueEntity) {
        viewModelScope.launch {
            if (!repository.aiService.isOnline()) {
                _aiParseState.value = AiParseUiState.Error("Still offline—connect to Wi-Fi or data to parse queued items.")
                return@launch
            }

            _aiParseState.value = AiParseUiState.Loading
            val result = if (item.inputType == "IMAGE" && !item.imageFilePath.isNullOrBlank()) {
                val bmp = repository.aiService.loadBitmapFromPath(item.imageFilePath)
                if (bmp == null) {
                    Result.failure(IllegalStateException("Cached receipt file is no longer available."))
                } else {
                    repository.aiService.parseReceiptBitmap(bmp)
                }
            } else {
                repository.aiService.parseTaglishText(item.rawText)
            }

            result.fold(
                onSuccess = { drafts ->
                    _aiParseState.value = AiParseUiState.DraftsReady(drafts = drafts, queueItemId = item.id)
                },
                onFailure = { error ->
                    val msg = error.message ?: "Failed to process queued item."
                    repository.markQueueFailed(item.id, msg)
                    _aiParseState.value = AiParseUiState.Error(msg)
                }
            )
        }
    }

    fun removeQueuedItem(item: AiQueueEntity) {
        viewModelScope.launch {
            repository.deleteQueueItem(item)
        }
    }

    fun confirmAiDrafts(drafts: List<AiParsedDraft>, queueItemId: Long?) {
        if (drafts.isEmpty()) {
            _aiParseState.value = AiParseUiState.Idle
            return
        }
        viewModelScope.launch {
            repository.saveDrafts(drafts, queueItemId)
            _aiParseState.value = AiParseUiState.Idle
        }
    }

    fun dismissAiState() {
        _aiParseState.value = AiParseUiState.Idle
    }

    fun generateWeeklyRecap() {
        val state = uiState.value
        viewModelScope.launch {
            if (!repository.aiService.isOnline()) {
                _recapState.value = WeeklyRecapUiState.Error("You're offline. Connect to internet to refresh your AI recap.")
                return@launch
            }

            _recapState.value = WeeklyRecapUiState.Loading
            try {
                val sevenDayTotal = state.last7DaysSpend.sumOf { it.spentCentavos }
                val topCategories = state.categoryBreakdown.take(3).joinToString(", ") {
                    "${it.category.name}: ${ForecastEngine.formatPeso(it.spentCentavos)}"
                }.ifEmpty { "No categorized spending yet" }

                val summary = """
                    Student: ${state.preferences.studentName}
                    Cycle Baon: ${ForecastEngine.formatPeso(state.forecast.baonAmountCentavos)}
                    Remaining Baon: ${ForecastEngine.formatPeso(state.forecast.remainingCentavos)}
                    Days until next baon: ${state.forecast.daysLeft}
                    Safe to spend today left: ${ForecastEngine.formatPeso(state.forecast.safeToSpendTodayCentavos)} (status: ${state.forecast.status})
                    Last 7 days total spent: ${ForecastEngine.formatPeso(sevenDayTotal)}
                    Top categories this cycle: $topCategories
                """.trimIndent()

                val result = repository.aiService.generateWeeklyRecap(summary)
                result.fold(
                    onSuccess = { text ->
                        repository.saveCachedWeeklyRecap(text)
                        _recapState.value = WeeklyRecapUiState.Ready(text, System.currentTimeMillis())
                    },
                    onFailure = { error ->
                        _recapState.value = WeeklyRecapUiState.Error(
                            error.message ?: "Unable to generate AI recap."
                        )
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error generating recap", e)
                _recapState.value = WeeklyRecapUiState.Error(e.localizedMessage ?: "Recap failed.")
            }
        }
    }

    fun updateStudentName(name: String) {
        viewModelScope.launch {
            repository.updateStudentName(name)
        }
    }

    fun updateThemeMode(mode: com.example.data.local.ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }

    fun updateAppPalette(palette: com.example.data.local.AppPalette) {
        viewModelScope.launch {
            repository.updateAppPalette(palette)
        }
    }

    fun updateWalletInitialBalance(walletId: Long, initialCentavos: Long) {
        viewModelScope.launch {
            repository.updateWalletInitialBalance(walletId, initialCentavos)
        }
    }

    fun updateCategoryEnvelope(categoryId: Long, envelopeCentavos: Long?) {
        viewModelScope.launch {
            repository.updateCategoryEnvelope(categoryId, envelopeCentavos)
        }
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateReminder(enabled, hour, minute)
            val appContext = getApplication<Application>().applicationContext
            if (enabled) {
                ReminderScheduler.scheduleDailyReminder(appContext, hour, minute)
            } else {
                ReminderScheduler.cancelDailyReminder(appContext)
            }
        }
    }

    suspend fun generateCsvContent(): String = repository.generateCsvReport()

    fun clearAllLocalData() {
        viewModelScope.launch {
            val appContext = getApplication<Application>().applicationContext
            ReminderScheduler.cancelDailyReminder(appContext)
            repository.clearAllLocalData()
            _aiParseState.value = AiParseUiState.Idle
            _recapState.value = WeeklyRecapUiState.Idle
            refreshTick.value += 1L
        }
    }

    fun groupTransactionsForHistory(
        items: List<TransactionItemUi>,
        searchQuery: String,
        selectedWalletId: Long?,
        selectedCategoryId: Long?
    ): List<DayTransactionGroup> {
        val today = EpochDayUtils.todayEpochDay()
        val query = searchQuery.trim().lowercase()
        val filtered = items.filter { item ->
            val tx = item.transaction
            val matchesWallet = selectedWalletId == null || tx.walletId == selectedWalletId
            val matchesCategory = selectedCategoryId == null || tx.categoryId == selectedCategoryId
            val matchesQuery = query.isEmpty() ||
                tx.note.lowercase().contains(query) ||
                (item.category?.name?.lowercase()?.contains(query) == true) ||
                (item.wallet?.name?.lowercase()?.contains(query) == true)
            matchesWallet && matchesCategory && matchesQuery
        }

        return filtered
            .groupBy { EpochDayUtils.millisToEpochDay(it.transaction.occurredAtMillis) }
            .entries
            .sortedByDescending { it.key }
            .map { (day, dayItems) ->
                val totalSpent = dayItems
                    .filter { it.transaction.kind == TransactionKind.EXPENSE.name }
                    .sumOf { it.transaction.amountCentavos }
                DayTransactionGroup(
                    epochDay = day,
                    headerLabel = EpochDayUtils.formatDayHeader(day, today),
                    totalExpenseCentavos = totalSpent,
                    items = dayItems
                )
            }
    }
}
