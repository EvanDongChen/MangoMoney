package com.example.financetest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.financetest.ui.theme.FinanceTestTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.mutableDoubleStateOf

class MainActivity : ComponentActivity() {
    private val financeViewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceTestTheme {
                FinanceAppScreen(financeViewModel)
            }
        }
    }
}

data class TransactionItem(
    val id: Long,
    val description: String,
    val amount: Double,
    val isExpense: Boolean
)

class FinanceViewModel : androidx.lifecycle.ViewModel() {
    private val _transactions = androidx.compose.runtime.mutableStateListOf<TransactionItem>()
    val transactions: List<TransactionItem> get() = _transactions

    val balance = mutableDoubleStateOf(0.0)

    fun addTransaction(description: String, amountInput: String, isExpense: Boolean) {
        val amount = amountInput.toDoubleOrNull() ?: return
        val signedAmount = if (isExpense) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
        val item = TransactionItem(
            id = System.currentTimeMillis(),
            description = description.ifBlank { if (isExpense) "Expense" else "Income" },
            amount = signedAmount,
            isExpense = isExpense
        )
        _transactions.add(0, item)
        recomputeBalance()
    }

    fun removeTransaction(id: Long) {
        _transactions.removeAll { it.id == id }
        recomputeBalance()
    }

    private fun recomputeBalance() {
        balance.value = _transactions.sumOf { it.amount }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAppScreen(vm: FinanceViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Finance") },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            BalanceHeader(balance = vm.balance.value)
            HorizontalDivider()
            TransactionList(
                items = vm.transactions,
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                onDelete = { vm.removeTransaction(it) }
            )
        }
        if (showDialog) {
            AddTransactionDialog(
                onDismiss = { showDialog = false },
                onAdd = { desc, amount, isExpense ->
                    vm.addTransaction(desc, amount, isExpense)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun BalanceHeader(balance: Double) {
    val color = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Balance", style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatCurrency(balance),
                color = color,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun TransactionList(
    items: List<TransactionItem>,
    contentPadding: PaddingValues,
    onDelete: (Long) -> Unit
) {
    LazyColumn(contentPadding = contentPadding) {
        items(items, key = { it.id }) { item ->
            TransactionRow(item = item, onDelete = onDelete)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
        }
    }
}

@Composable
fun TransactionRow(item: TransactionItem, onDelete: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.description, style = MaterialTheme.typography.bodyLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = formatCurrency(item.amount),
                color = if (item.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            IconButton(onClick = { onDelete(item.id) }) {
                Text("×")
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Boolean) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { isExpense = true }) { Text("Expense") }
                    TextButton(onClick = { isExpense = false }) { Text("Income") }
                    Text(if (isExpense) "Marked as Expense" else "Marked as Income")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(description, amount, isExpense) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatCurrency(value: Double): String {
    val sign = if (value < 0) "-" else ""
    val abs = kotlin.math.abs(value)
    return "$sign$" + String.format(java.util.Locale.US, "%.2f", abs)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FinanceTestTheme {
        BalanceHeader(balance = 1234.56)
    }
}