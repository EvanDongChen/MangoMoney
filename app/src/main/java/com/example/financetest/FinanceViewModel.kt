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

    // Goals: amounts are positive targets for spending caps per period
    val dailyGoal = mutableDoubleStateOf(0.0)
    val weeklyGoal = mutableDoubleStateOf(0.0)
    val biweeklyGoal = mutableDoubleStateOf(0.0)
    val monthlyGoal = mutableDoubleStateOf(0.0)

    // Reminders
    private val _reminders = mutableStateListOf<Reminder>()
    val reminders: List<Reminder> get() = _reminders

    fun addTransaction(description: String, amountInput: String, isExpense: Boolean, selectedTags: List<String> = emptyList()): Boolean {
        // Sanitize amount input by keeping digits and dot only (removes currency symbols, commas, etc.)
        val sanitized = amountInput.replace(Regex("[^0-9.]"), "").trim()
        val amount = sanitized.toDoubleOrNull()
        if (amount == null) {
            android.util.Log.w("FinanceViewModel", "Failed to parse transaction amount: '$amountInput' -> '$sanitized'")
            return false
        }
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
        return true
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

    fun setGoal(period: GoalPeriod, amountInput: String) {
        val amount = amountInput.toDoubleOrNull() ?: return
        when (period) {
            GoalPeriod.DAILY -> dailyGoal.doubleValue = kotlin.math.max(0.0, amount)
            GoalPeriod.WEEKLY -> weeklyGoal.doubleValue = kotlin.math.max(0.0, amount)
            GoalPeriod.BIWEEKLY -> biweeklyGoal.doubleValue = kotlin.math.max(0.0, amount)
            GoalPeriod.MONTHLY -> monthlyGoal.doubleValue = kotlin.math.max(0.0, amount)
        }
    }

    fun getSpentFor(period: GoalPeriod): Double {
        val now = java.time.ZonedDateTime.now()
        val (start, end) = when (period) {
            GoalPeriod.DAILY -> {
                val startOfDay = now.toLocalDate().atStartOfDay(now.zone)
                startOfDay to startOfDay.plusDays(1)
            }
            GoalPeriod.WEEKLY -> {
                val startOfWeek = now.with(java.time.DayOfWeek.MONDAY).toLocalDate().atStartOfDay(now.zone)
                startOfWeek to startOfWeek.plusWeeks(1)
            }
            GoalPeriod.BIWEEKLY -> {
                val startOfWeek = now.with(java.time.DayOfWeek.MONDAY).toLocalDate().atStartOfDay(now.zone)
                startOfWeek to startOfWeek.plusWeeks(2)
            }
            GoalPeriod.MONTHLY -> {
                val startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay(now.zone)
                startOfMonth to startOfMonth.plusMonths(1)
            }
        }

        val startMillis = start.toInstant().toEpochMilli()
        val endMillis = end.toInstant().toEpochMilli()

        return _transactions
            .asSequence()
            .filter { it.amount < 0 }
            .filter { it.id in startMillis until endMillis }
            .sumOf { kotlin.math.abs(it.amount) }
    }

    fun addReminder(title: String, amountInput: String?, dueAtMillis: Long): Long {
        val amount = amountInput?.toDoubleOrNull()
        val id = System.currentTimeMillis()
        val reminder = Reminder(
            id = id,
            title = title.ifBlank { "Reminder" },
            amount = amount,
            dueAtMillis = dueAtMillis,
            isDone = false
        )
        _reminders.add(0, reminder)
        return id
    }

    fun toggleReminderDone(id: Long) {
        val index = _reminders.indexOfFirst { it.id == id }
        if (index >= 0) {
            val current = _reminders[index]
            _reminders[index] = current.copy(isDone = !current.isDone)
        }
    }

    fun removeReminder(id: Long) {
        _reminders.removeAll { it.id == id }
    }
}
