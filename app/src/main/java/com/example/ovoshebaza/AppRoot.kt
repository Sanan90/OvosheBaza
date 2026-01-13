package com.example.ovoshebaza.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ovoshebaza.ui.root.VeggieShopApp
import com.example.ovoshebaza.ui.splash.SplashScreen
import kotlinx.coroutines.delay

@Composable
fun AppRoot() {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1200)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        VeggieShopApp()
    }
}
