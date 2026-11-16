package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowanceCycleDao {
    @Query("SELECT * FROM allowance_cycles WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun observeActiveCycle(): Flow<AllowanceCycleEntity?>

    @Query("SELECT * FROM allowance_cycles WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveCycle(): AllowanceCycleEntity?

    @Query("UPDATE allowance_cycles SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllCycles()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: AllowanceCycleEntity): Long

    @Update
    suspend fun updateCycle(cycle: AllowanceCycleEntity)

    @Query("DELETE FROM allowance_cycles")
    suspend fun deleteAll()
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY sortOrder ASC, id ASC")
    fun observeWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets ORDER BY sortOrder ASC, id ASC")
    suspend fun getWallets(): List<WalletEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallets: List<WalletEntity>)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("UPDATE wallets SET initialBalanceCentavos = :amountCentavos WHERE id = :walletId")
    suspend fun updateInitialBalance(walletId: Long, amountCentavos: Long)

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET envelopeCentavos = :envelopeCentavos WHERE id = :categoryId")
    suspend fun updateEnvelope(categoryId: Long, envelopeCentavos: Long?)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY occurredAtMillis DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY occurredAtMillis DESC, id DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(tx: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 0, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun restoreSoftDeleted(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface AiQueueDao {
    @Query("SELECT * FROM ai_queue_items WHERE status != 'PARSED' ORDER BY createdAtMillis DESC")
    fun observePendingQueue(): Flow<List<AiQueueEntity>>

    @Query("SELECT * FROM ai_queue_items WHERE status != 'PARSED' ORDER BY createdAtMillis ASC")
    suspend fun getPendingQueue(): List<AiQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: AiQueueEntity): Long

    @Query("UPDATE ai_queue_items SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String? = null)

    @Query("DELETE FROM ai_queue_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ai_queue_items")
    suspend fun deleteAll()
}
