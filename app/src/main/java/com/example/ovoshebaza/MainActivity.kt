package com.example.ovoshebaza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ovoshebaza.ui.theme.VeggieTheme
import com.example.ovoshebaza.app.AppRoot
import android.graphics.Color as AndroidColor


// Главная Activity — точка входа в приложение
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.parseColor("#2E7D32")

        // setContent — запускаем Compose UI
        setContent {
            // Можно потом сделать свою тему, пока используем Material3 по умолчанию
            VeggieTheme {
                AppRoot()
            }

        }
    }
}

