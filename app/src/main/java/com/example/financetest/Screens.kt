package com.example.financetest

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api

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
                    detectDragGestures(
                        onDragEnd = { }
                    ) { _, dragAmount ->
                        // Detect horizontal swipes
                        if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                            if (dragAmount.x > 50) {
                                // Swipe right - go to previous tab
                                selectedTab = if (selectedTab > 0) selectedTab - 1 else selectedTab
                            } else if (dragAmount.x < -50) {
                                // Swipe left - go to next tab
                                selectedTab = if (selectedTab < 1) selectedTab + 1 else selectedTab
                            }
                        }
                    }
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
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
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
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
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
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
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
fun AnalyticsTab(vm: FinanceViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Analytics", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
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
