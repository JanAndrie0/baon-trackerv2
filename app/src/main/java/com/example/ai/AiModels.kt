package com.example.ai

import com.example.data.local.TransactionSource
import java.util.UUID

data class AiParsedDraft(
    val draftId: String = UUID.randomUUID().toString(),
    val amountCentavos: Long,
    val categoryName: String,
    val walletName: String,
    val note: String,
    val source: TransactionSource
)

sealed interface AiParseUiState {
    data object Idle : AiParseUiState
    data object Loading : AiParseUiState
    data class DraftsReady(
        val drafts: List<AiParsedDraft>,
        val queueItemId: Long? = null
    ) : AiParseUiState
    data class QueuedOffline(val message: String) : AiParseUiState
    data class Error(val message: String) : AiParseUiState
}

sealed interface WeeklyRecapUiState {
    data object Idle : WeeklyRecapUiState
    data object Loading : WeeklyRecapUiState
    data class Ready(val text: String, val generatedAtMillis: Long) : WeeklyRecapUiState
    data class Error(val message: String) : WeeklyRecapUiState
}
