package com.example.financetest

import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double,
    val description: String,
    val confidence: Float
)

class NumberParser {
    
    companion object {
        // Regex patterns to match various currency formats
        private val CURRENCY_PATTERNS = listOf(
            // $123.45, $1,234.56
            Pattern.compile("\\$\\s*([0-9,]+(?:\\.[0-9]{2})?)"),
            // 123.45, 1,234.56
            Pattern.compile("\\b([0-9,]+(?:\\.[0-9]{2})?)\\b"),
            // USD 123.45
            Pattern.compile("USD\\s*([0-9,]+(?:\\.[0-9]{2})?)"),
            // 123.45 USD
            Pattern.compile("([0-9,]+(?:\\.[0-9]{2})?)\\s*USD")
        )
        
        // Common transaction description keywords
        private val TRANSACTION_KEYWORDS = listOf(
            "purchase", "payment", "charge", "debit", "credit", "withdrawal", "deposit",
            "grocery", "gas", "restaurant", "coffee", "shopping", "amazon", "uber",
            "salary", "income", "refund", "transfer", "fee", "subscription"
        )
    }
    
    fun parseTextForTransactions(text: String): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()
        
        // Extract all potential monetary amounts
        val amounts = extractAmounts(text)
        
        // For each amount, try to find a description
        amounts.forEach { amount ->
            val description = extractDescription(text, amount)
            val confidence = calculateConfidence(text, amount, description)
            
            if (confidence > 0.3f) { // Only include transactions with reasonable confidence
                transactions.add(ParsedTransaction(amount, description, confidence))
            }
        }
        
        // Remove duplicates and sort by confidence
        return transactions
            .distinctBy { "${it.amount}_${it.description}" }
            .sortedByDescending { it.confidence }
    }
    
    private fun extractAmounts(text: String): List<Double> {
        val amounts = mutableSetOf<Double>()
        
        CURRENCY_PATTERNS.forEach { pattern ->
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0 && amount < 100000) { // Reasonable range
                    amounts.add(amount)
                }
            }
        }
        
        return amounts.toList()
    }
    
    private fun extractDescription(text: String, amount: Double): String {
        val amountStr = amount.toString()
        val amountIndex = text.indexOf(amountStr)
        
        if (amountIndex == -1) return "Transaction from receipt"
        
        // Look for text around the amount
        val start = maxOf(0, amountIndex - 50)
        val end = minOf(text.length, amountIndex + 50)
        val context = text.substring(start, end)
        
        // Try to find meaningful words
        val words = context.split("\\s+".toRegex())
            .filter { it.length > 2 }
            .filter { word -> 
                TRANSACTION_KEYWORDS.any { keyword -> 
                    word.contains(keyword, ignoreCase = true) 
                }
            }
        
        return if (words.isNotEmpty()) {
            words.joinToString(" ").take(50)
        } else {
            // Fallback: use nearby words
            val nearbyWords = context.split("\\s+".toRegex())
                .filter { it.length > 2 && !it.matches("\\d+".toRegex()) }
                .take(3)
            if (nearbyWords.isNotEmpty()) {
                nearbyWords.joinToString(" ").take(50)
            } else {
                "Transaction from receipt"
            }
        }
    }
    
    private fun calculateConfidence(text: String, amount: Double, description: String): Float {
        var confidence = 0.5f
        
        // Higher confidence for amounts with currency symbols
        if (text.contains("$$amount") || text.contains("$amount")) {
            confidence += 0.2f
        }
        
        // Higher confidence for known transaction keywords
        if (TRANSACTION_KEYWORDS.any { description.contains(it, ignoreCase = true) }) {
            confidence += 0.2f
        }
        
        // Higher confidence for reasonable amounts
        when {
            amount in 0.01..1000.0 -> confidence += 0.1f
            amount in 1000.0..10000.0 -> confidence += 0.05f
        }
        
        // Lower confidence for very round numbers (might be noise)
        if (amount % 1.0 == 0.0 && amount > 100) {
            confidence -= 0.1f
        }
        
        return confidence.coerceIn(0.0f, 1.0f)
    }
}
