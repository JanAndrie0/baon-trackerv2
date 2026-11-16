package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.ai.AiParsedDraft
import com.example.ai.AiService
import com.example.data.local.AiQueueEntity
import com.example.data.local.AiQueueStatus
import com.example.data.local.AllowanceCycleEntity
import com.example.data.local.AllowanceFrequency
import com.example.data.local.BaonDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.DefaultSeeds
import com.example.data.local.PreferencesManager
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionKind
import com.example.data.local.TransactionSource
import com.example.data.local.UserPreferences
import com.example.data.local.WalletEntity
import com.example.domain.EpochDayUtils
import com.example.domain.ForecastEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BaonRepository(
    context: Context,
    private val db: BaonDatabase = BaonDatabase.getInstance(context),
    private val prefs: PreferencesManager = PreferencesManager(context),
    val aiService: AiService = AiService(context)
) {
    private val cycleDao = db.allowanceCycleDao()
    private val walletDao = db.walletDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val aiQueueDao = db.aiQueueDao()

    val activeCycle: Flow<AllowanceCycleEntity?> = cycleDao.observeActiveCycle()
    val wallets: Flow<List<WalletEntity>> = walletDao.observeWallets()
    val categories: Flow<List<CategoryEntity>> = categoryDao.observeCategories()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAllTransactions()
    val pendingAiQueue: Flow<List<AiQueueEntity>> = aiQueueDao.observePendingQueue()
    val userPreferences: Flow<UserPreferences> = prefs.preferences

    suspend fun ensureDefaultsSeeded() = withContext(Dispatchers.IO) {
        if (walletDao.getWallets().isEmpty()) {
            walletDao.insertAll(DefaultSeeds.defaultWallets)
        }
        if (categoryDao.getCategories().isEmpty()) {
            categoryDao.insertAll(DefaultSeeds.defaultCategories)
        }
    }

    suspend fun completeOnboarding(
        studentName: String,
        frequency: AllowanceFrequency,
        baonAmountCentavos: Long,
        nextBaonEpochDay: Long,
        walletStartingCentavos: Map<String, Long>
    ) = withContext(Dispatchers.IO) {
        ensureDefaultsSeeded()
        val today = EpochDayUtils.todayEpochDay()
        cycleDao.deactivateAllCycles()
        cycleDao.insertCycle(
            AllowanceCycleEntity(
                amountCentavos = baonAmountCentavos,
                frequency = frequency.name,
                cycleStartEpochDay = today,
                nextBaonEpochDay = nextBaonEpochDay.coerceAtLeast(today + 1L),
                isActive = true
            )
        )
        val currentWallets = walletDao.getWallets()
        for (wallet in currentWallets) {
            val initial = walletStartingCentavos[wallet.name] ?: 0L
            walletDao.updateInitialBalance(wallet.id, initial)
        }
        prefs.completeOnboarding(studentName)
    }

    suspend fun updateOrStartCycle(
        amountCentavos: Long,
        frequency: AllowanceFrequency,
        nextBaonEpochDay: Long,
        startFreshCycle: Boolean
    ) = withContext(Dispatchers.IO) {
        val today = EpochDayUtils.todayEpochDay()
        val existing = cycleDao.getActiveCycle()
        if (existing == null || startFreshCycle) {
            cycleDao.deactivateAllCycles()
            cycleDao.insertCycle(
                AllowanceCycleEntity(
                    amountCentavos = amountCentavos,
                    frequency = frequency.name,
                    cycleStartEpochDay = today,
                    nextBaonEpochDay = nextBaonEpochDay.coerceAtLeast(today + 1L),
                    isActive = true
                )
            )
        } else {
            cycleDao.updateCycle(
                existing.copy(
                    amountCentavos = amountCentavos,
                    frequency = frequency.name,
                    nextBaonEpochDay = nextBaonEpochDay.coerceAtLeast(today + 1L)
                )
            )
        }
    }

    suspend fun updateWalletInitialBalance(walletId: Long, initialCentavos: Long) {
        walletDao.updateInitialBalance(walletId, initialCentavos)
    }

    suspend fun updateCategoryEnvelope(categoryId: Long, envelopeCentavos: Long?) {
        categoryDao.updateEnvelope(categoryId, envelopeCentavos)
    }

    suspend fun saveTransaction(
        editingId: Long? = null,
        walletId: Long,
        categoryId: Long,
        kind: TransactionKind,
        amountCentavos: Long,
        note: String,
        occurredAtMillis: Long,
        source: TransactionSource = TransactionSource.MANUAL
    ) = withContext(Dispatchers.IO) {
        val cleanNote = note.trim().ifEmpty {
            if (kind == TransactionKind.BAON) "Extra baon" else "Gastos"
        }
        val entity = TransactionEntity(
            id = editingId ?: 0L,
            walletId = walletId,
            categoryId = categoryId,
            kind = kind.name,
            amountCentavos = amountCentavos,
            note = cleanNote,
            occurredAtMillis = occurredAtMillis,
            source = source.name,
            isDeleted = false,
            updatedAtMillis = System.currentTimeMillis()
        )
        if (editingId != null && editingId > 0L) {
            transactionDao.updateTransaction(entity)
        } else {
            transactionDao.insertTransaction(entity)
        }

        // If user logs "Add Baon" during active cycle, bump active cycle allowance pool too
        if (kind == TransactionKind.BAON && editingId == null) {
            val active = cycleDao.getActiveCycle()
            if (active != null) {
                cycleDao.updateCycle(active.copy(amountCentavos = active.amountCentavos + amountCentavos))
            }
        }
    }

    suspend fun saveDrafts(drafts: List<AiParsedDraft>, queueItemId: Long?) =
        withContext(Dispatchers.IO) {
            ensureDefaultsSeeded()
            val walletsList = walletDao.getWallets()
            val categoriesList = categoryDao.getCategories()
            val now = System.currentTimeMillis()

            val entities = drafts.map { draft ->
                val wallet = walletsList.firstOrNull {
                    it.name.equals(draft.walletName, ignoreCase = true)
                } ?: walletsList.first()
                val category = categoriesList.firstOrNull {
                    it.name.equals(draft.categoryName, ignoreCase = true)
                } ?: categoriesList.last()

                TransactionEntity(
                    walletId = wallet.id,
                    categoryId = category.id,
                    kind = TransactionKind.EXPENSE.name,
                    amountCentavos = draft.amountCentavos,
                    note = draft.note.trim().ifEmpty { category.name },
                    occurredAtMillis = now,
                    source = draft.source.name,
                    isDeleted = false,
                    updatedAtMillis = now
                )
            }
            transactionDao.insertTransactions(entities)
            if (queueItemId != null) {
                aiQueueDao.updateStatus(queueItemId, AiQueueStatus.PARSED.name, null)
            }
        }

    suspend fun softDeleteTransaction(id: Long) {
        transactionDao.softDelete(id)
    }

    suspend fun restoreTransaction(id: Long) {
        transactionDao.restoreSoftDeleted(id)
    }

    suspend fun queueOfflineText(rawText: String): Long {
        return aiQueueDao.insertItem(
            AiQueueEntity(
                inputType = "TEXT",
                rawText = rawText,
                status = AiQueueStatus.PENDING.name
            )
        )
    }

    suspend fun queueOfflineImage(bitmap: Bitmap): Long? {
        val path = aiService.saveCompressedBitmapToCache(bitmap) ?: return null
        return aiQueueDao.insertItem(
            AiQueueEntity(
                inputType = "IMAGE",
                rawText = "Receipt / e-wallet screenshot",
                imageFilePath = path,
                status = AiQueueStatus.PENDING.name
            )
        )
    }

    suspend fun decodeImageFromUri(uri: Uri): Bitmap? = aiService.decodeCompressedBitmap(uri)

    suspend fun deleteQueueItem(item: AiQueueEntity) = withContext(Dispatchers.IO) {
        item.imageFilePath?.let { path ->
            runCatching { File(path).delete() }
        }
        aiQueueDao.deleteById(item.id)
    }

    suspend fun markQueueFailed(id: Long, message: String) {
        aiQueueDao.updateStatus(id, AiQueueStatus.FAILED.name, message)
    }

    suspend fun updateStudentName(name: String) = prefs.updateStudentName(name)

    suspend fun updateThemeMode(mode: com.example.data.local.ThemeMode) = prefs.updateThemeMode(mode)

    suspend fun updateAppPalette(palette: com.example.data.local.AppPalette) = prefs.updateAppPalette(palette)

    suspend fun updateReminder(enabled: Boolean, hour: Int, minute: Int) =
        prefs.updateReminder(enabled, hour, minute)

    suspend fun saveCachedWeeklyRecap(text: String) = prefs.saveCachedRecap(text)

    suspend fun generateCsvReport(): String = withContext(Dispatchers.IO) {
        val txs = transactionDao.getAllTransactions()
        val walletsById = walletDao.getWallets().associateBy { it.id }
        val catsById = categoryDao.getCategories().associateBy { it.id }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sb = StringBuilder()
        sb.appendLine("id,date_time,kind,category,wallet,amount_php,note,source")
        for (tx in txs) {
            val dateStr = sdf.format(Date(tx.occurredAtMillis))
            val catName = csvEscape(catsById[tx.categoryId]?.name ?: "Others")
            val walletName = csvEscape(walletsById[tx.walletId]?.name ?: "Cash")
            val amountStr = ForecastEngine.formatPeso(tx.amountCentavos, showExactCentavos = true)
                .replace("₱", "")
                .replace(",", "")
            val noteStr = csvEscape(tx.note)
            sb.appendLine("${tx.id},$dateStr,${tx.kind},$catName,$walletName,$amountStr,$noteStr,${tx.source}")
        }
        sb.toString()
    }

    suspend fun clearAllLocalData() = withContext(Dispatchers.IO) {
        transactionDao.deleteAll()
        cycleDao.deleteAll()
        aiQueueDao.deleteAll()
        categoryDao.deleteAll()
        walletDao.deleteAll()
        prefs.clearAll()
        ensureDefaultsSeeded()
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
