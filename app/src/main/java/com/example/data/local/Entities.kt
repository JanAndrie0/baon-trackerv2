package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AllowanceFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class WalletKind {
    CASH,
    GCASH,
    MAYA
}

enum class TransactionKind(val label: String) {
    EXPENSE("Gastos"),
    INCOME("Income"),
    BAON("Add Baon")
}

enum class TransactionSource {
    MANUAL,
    AI_TEXT,
    AI_IMAGE
}

enum class AiQueueStatus {
    PENDING,
    FAILED,
    PARSED
}

@Entity(tableName = "allowance_cycles")
data class AllowanceCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountCentavos: Long,
    val frequency: String,
    val cycleStartEpochDay: Long,
    val nextBaonEpochDay: Long,
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val kind: String,
    val initialBalanceCentavos: Long = 0L,
    val sortOrder: Int = 0
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val envelopeCentavos: Long? = null,
    val sortOrder: Int = 0
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val walletId: Long,
    val categoryId: Long,
    val kind: String,
    val amountCentavos: Long,
    val note: String,
    val occurredAtMillis: Long,
    val source: String = TransactionSource.MANUAL.name,
    val isDeleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_queue_items")
data class AiQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val inputType: String, // "TEXT" or "IMAGE"
    val rawText: String,
    val imageFilePath: String? = null,
    val status: String = AiQueueStatus.PENDING.name,
    val errorMessage: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
