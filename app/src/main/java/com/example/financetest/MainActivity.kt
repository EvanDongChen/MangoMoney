package com.example.financetest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.financetest.ui.theme.FinanceTestTheme

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