package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.data.local.TransactionSource
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToLong

private const val TAG = "BaonAiService"

class AiService(private val context: Context) {

    private val entrySchema = Schema.obj(
        properties = mapOf(
            "amountPesos" to Schema.double(description = "Expense amount in Philippine Pesos (e.g. 85.0 or 13.50)"),
            "category" to Schema.enumeration(
                values = listOf("Pagkain", "Pamasahe", "Load", "Print/Xerox", "School", "Gala", "Others"),
                description = "Best matching Filipino student spending category"
            ),
            "wallet" to Schema.enumeration(
                values = listOf("Cash", "GCash", "Maya"),
                description = "Payment wallet mentioned or inferred, default Cash if unclear"
            ),
            "note" to Schema.string(description = "Short clean description in English or Taglish (e.g. Jeepney fare, Lunch, Xerox)")
        )
    )

    private val parseSchema = Schema.obj(
        properties = mapOf(
            "entries" to Schema.array(
                items = entrySchema,
                description = "List of parsed expenses from the student's text or receipt/payment screenshot"
            )
        )
    )

    private val parserModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = parseSchema
                temperature = 0.1f
            }
        )
    }

    private val recapModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.5f
            }
        )
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun parseTaglishText(input: String): Result<List<AiParsedDraft>> {
        return try {
            val prompt = """
                You are an assistant for a Filipino student allowance tracker called Baon Tracker.
                Parse the student's Taglish/English quick expense note into structured Philippine Peso (PHP) expenses.
                Default wallet is "Cash" unless GCash or Maya is mentioned.
                Map items to the closest category: Pagkain, Pamasahe, Load, Print/Xerox, School, Gala, Others.
                Examples: "jeep 13, xerox 20, lunch 85 gcash" -> 3 entries.
                Student input: "$input"
            """.trimIndent()

            val response = withTimeout(60_000L) {
                parserModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val drafts = parseEntriesJson(jsonText, TransactionSource.AI_TEXT)
            if (drafts.isEmpty()) {
                Result.failure(IllegalStateException("No expenses found in \"$input\". Try e.g. \"jeep 13, lunch 85\"."))
            } else {
                Result.success(drafts)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Taglish AI parse timed out", e)
            Result.failure(Exception("AI parsing timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Taglish AI parse failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI parsing failed."))
        }
    }

    suspend fun parseReceiptBitmap(bitmap: Bitmap): Result<List<AiParsedDraft>> {
        return try {
            val resized = downscaleBitmapIfNeeded(bitmap, 1024)
            val prompt = """
                Extract expense items or payment total from this receipt or GCash/Maya screenshot for a Filipino student.
                If it is a GCash or Maya transfer/payment screenshot, set wallet to "GCash" or "Maya" and extract the total amount paid and merchant/purpose.
                If it is a paper receipt with clear student items or a single store total, extract the total or clear individual gastos in Philippine Pesos (PHP).
                Choose the best category from: Pagkain, Pamasahe, Load, Print/Xerox, School, Gala, Others.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                parserModel.generateContent(
                    content {
                        image(resized)
                        text(prompt)
                    }
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val drafts = parseEntriesJson(jsonText, TransactionSource.AI_IMAGE)
            if (drafts.isEmpty()) {
                Result.failure(IllegalStateException("Could not detect any Peso amount in that image."))
            } else {
                Result.success(drafts)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image AI parse timed out", e)
            Result.failure(Exception("Image scan timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image AI parse failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Receipt scan failed."))
        }
    }

    suspend fun generateWeeklyRecap(summaryPromptData: String): Result<String> {
        return try {
            val prompt = """
                You are a friendly Filipino student budget buddy in Baon Tracker.
                Write a concise 2-sentence Taglish recap (friendly, practical, encouraging, no markdown bullets) based strictly on these numbers:
                $summaryPromptData
            """.trimIndent()

            val response = withTimeout(60_000L) {
                recapModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Model returned an empty recap."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Weekly recap timed out", e)
            Result.failure(Exception("Weekly recap timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Weekly recap failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to generate weekly recap."))
        }
    }

    suspend fun decodeCompressedBitmap(uri: Uri, maxDim: Int = 1024): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val raw = BitmapFactory.decodeStream(stream) ?: return@withContext null
                    downscaleBitmapIfNeeded(raw, maxDim)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode bitmap from URI", e)
                null
            }
        }

    suspend fun saveCompressedBitmapToCache(bitmap: Bitmap): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "ai_queue").apply { mkdirs() }
                val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
                val scaled = downscaleBitmapIfNeeded(bitmap, 1024)
                FileOutputStream(file).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
                }
                file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache offline receipt image", e)
                null
            }
        }

    suspend fun loadBitmapFromPath(path: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cached bitmap", e)
                null
            }
        }

    private fun downscaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largest = max(width, height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest.toFloat()
        val targetW = (width * scale).roundToLong().toInt().coerceAtLeast(1)
        val targetH = (height * scale).roundToLong().toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun parseEntriesJson(
        jsonString: String,
        source: TransactionSource
    ): List<AiParsedDraft> {
        val root = JSONObject(jsonString)
        val entries = root.optJSONArray("entries") ?: return emptyList()
        val drafts = mutableListOf<AiParsedDraft>()
        for (i in 0 until entries.length()) {
            val obj = entries.optJSONObject(i) ?: continue
            val amountPesos = obj.optDouble("amountPesos", 0.0)
            val centavos = (amountPesos * 100.0).roundToLong()
            if (centavos <= 0L) continue
            val category = obj.optString("category", "Others").ifBlank { "Others" }
            val wallet = obj.optString("wallet", "Cash").ifBlank { "Cash" }
            val note = obj.optString("note", category).ifBlank { category }
            drafts.add(
                AiParsedDraft(
                    amountCentavos = centavos,
                    categoryName = category,
                    walletName = wallet,
                    note = note,
                    source = source
                )
            )
        }
        return drafts
    }
}
