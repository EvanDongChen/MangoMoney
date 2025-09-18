package com.example.financetest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

data class EditableTransaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    var amount: String,
    var description: String,
    var isExpense: Boolean = true,
    var isSelected: Boolean = true,
    var tags: Set<String> = emptySet(),
    val originalConfidence: Float
)

@Composable
fun TransactionPreviewScreen(
    parsedTransactions: List<ParsedTransaction>,
    availableTags: List<Tag>,
    onAddTag: (String, Long) -> Unit,
    onConfirm: (List<EditableTransaction>) -> Unit,
    onCancel: () -> Unit
) {
    var editableTransactions by remember {
        mutableStateOf(
            parsedTransactions.map { parsed ->
                EditableTransaction(
                    amount = String.format("%.2f", parsed.amount),
                    description = parsed.description,
                    isExpense = true, // Default to expense
                    isSelected = true,
                    tags = emptySet(),
                    originalConfidence = parsed.confidence
                )
            }
        )
    }
    
    var showAddTagDialog by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Review Transactions",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = { showAddTagDialog = true }) {
                Text("Add Tag")
            }
        }
        
        Text(
            text = "Review and edit the transactions found in your receipt. Uncheck any you don't want to add.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Transaction list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(editableTransactions) { transaction ->
                EditableTransactionCard(
                    transaction = transaction,
                    availableTags = availableTags,
                    onAmountChange = { newAmount ->
                        editableTransactions = editableTransactions.map {
                            if (it.id == transaction.id) it.copy(amount = newAmount) else it
                        }
                    },
                    onDescriptionChange = { newDescription ->
                        editableTransactions = editableTransactions.map {
                            if (it.id == transaction.id) it.copy(description = newDescription) else it
                        }
                    },
                    onExpenseToggle = {
                        editableTransactions = editableTransactions.map {
                            if (it.id == transaction.id) it.copy(isExpense = !it.isExpense) else it
                        }
                    },
                    onSelectionToggle = {
                        editableTransactions = editableTransactions.map {
                            if (it.id == transaction.id) it.copy(isSelected = !it.isSelected) else it
                        }
                    },
                    onTagToggle = { tagName ->
                        editableTransactions = editableTransactions.map {
                            if (it.id == transaction.id) {
                                val newTags = if (it.tags.contains(tagName)) it.tags - tagName else it.tags + tagName
                                it.copy(tags = newTags)
                            } else it
                        }
                    },
                    onRemove = {
                        editableTransactions = editableTransactions.filter { it.id != transaction.id }
                    }
                )
            }
        }
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val selectedTransactions = editableTransactions.filter { it.isSelected }
                    onConfirm(selectedTransactions)
                },
                modifier = Modifier.weight(1f),
                enabled = editableTransactions.any { it.isSelected }
            ) {
                Text("Add ${editableTransactions.count { it.isSelected }} Transaction(s)")
            }
        }
    }
    
    // Add tag dialog
        if (showAddTagDialog) {
            AddTagDialog(
                onDismiss = { showAddTagDialog = false },
                onAdd = { name, color ->
                    onAddTag(name, color)
                    showAddTagDialog = false
                }
            )
        }
}

@Composable
fun EditableTransactionCard(
    transaction: EditableTransaction,
    availableTags: List<Tag> = emptyList(),
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onExpenseToggle: () -> Unit,
    onSelectionToggle: () -> Unit,
    onTagToggle: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (transaction.isSelected) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with confidence and remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Selection checkbox
                    Checkbox(
                        checked = transaction.isSelected,
                        onCheckedChange = { onSelectionToggle() }
                    )
                    
                    // Confidence indicator
                    ConfidenceBadge(confidence = transaction.originalConfidence)
                }
                
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            }
            
            // Amount field
            OutlinedTextField(
                value = transaction.amount,
                onValueChange = { newValue ->
                    // Only allow valid number input
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        onAmountChange(newValue)
                    }
                },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                isError = transaction.amount.isNotEmpty() && transaction.amount.toDoubleOrNull() == null
            )
            
            // Description field
            OutlinedTextField(
                value = transaction.description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Expense/Income toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { if (!transaction.isExpense) onExpenseToggle() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (transaction.isExpense) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(if (transaction.isExpense) "✓ Expense" else "Expense")
                }
                TextButton(
                    onClick = { if (transaction.isExpense) onExpenseToggle() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (!transaction.isExpense) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(if (!transaction.isExpense) "✓ Income" else "Income")
                }
            }
            // Tags
            if (availableTags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), 
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(availableTags) { tag ->
                        TagChip(
                            name = tag.name,
                            isSelected = transaction.tags.contains(tag.name),
                            onClick = { onTagToggle(tag.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: Float) {
    val color = when {
        confidence > 0.7f -> Color(0xFF4CAF50) // Green
        confidence > 0.4f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
    
    val text = when {
        confidence > 0.7f -> "High"
        confidence > 0.4f -> "Medium"
        else -> "Low"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "$text Confidence",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}
