package com.example.financetest

data class TransactionItem(
    val id: Long,
    val description: String,
    val amount: Double,
    val isExpense: Boolean,
    val tags: List<String> = emptyList()
)

data class Tag(
    val id: Long,
    val name: String,
    val color: Long
)
