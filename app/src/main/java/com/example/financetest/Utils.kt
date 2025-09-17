package com.example.financetest

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.financetest.ui.theme.FinanceTestTheme

fun formatCurrency(value: Double): String {
    val sign = if (value < 0) "-" else ""
    val abs = kotlin.math.abs(value)
    return "$sign$" + String.format(java.util.Locale.US, "%.2f", abs)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FinanceTestTheme {
        // Use BalanceHeader from Components
        BalanceHeader(balance = 1234.56)
    }
}
