package com.example.financetest

import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class FinanceViewModel : ViewModel() {
    private val _transactions = mutableStateListOf<TransactionItem>()
    val transactions: List<TransactionItem> get() = _transactions

    private val _tags = mutableStateListOf<Tag>()
    val tags: List<Tag> get() = _tags

    val balance = mutableDoubleStateOf(0.0)
    val selectedTagFilter = mutableStateOf<String?>(null)

    fun addTransaction(description: String, amountInput: String, isExpense: Boolean, selectedTags: List<String> = emptyList()) {
        val amount = amountInput.toDoubleOrNull() ?: return
        val signedAmount = if (isExpense) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
        val item = TransactionItem(
            id = System.currentTimeMillis(),
            description = description.ifBlank { if (isExpense) "Expense" else "Income" },
            amount = signedAmount,

            isExpense = isExpense,
            tags = selectedTags
        )
        _transactions.add(0, item)
        recomputeBalance()
    }

    fun removeTransaction(id: Long) {
        _transactions.removeAll { it.id == id }
        recomputeBalance()
    }

    fun addTag(name: String, color: Long) {
        val tag = Tag(
            id = System.currentTimeMillis(),
            name = name,
            color = color
        )
        _tags.add(tag)
    }

    fun removeTag(id: Long) {
        _tags.removeAll { it.id == id }
        _transactions.forEachIndexed { index, transaction ->
            _transactions[index] = transaction.copy(tags = transaction.tags.filter { it != _tags.find { tag -> tag.id == id }?.name })
        }
    }

    fun setTagFilter(tagName: String?) {
        selectedTagFilter.value = tagName
    }

    fun getFilteredTransactions(): List<TransactionItem> {
        return if (selectedTagFilter.value == null) transactions else transactions.filter { it.tags.contains(selectedTagFilter.value) }
    }

    private fun recomputeBalance() {
        balance.value = _transactions.sumOf { it.amount }
    }
}
