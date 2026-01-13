package com.example.ovoshebaza

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions

fun sendOrderViaTelegram(context: Context, message: String) {
    // 1. Кладём текст заказа в буфер обмена
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Заказ", message)
    clipboard.setPrimaryClip(clip)

    val pm = context.packageManager
    val username = "Mafee90"

    try {
        // 2. Сначала пробуем открыть Telegram напрямую через tg://
        val tgUri = Uri.parse("tg://resolve?domain=$username")
        val tgIntent = Intent(Intent.ACTION_VIEW, tgUri).apply {
            // пробуем стандартный пакет телеги
            setPackage("org.telegram.messenger")
        }

        if (tgIntent.resolveActivity(pm) != null) {
            context.startActivity(tgIntent)
        } else {
            // 3. Если так не получилось — пробуем открыть через https://t.me/ в браузере
            val webUri = Uri.parse("https://t.me/$username")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)

            context.startActivity(webIntent)
        }

        Toast.makeText(
            context,
            "Текст заказа скопирован. Откройте чат с вами и вставьте сообщение.",
            Toast.LENGTH_LONG
        ).show()
    } catch (e: Exception) {
        // Если вообще нечем открыть (нет ни телеги, ни браузера)
        Toast.makeText(
            context,
            "Не удалось открыть Telegram или браузер для ссылки.",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun sendOrderViaFirebaseTelegram(
    context: Context,
    order: Map<String, Any>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val functions = Firebase.functions

    functions
        .getHttpsCallable("sendOrderToTelegram")
        .call(order)
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Ошибка отправки")
        }
}