package com.example.financetest

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

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
                                selectedTab = if (selectedTab < 3) selectedTab + 1 else selectedTab
                            }
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Content area
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> RemindersTab(vm = vm)
                        1 -> AnalyticsTab(vm = vm)
                        2 -> FinanceTab(vm = vm, showBottomSheet = showBottomSheet, onShowBottomSheet = { showBottomSheet = it })
                        3 -> TagsTab(vm = vm)
                        4 -> GoalsTab(vm = vm)
                    }
                }

                // Bottom tab row
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Reminders") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Analytics") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Transactions") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Tags") }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Goals") }
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
    // Compute monthly spending (sum of expenses per month).
    val months = remember(vm.transactions) {
        // group transactions by month/year derived from id (epoch millis)
        val df = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")
        vm.transactions
            .filter { it.amount < 0 }
            .groupBy { tx ->
                val instant = java.time.Instant.ofEpochMilli(tx.id)
                val zdt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                df.format(zdt)
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.first }
    }

    val maxAbs = months.maxOfOrNull { kotlin.math.abs(it.second) } ?: 1.0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Monthly Spending", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        if (months.isEmpty()) {
            Text("No expense transactions yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(months) { (month, total) ->
                    val spent = kotlin.math.abs(total)
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(month, style = MaterialTheme.typography.bodyLarge)
                            Text(formatCurrency(-spent), style = MaterialTheme.typography.bodyLarge)
                        }
                        // visual bar
                        val fraction = (spent / maxAbs).coerceIn(0.0, 1.0)
                        androidx.compose.foundation.layout.Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier
                                .fillMaxWidth(fraction.toFloat())
                                .height(12.dp)
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsTab(vm: FinanceViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Top) {
        Text("Goals", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        GoalRow(
            title = "Daily",
            goalAmount = vm.dailyGoal.value,
            spentAmount = vm.getSpentFor(GoalPeriod.DAILY),
            onSet = { vm.setGoal(GoalPeriod.DAILY, it) }
        )
        GoalRow(
            title = "Weekly",
            goalAmount = vm.weeklyGoal.value,
            spentAmount = vm.getSpentFor(GoalPeriod.WEEKLY),
            onSet = { vm.setGoal(GoalPeriod.WEEKLY, it) }
        )
        GoalRow(
            title = "Biweekly",
            goalAmount = vm.biweeklyGoal.value,
            spentAmount = vm.getSpentFor(GoalPeriod.BIWEEKLY),
            onSet = { vm.setGoal(GoalPeriod.BIWEEKLY, it) }
        )
        GoalRow(
            title = "Monthly",
            goalAmount = vm.monthlyGoal.value,
            spentAmount = vm.getSpentFor(GoalPeriod.MONTHLY),
            onSet = { vm.setGoal(GoalPeriod.MONTHLY, it) }
        )
    }
}

@Composable
fun RemindersTab(vm: FinanceViewModel) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    val df = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    fun parseMillisOrNull(text: String): Long? = try {
        val zdt = java.time.LocalDateTime.parse(text, df).atZone(java.time.ZoneId.systemDefault())
        zdt.toInstant().toEpochMilli()
    } catch (t: Throwable) { null }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Reminders",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Due (yyyy-MM-dd HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        val millis = parseMillisOrNull(dateText) ?: return@TextButton
                        vm.addReminder(title, amount.ifBlank { null }, millis)
                        title = ""
                        amount = ""
                        dateText = ""
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Add") }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            items(vm.reminders, key = { it.id }) { r ->
                ReminderRow(
                    reminder = r,
                    onToggleDone = { vm.toggleReminderDone(it) },
                    onDelete = { vm.removeReminder(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}
