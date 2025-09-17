package com.example.financetest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextAlign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Semi-transparent dark green (alpha 0x80)
val TransparentDarkGreen = Color(0x8033882B)

@Composable
fun BalanceHeader(balance: Double) {
    val color = if (balance >= 0) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = TransparentDarkGreen)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatCurrency(balance),
                color = color,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text("Balance", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TransactionList(
    items: List<TransactionItem>,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
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
            Text(item.description, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            if (item.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(item.tags) { tag ->
                        Text(
                            text = "#$tag",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
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
                color = if (item.amount >= 0) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.error
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
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
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
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall
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
            androidx.compose.material3.Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Swipe up to add transaction",
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Swipe up to add transaction",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
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
                if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
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
                        Color(tag.color),
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

                Text("Color", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color(color),
                                    CircleShape
                                )
                                .clickable { selectedColor = color }
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(
                                            2.dp,
                                            androidx.compose.material3.MaterialTheme.colorScheme.primary,
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

@Composable
fun SideTabButton(title: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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
fun GoalRow(
    title: String,
    goalAmount: Double,
    spentAmount: Double,
    onSet: (String) -> Unit
) {
    var input by remember { mutableStateOf(if (goalAmount > 0) goalAmount.toString() else "") }
    val progress = if (goalAmount > 0) (spentAmount / goalAmount).coerceIn(0.0, 1.0) else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = TransparentDarkGreen)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(formatCurrency(-spentAmount), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(progress = progress.toFloat(), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Goal amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onSet(input) }) { Text("Set") }
            }
            if (goalAmount > 0) {
                val remaining = (goalAmount - spentAmount).coerceAtLeast(0.0)
                Text(
                    text = "Remaining: ${formatCurrency(-remaining)}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ReminderRow(
    reminder: Reminder,
    onToggleDone: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val dt = Instant.ofEpochMilli(reminder.dueAtMillis).atZone(ZoneId.systemDefault())
    val df = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(reminder.title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            val subtitle = buildString {
                append(df.format(dt))
                reminder.amount?.let { append("  •  ").append(formatCurrency(-kotlin.math.abs(it))) }
            }
            Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onToggleDone(reminder.id) }) { Text(if (reminder.isDone) "Undone" else "Done") }
            TextButton(onClick = { onDelete(reminder.id) }) { Text("Delete") }
        }
    }
}

@Composable
fun GoalCircleCard(
    title: String,
    goalAmount: Double,
    spentAmount: Double
) {
    val progress = if (goalAmount > 0) (spentAmount / goalAmount).coerceIn(0.0, 1.0) else 0.0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = TransparentDarkGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bigger circle
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                CircularProgressIndicator(progress = progress.toFloat(), strokeWidth = 8.dp)
                Text(
                    text = if (goalAmount > 0) "${(progress * 100).toInt()}%" else "—",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                )
            }
            // Title below the circle
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            val remaining = if (goalAmount > 0) (goalAmount - spentAmount).coerceAtLeast(0.0) else 0.0
            val subtitle = if (goalAmount > 0) {
                "Spent ${formatCurrency(-kotlin.math.abs(spentAmount))} of ${formatCurrency(-kotlin.math.abs(goalAmount))} (Remaining ${formatCurrency(-remaining)})"
            } else {
                "No goal set"
            }
            Text(
                subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
