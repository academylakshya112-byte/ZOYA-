package com.example.core

import android.content.Context
import android.util.Log

object MessageManager {
    private const val TAG = "MessageManager"

    private val SENSITIVE_KEYWORDS = listOf(
        "otp", "password", "pin", "cvv", "bank", "account", "money",
        "rupees", "rs.", "dollar", "card", "debit", "credit", "payment", "upi"
    )

    fun isMessageSafeForAutoReply(messageText: String): Boolean {
        val lower = messageText.lowercase()
        return !SENSITIVE_KEYWORDS.any { lower.contains(it) }
    }

    suspend fun sendVerifiedReply(
        context: Context,
        app: String,
        recipient: String,
        replyText: String
    ): VerificationResult {
        if (!isMessageSafeForAutoReply(replyText)) {
            return VerificationResult.Failure("सुरक्षा कारणों से संवेदनशील संदेश स्वतः नहीं भेजे जा सकते।")
        }

        return if (app.equals("WhatsApp", ignoreCase = true)) {
            // WhatsApp message automation
            val engine = com.example.tools.ToolExecutionEngine(context)
            val result = engine.execute(
                "sendWhatsAppMessage",
                kotlinx.serialization.json.buildJsonObject {
                    put("contactName", kotlinx.serialization.json.JsonPrimitive(recipient))
                    put("message", kotlinx.serialization.json.JsonPrimitive(replyText))
                }
            )
            if (result.contains("error", ignoreCase = true) || result.contains("failed", ignoreCase = true)) {
                VerificationResult.Failure("WhatsApp संदेश नहीं भेजा जा सका।")
            } else {
                VerificationResult.Success("WhatsApp पर $recipient को संदेश भेज दिया गया है।")
            }
        } else {
            // SMS automation
            val engine = com.example.tools.ToolExecutionEngine(context)
            val result = engine.execute(
                "sendSmsMessage",
                kotlinx.serialization.json.buildJsonObject {
                    put("contactNameOrNumber", kotlinx.serialization.json.JsonPrimitive(recipient))
                    put("message", kotlinx.serialization.json.JsonPrimitive(replyText))
                }
            )
            if (result.contains("error", ignoreCase = true) || result.contains("failed", ignoreCase = true)) {
                VerificationResult.Failure("SMS नहीं भेजा जा सका।")
            } else {
                VerificationResult.Success("$recipient को SMS भेज दिया गया है।")
            }
        }
    }
}
