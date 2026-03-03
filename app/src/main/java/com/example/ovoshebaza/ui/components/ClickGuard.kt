package com.example.ovoshebaza.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberClickGate(delayMs: Long = 600L): () -> Boolean {
    var isEnabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    return {
        if (!isEnabled) {
            false
        } else {
            isEnabled = false
            scope.launch {
                delay(delayMs)
                isEnabled = true
            }
            true
        }
    }
}