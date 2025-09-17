package com.example.financetest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

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
    val isExpense: Boolean,
    val tags: List<String> = emptyList()
)

data class Tag(
    val id: Long,
    val name: String,
    val color: Long
)

class FinanceViewModel : androidx.lifecycle.ViewModel() {
    private val _transactions = androidx.compose.runtime.mutableStateListOf<TransactionItem>()
    val transactions: List<TransactionItem> get() = _transactions

    private val _tags = androidx.compose.runtime.mutableStateListOf<Tag>()
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
        // Remove tag from all transactions
        _transactions.forEachIndexed { index, transaction ->
            _transactions[index] = transaction.copy(tags = transaction.tags.filter { it != _tags.find { tag -> tag.id == id }?.name })
        }
    }

    fun setTagFilter(tagName: String?) {
        selectedTagFilter.value = tagName
    }

    fun getFilteredTransactions(): List<TransactionItem> {
        return if (selectedTagFilter.value == null) {
            transactions
        } else {
            transactions.filter { it.tags.contains(selectedTagFilter.value) }
        }
    }

    private fun recomputeBalance() {
        balance.value = _transactions.sumOf { it.amount }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAppScreen(vm: FinanceViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Finance") },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    var totalX = 0f
                    var totalY = 0f
                    detectDragGestures(
                        onDragStart = {
                            totalX = 0f
                            totalY = 0f
                        },
                        onDrag = { _, dragAmount ->
                            totalX += dragAmount.x
                            totalY += dragAmount.y
                        },
                        onDragEnd = {
                            if (kotlin.math.abs(totalX) > kotlin.math.abs(totalY) && kotlin.math.abs(totalX) > 100f) {
                                val tabCount = 3
                                if (totalX < 0f) {
                                    // Swipe left - next tab (wrap to first from last)
                                    selectedTab = (selectedTab + 1) % tabCount
                                } else {
                                    // Swipe right - previous tab (wrap to last from first)
                                    selectedTab = (selectedTab - 1 + tabCount) % tabCount
                                }
                            }
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Content area
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> AnalyticsTab(vm = vm)
                        1 -> FinanceTab(vm = vm, showBottomSheet = showBottomSheet, onShowBottomSheet = { showBottomSheet = it })
                        2 -> TagsTab(vm = vm)
                    }
                }

                // Bottom tab row
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Analytics") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Transactions") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Tags") }
                    )
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AddTransactionBottomSheet(
                onDismiss = { showBottomSheet = false },
                onAdd = { desc, amount, isExpense, tags ->
                    vm.addTransaction(desc, amount, isExpense, tags)
                    showBottomSheet = false
                },
                availableTags = vm.tags
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceTab(vm: FinanceViewModel, showBottomSheet: Boolean, onShowBottomSheet: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { 
                        onShowBottomSheet(true)
                    }
                ) { _, dragAmount ->
                    // Only trigger on upward swipes (not horizontal)
                    if (dragAmount.y < -50 && kotlin.math.abs(dragAmount.x) < kotlin.math.abs(dragAmount.y)) {
                        onShowBottomSheet(true)
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BalanceHeader(balance = vm.balance.value)
            HorizontalDivider()
            TransactionList(
                items = vm.getFilteredTransactions(),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                onDelete = { vm.removeTransaction(it) }
            )
            
            SwipeUpIndicator(
                onSwipeUp = { onShowBottomSheet(true) }
            )
        }
    }
}

@Composable
fun TagsTab(vm: FinanceViewModel) {
    var showAddTagDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter section
        Text(
            text = "Filter by Tag",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TagChip(
                    name = "All",
                    isSelected = vm.selectedTagFilter.value == null,
                    onClick = { vm.setTagFilter(null) }
                )
            }
            items(vm.tags) { tag ->
                TagChip(
                    name = tag.name,
                    isSelected = vm.selectedTagFilter.value == tag.name,
                    onClick = { vm.setTagFilter(tag.name) }
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Tag management
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Tags",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = { showAddTagDialog = true }) {
                Text("Add Tag")
            }
        }
        
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vm.tags) { tag ->
                TagItem(
                    tag = tag,
                    onDelete = { vm.removeTag(tag.id) }
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // All transactions
        Text(
            text = "All Transactions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        TransactionList(
            items = vm.getFilteredTransactions(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
            onDelete = { vm.removeTransaction(it) }
        )
    }
    
    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onAdd = { name, color ->
                vm.addTag(name, color)
                showAddTagDialog = false
            }
        )
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
            if (item.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(item.tags) { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
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
fun AddTransactionBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (String, String, Boolean, List<String>) -> Unit,
    availableTags: List<Tag> = emptyList()
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RectangleShape)
                .padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RectangleShape)
            )
        }
        
        Text(
            text = "Add Transaction",
            style = MaterialTheme.typography.headlineSmall
        )
        
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
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { isExpense = true }) { 
                Text(if (isExpense) "✓ Expense" else "Expense") 
            }
            TextButton(onClick = { isExpense = false }) { 
                Text(if (!isExpense) "✓ Income" else "Income") 
            }
        }
        
        if (availableTags.isNotEmpty()) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleSmall
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableTags) { tag ->
                    TagChip(
                        name = tag.name,
                        isSelected = selectedTags.contains(tag.name),
                        onClick = { 
                            selectedTags = if (selectedTags.contains(tag.name)) {
                                selectedTags - tag.name
                            } else {
                                selectedTags + tag.name
                            }
                        }
                    )
                }
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) { 
                Text("Cancel") 
            }
            TextButton(
                onClick = { onAdd(description, amount, isExpense, selectedTags.toList()) },
                modifier = Modifier.weight(1f)
            ) { 
                Text("Add") 
            }
        }
    }
}

@Composable
fun SwipeUpIndicator(
    onSwipeUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onSwipeUp() }
                ) { _, _ -> }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Swipe up to add transaction",
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Swipe up to add transaction",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TagChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
    Text(
            text = name,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun TagItem(
    tag: Tag,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        androidx.compose.ui.graphics.Color(tag.color),
                        CircleShape
                    )
            )
            Text(tag.name)
        }
        IconButton(onClick = onDelete) {
            Text("×")
        }
    }
}

@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF6200EE) }
    
    val colors = listOf(
        0xFF6200EE, 0xFF03DAC6, 0xFF018786, 0xFF03DAC5,
        0xFFCF6679, 0xFFBB86FC, 0xFF3700B3, 0xFF03DAC6
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tag Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Color", style = MaterialTheme.typography.titleSmall)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    androidx.compose.ui.graphics.Color(color),
                                    CircleShape
                                )
                                .clickable { selectedColor = color }
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, selectedColor) },
                enabled = name.isNotBlank()
            ) { 
                Text("Add") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel") 
            }
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

@Composable
fun SideTabButton(title: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = title, color = contentColor)
    }
}

@Composable
fun AnalyticsTab(vm: FinanceViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Analytics", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Summary", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Balance: ${formatCurrency(vm.balance.value)}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Total Transactions: ${vm.transactions.size}")
                Spacer(modifier = Modifier.height(8.dp))
                val expense = vm.transactions.filter { it.amount < 0 }.sumOf { it.amount }
                val income = vm.transactions.filter { it.amount > 0 }.sumOf { it.amount }
                Text(text = "Total Income: ${formatCurrency(income)}")
                Text(text = "Total Expense: ${formatCurrency(expense)}")
            }
        }
    }
}