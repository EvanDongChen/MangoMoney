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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAppScreen(vm: FinanceViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            var totalDragX = 0f
                            var totalDragY = 0f
                            detectDragGestures(
                                onDragStart = { totalDragX = 0f; totalDragY = 0f },
                                onDragEnd = {
                                    if (kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY)) {
                                        // Horizontal swipe - switch tabs
                                        if (totalDragX > 80f) {
                                            selectedTab = if (selectedTab > 0) selectedTab - 1 else selectedTab
                                        } else if (totalDragX < -80f) {
                                            selectedTab = if (selectedTab < 4) selectedTab + 1 else selectedTab
                                        }
                                    } else if (selectedTab == 2 && kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX) && totalDragY < -50f) {
                                        // Vertical swipe up on FinanceTab - add transaction
                                        showBottomSheet = true
                                    }
                                }
                            ) { _, dragAmount ->
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                            }
                        }
                ) {
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
                        icon = { Icon(Icons.Outlined.Notifications, contentDescription = "Reminders") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Outlined.InsertChart, contentDescription = "Analytics") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Outlined.ListAlt, contentDescription = "Transactions") }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Outlined.Flag, contentDescription = "Goals") }
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
                    val ok = vm.addTransaction(desc, amount, isExpense, tags)
                    if (!ok) {
                        Toast.makeText(ctx, "Invalid amount: $amount", Toast.LENGTH_LONG).show()
                    } else {
                        showBottomSheet = false
                    }
                },
                availableTags = vm.tags
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceTab(vm: FinanceViewModel, showBottomSheet: Boolean, onShowBottomSheet: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    var showImagePicker by remember { mutableStateOf(false) }
    var showTransactionPreview by remember { mutableStateOf(false) }
    var parsedTransactions by remember { mutableStateOf<List<ParsedTransaction>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val ocrService = remember { OCRService() }
    val numberParser = remember { NumberParser() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.moneymango),
                    contentDescription = "Money Mango Logo",
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.FillWidth
                )
                DottedDivider(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp).height(1.dp))
            }
            BalanceHeader(balance = vm.balance.value)
            
            // Quick action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showImagePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "Upload Receipt")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Receipt")
                }
                OutlinedButton(
                    onClick = { onShowBottomSheet(true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Manually")
                }
            }
            
            // Monthly goal circle
            GoalCircleCard(
                title = "Monthly Goal",
                goalAmount = vm.monthlyGoal.value,
                spentAmount = vm.getSpentFor(GoalPeriod.MONTHLY)
            )
            HorizontalDivider()
            TransactionList(
                items = vm.getFilteredTransactions(),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                onDelete = { vm.removeTransaction(it) }
            )

            // Visual indicator for swipe up
            SwipeUpIndicator(
                onSwipeUp = { /* No longer needed - swipe handled by parent Box */ }
            )
        }
    }
    
    // Image picker bottom sheet
    if (showImagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showImagePicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SimpleImagePickerBottomSheet(
                onDismiss = { showImagePicker = false },
                onImageSelected = { bitmap ->
                    showImagePicker = false
                    coroutineScope.launch {
                        try {
                            val extractedText = ocrService.extractTextFromImage(bitmap)
                            val transactions = numberParser.parseTextForTransactions(extractedText)
                            parsedTransactions = transactions
                            showTransactionPreview = true
                        } catch (e: Exception) {
                            // Handle error - could show a toast or error dialog
                            showImagePicker = false
                        }
                    }
                }
            )
        }
    }
    
    // Transaction preview bottom sheet
    if (showTransactionPreview) {
        ModalBottomSheet(
            onDismissRequest = { showTransactionPreview = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            TransactionPreviewScreen(
                parsedTransactions = parsedTransactions,
                availableTags = vm.tags,
                onAddTag = { name, color -> vm.addTag(name, color) },
                onConfirm = { editableTransactions ->
                    try {
                        editableTransactions.forEach { editable ->
                            // Validate amount before adding (sanitization handled in ViewModel)
                            val amount = editable.amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                            android.util.Log.d("TransactionPreview", "Processing transaction: amount='${editable.amount}', parsed=$amount, description='${editable.description}'")

                            if (amount != null && amount > 0) {
                                val ok = vm.addTransaction(
                                    editable.description,
                                    editable.amount,
                                    editable.isExpense,
                                    editable.tags.toList()
                                )
                                if (ok) {
                                    android.util.Log.d("TransactionPreview", "Successfully added transaction")
                                } else {
                                    android.util.Log.w("TransactionPreview", "Failed to add transaction: amount='${editable.amount}'")
                                }
                            } else {
                                android.util.Log.w("TransactionPreview", "Skipping invalid transaction: amount='${editable.amount}'")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TransactionPreview", "Error adding transactions: ${e.message}", e)
                        Toast.makeText(ctx, "Failed to add transactions: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        showTransactionPreview = false
                    }
                },
                onCancel = { showTransactionPreview = false }
            )
        }
    }
}

@Composable
fun TagsTab(vm: FinanceViewModel) {
    var showAddTagDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.transactions),
                contentDescription = "Transactions Image",
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(200.dp),
                contentScale = ContentScale.FillWidth
            )
            DottedDivider(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp).height(1.dp))
        }
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
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.analytics),
                contentDescription = "Analytics Image",
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(200.dp),
                contentScale = ContentScale.FillWidth
            )
            DottedDivider(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp).height(1.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.goals),
                    contentDescription = "Goals Image",
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(200.dp),
                        contentScale = ContentScale.FillWidth
                )
                DottedDivider(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp).height(1.dp))
            }
        }

        item {
            GoalRow(
                title = "Daily",
                goalAmount = vm.dailyGoal.value,
                spentAmount = vm.getSpentFor(GoalPeriod.DAILY),
                onSet = { vm.setGoal(GoalPeriod.DAILY, it) }
            )
        }
        item {
            GoalRow(
                title = "Weekly",
                goalAmount = vm.weeklyGoal.value,
                spentAmount = vm.getSpentFor(GoalPeriod.WEEKLY),
                onSet = { vm.setGoal(GoalPeriod.WEEKLY, it) }
            )
        }
        item {
            GoalRow(
                title = "Biweekly",
                goalAmount = vm.biweeklyGoal.value,
                spentAmount = vm.getSpentFor(GoalPeriod.BIWEEKLY),
                onSet = { vm.setGoal(GoalPeriod.BIWEEKLY, it) }
            )
        }
        item {
            GoalRow(
                title = "Monthly",
                goalAmount = vm.monthlyGoal.value,
                spentAmount = vm.getSpentFor(GoalPeriod.MONTHLY),
                onSet = { vm.setGoal(GoalPeriod.MONTHLY, it) }
            )
        }
    }
}

@Composable
fun RemindersTab(vm: FinanceViewModel) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Start with now rounded to next hour
    var selectedDateTime by remember {
        mutableStateOf(
            ZonedDateTime.now().plusMinutes(60 - (ZonedDateTime.now().minute % 60).toLong())
        )
    }
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.reminders),
                contentDescription = "Reminders Image",
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(200.dp),
                contentScale = ContentScale.FillWidth
            )
            DottedDivider(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp).height(1.dp))
        }

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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        val z = selectedDateTime
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                selectedDateTime = selectedDateTime
                                    .withYear(year)
                                    .withMonth(month + 1)
                                    .withDayOfMonth(dayOfMonth)
                            },
                            z.year,
                            z.monthValue - 1,
                            z.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Date: ${selectedDateTime.format(dateFormatter)}") }
                TextButton(
                    onClick = {
                        val z = selectedDateTime
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                selectedDateTime = selectedDateTime
                                    .withHour(hour)
                                    .withMinute(minute)
                            },
                            z.hour,
                            z.minute,
                            false
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Time: ${selectedDateTime.format(timeFormatter)}") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                            val millis = selectedDateTime.withSecond(0).withNano(0).toInstant().toEpochMilli()

                            if (millis <= System.currentTimeMillis()) {
                                Toast.makeText(context, "Please pick a future date/time for the reminder", Toast.LENGTH_LONG).show()
                                return@TextButton
                            }

                            try {
                                val id = vm.addReminder(title, amount.ifBlank { null }, millis)
                                // schedule notification; protect against any runtime exception so the app doesn't crash
                                scheduleReminderNotification(
                                    context,
                                    id,
                                    title.ifBlank { "Reminder" },
                                    amount.ifBlank { null }?.toDoubleOrNull(),
                                    millis
                                )
                                // only clear inputs if scheduling succeeded
                                title = ""
                                amount = ""
                                selectedDateTime = ZonedDateTime.now(ZoneId.systemDefault()).plusHours(1)
                            } catch (e: Exception) {
                                // remove the reminder we may have added (scheduling failed)
                                try {
                                    // If id was created, remove any matching reminders; safe to call even if none
                                    // (we don't have id here if addReminder failed before returning, but calling remove by time-based search to be safe)
                                    // Remove any reminders scheduled at this due time
                                    vm.reminders.filter { it.dueAtMillis == millis }.forEach { vm.removeReminder(it.id) }
                                } catch (_: Exception) {}
                                Toast.makeText(context, "Failed to schedule reminder: ${e.message}", Toast.LENGTH_LONG).show()
                                Log.e("RemindersTab", "Failed to schedule reminder", e)
                            }
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
                    onDelete = {
                        vm.removeReminder(it)
                        cancelReminderNotification(context, it)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}
