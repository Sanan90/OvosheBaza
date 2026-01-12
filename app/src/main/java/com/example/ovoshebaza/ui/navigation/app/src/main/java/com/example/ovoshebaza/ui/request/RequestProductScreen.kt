package com.example.ovoshebaza.ui.request

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ovoshebaza.buildRequestMap
import com.example.ovoshebaza.sendOrderViaFirebaseTelegram

@Composable
fun RequestProductScreen() {
    val context = LocalContext.current

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var requestedProduct by remember { mutableStateOf("") }
    var requestedQuantity by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var isSendingRequest by remember { mutableStateOf(false) }

    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Заявка на редкий товар",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Если нужного товара нет в каталоге — оставьте заявку, и мы постараемся привезти его.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = requestedProduct,
            onValueChange = { requestedProduct = it },
            label = { Text("Что вам нужно (товар)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = requestedQuantity,
            onValueChange = { requestedQuantity = it },
            label = { Text("Желаемое количество (кг/шт)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Ваше имя") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customerPhone,
            onValueChange = { customerPhone = it },
            label = { Text("Телефон для связи") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Комментарий (необязательно)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                when {
                    requestedProduct.isBlank() -> {
                        errorText = "Пожалуйста, укажите, какой товар вам нужен."
                    }

                    customerName.isBlank() -> {
                        errorText = "Пожалуйста, укажите ваше имя."
                    }

                    customerPhone.isBlank() -> {
                        errorText = "Пожалуйста, укажите телефон."
                    }

                    else -> {
                        errorText = null

                        val requestPayload = buildRequestMap(
                            customerName = customerName,
                            customerPhone = customerPhone,
                            requestedProduct = requestedProduct,
                            requestedQuantity = requestedQuantity.ifBlank { "Не указано" },
                            comment = comment
                        )

                        // Используем ту же функцию, что и для заказа
                        isSendingRequest = true
                        sendOrderViaFirebaseTelegram(
                            context = context,
                            order = requestPayload,
                            onSuccess = {
                                isSendingRequest = false
                                Toast.makeText(
                                    context,
                                    "Заявка отправлена в Telegram ✅",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onError = { err ->
                                isSendingRequest = false
                                Toast.makeText(
                                    context,
                                    "Ошибка отправки: $err",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )

                        // По желанию — очищаем поля после отправки
                        requestedProduct = ""
                        requestedQuantity = ""
                        customerName = ""
                        customerPhone = ""
                        comment = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSendingRequest
        ) {
            Text("Отправить заявку")
        }
    }

    if (isSendingRequest) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Отправка заявки") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Отправляем заявку, пожалуйста подождите…")
                }
            },
            confirmButton = {}
        )
    }
}