package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AllowanceCycleEntity::class,
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        AiQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BaonDatabase : RoomDatabase() {
    abstract fun allowanceCycleDao(): AllowanceCycleDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun aiQueueDao(): AiQueueDao

    companion object {
        @Volatile
        private var INSTANCE: BaonDatabase? = null

        fun getInstance(context: Context): BaonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BaonDatabase::class.java,
                    "baon_tracker.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object DefaultSeeds {
    val defaultWallets = listOf(
        WalletEntity(name = "Cash", kind = WalletKind.CASH.name, initialBalanceCentavos = 0L, sortOrder = 0),
        WalletEntity(name = "GCash", kind = WalletKind.GCASH.name, initialBalanceCentavos = 0L, sortOrder = 1),
        WalletEntity(name = "Maya", kind = WalletKind.MAYA.name, initialBalanceCentavos = 0L, sortOrder = 2)
    )

    val defaultCategories = listOf(
        CategoryEntity(name = "Pagkain", iconKey = "pagkain", colorHex = 0xFF10B981, sortOrder = 0),
        CategoryEntity(name = "Pamasahe", iconKey = "pamasahe", colorHex = 0xFF0EA5E9, sortOrder = 1),
        CategoryEntity(name = "Load", iconKey = "load", colorHex = 0xFF8B5CF6, sortOrder = 2),
        CategoryEntity(name = "Print/Xerox", iconKey = "print", colorHex = 0xFFF59E0B, sortOrder = 3),
        CategoryEntity(name = "School", iconKey = "school", colorHex = 0xFF3B82F6, sortOrder = 4),
        CategoryEntity(name = "Gala", iconKey = "gala", colorHex = 0xFFEC4899, sortOrder = 5),
        CategoryEntity(name = "Others", iconKey = "others", colorHex = 0xFF64748B, sortOrder = 6)
    )
}
