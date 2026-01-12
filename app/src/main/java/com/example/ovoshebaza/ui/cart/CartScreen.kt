package com.example.ovoshebaza.ui.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ovoshebaza.CartItem
import com.example.ovoshebaza.Constants.DELIVERY_FEE_RUB
import com.example.ovoshebaza.PaymentMethod
import com.example.ovoshebaza.UnitType
import com.example.ovoshebaza.buildOrderMap
import com.example.ovoshebaza.formatQuantity
import com.example.ovoshebaza.loadUserProfile
import com.example.ovoshebaza.saveOrderToHistory
import com.example.ovoshebaza.saveUserProfileFromOrder
import com.example.ovoshebaza.sendOrderViaFirebaseTelegram
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onUpdateQuantity: (String, Double) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onClearCart: () -> Unit
) {
    val context = LocalContext.current

    // Считаем примерную сумму заказа
    val itemsSubtotal = cartItems.sumOf { it.product.price * it.quantity }
    val isFreeDelivery = itemsSubtotal >= 1500.0
    val deliveryFee = if (isFreeDelivery) 0.0 else DELIVERY_FEE_RUB
    val paymentDiscountPercent = 0.05

    // Показывать ли диалог с формой оформления заказа
    var showOrderDialog by remember { mutableStateOf(false) }
    var isSendingOrder by remember { mutableStateOf(false) }

    // Поля клиента (для диалога)
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var customerAddresses by remember { mutableStateOf<List<String>>(emptyList()) }
    var addressMenuExpanded by remember { mutableStateOf(false) }
    var isManualAddress by remember { mutableStateOf(false) }
    var customerComment by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }

    LaunchedEffect(showOrderDialog) {
        if (!showOrderDialog) return@LaunchedEffect

        // 1) Телефон из авторизации (если поле пустое)
        val authPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber
        if (customerPhone.isBlank() && !authPhone.isNullOrBlank()) {
            customerPhone = authPhone
        }

        // 2) Имя/адрес из Firestore (только если поля пустые)
        loadUserProfile(
            onResult = { profile ->
                if (profile == null) return@loadUserProfile
                if (customerName.isBlank() && profile.name.isNotBlank()) customerName = profile.name
                if (customerPhone.isBlank() && profile.phone.isNotBlank()) customerPhone = profile.phone
                if (customerAddresses.isEmpty()) customerAddresses = profile.addresses
                if (customerAddress.isBlank()) {
                    val lastAddress = profile.lastAddress.ifBlank {
                        profile.addresses.lastOrNull().orEmpty()
                    }
                    if (lastAddress.isNotBlank()) {
                        customerAddress = lastAddress
                    }
                }
                if (profile.addresses.size > 1) {
                    isManualAddress = false
                }
            }
        )
    }

    // Текст ошибки в диалоге
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (cartItems.isEmpty()) {
            // Если корзина пустая
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Корзина пуста")
            }
        } else {
            // Список товаров в корзине
            val discount = if (paymentMethod == PaymentMethod.CASH) {
                itemsSubtotal * paymentDiscountPercent
            } else {
                0.0
            }
            val totalPrice = itemsSubtotal - discount + deliveryFee
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onUpdateQuantity = onUpdateQuantity,
                        onRemoveFromCart = onRemoveFromCart
                    )
                }
                item {
                    CartSummaryCard(
                        itemsSubtotal = itemsSubtotal,
                        discount = discount,
                        deliveryFee = deliveryFee,
                        totalPrice = totalPrice,
                        isFreeDelivery = isFreeDelivery,
                        onCheckout = {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user == null) {
                                Toast.makeText(
                                    context,
                                    "Чтобы сделать заказ, авторизуйтесь.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@CartSummaryCard
                            }
                            showOrderDialog = true
                        },
                        paymentMethod = paymentMethod,
                        onPaymentMethodChange = { paymentMethod = it }
                    )
                }
            }
        }
    }

    // ---------- ДИАЛОГ ОФОРМЛЕНИЯ ЗАКАЗА ----------

    if (showOrderDialog && cartItems.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                showOrderDialog = false
                errorText = null
            },
            title = {
                Text("Оформление заказа")
            },
            text = {
                Column {
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
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (customerAddresses.size <= 1 || isManualAddress) {
                        OutlinedTextField(
                            value = customerAddress,
                            onValueChange = { customerAddress = it },
                            label = { Text("Адрес доставки") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = addressMenuExpanded,
                            onExpandedChange = { addressMenuExpanded = !addressMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = customerAddress,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Адрес доставки") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = addressMenuExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = addressMenuExpanded,
                                onDismissRequest = { addressMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Новый адрес") },
                                    onClick = {
                                        addressMenuExpanded = false
                                        isManualAddress = true
                                        customerAddress = ""
                                    }
                                )
                                customerAddresses.forEach { address ->
                                    DropdownMenuItem(
                                        text = { Text(address) },
                                        onClick = {
                                            customerAddress = address
                                            isManualAddress = false
                                            addressMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customerComment,
                        onValueChange = { customerComment = it },
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
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            // проверка полей (как у тебя)
                            when {
                                customerName.isBlank() -> errorText = "Пожалуйста, укажите имя."
                                customerPhone.isBlank() -> errorText =
                                    "Пожалуйста, укажите телефон."

                                customerAddress.isBlank() -> errorText =
                                    "Пожалуйста, укажите адрес доставки."

                                else -> {
                                    errorText = null

                                    isSendingOrder = true

                                    val discount = if (paymentMethod == PaymentMethod.CASH) {
                                        itemsSubtotal * paymentDiscountPercent
                                    } else {
                                        0.0
                                    }
                                    val total = itemsSubtotal - discount + deliveryFee

                                    val order = buildOrderMap(
                                        cartItems = cartItems,
                                        customerName = customerName,
                                        customerPhone = customerPhone,
                                        customerAddress = customerAddress,
                                        comment = customerComment,
                                        paymentMethod = paymentMethod,
                                        deliveryFee = deliveryFee,
                                        discount = discount,
                                        total = total
                                    )

                                    saveOrderToHistory(
                                        order = order,
                                        channel = "TELEGRAM",
                                        onDone = { orderId ->
                                            val user = FirebaseAuth.getInstance().currentUser
                                            if (user == null) {
                                                isSendingOrder = false
                                                Toast.makeText(
                                                    context,
                                                    "Пользователь не авторизован",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@saveOrderToHistory
                                            }
                                            val payload = order.toMutableMap().apply {
                                                put("uid", user.uid)
                                                put("orderId", orderId)
                                            }
                                            sendOrderViaFirebaseTelegram(
                                                context = context,
                                                order = payload,
                                                onSuccess = {
                                                    saveUserProfileFromOrder(
                                                        name = customerName,
                                                        phone = customerPhone,
                                                        address = customerAddress
                                                    )

                                                    onClearCart()
                                                    isSendingOrder = false
                                                    Toast.makeText(
                                                        context,
                                                        "Заказ отправлен в Telegram ✅",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    showOrderDialog = false

                                                    // (по желанию) очистка полей после успешной отправки:
                                                    customerName = ""
                                                    customerPhone = ""
                                                    customerAddress = ""
                                                    customerComment = ""
                                                },
                                                onError = { err ->
                                                    isSendingOrder = false
                                                    Toast.makeText(
                                                        context,
                                                        "Ошибка отправки: $err",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            )
                                        },
                                        onError = { err ->
                                            isSendingOrder = false
                                            Toast.makeText(
                                                context,
                                                "Ошибка сохранения: $err",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !isSendingOrder
                    ) {
                        Text("Сделать заказ")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOrderDialog = false
                        errorText = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (isSendingOrder) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Отправка заказа") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Отправляем данные, пожалуйста подождите…")
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onUpdateQuantity: (String, Double) -> Unit,
    onRemoveFromCart: (String) -> Unit
) {
    val unitLabel = when (item.product.unit) {
        UnitType.KG -> "кг"
        UnitType.PIECE -> "шт"
    }
    val step = if (item.product.unit == UnitType.KG) 0.5 else 1.0
    val itemTotal = item.product.price * item.quantity

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!item.product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.product.imageUrl,
                        contentDescription = item.product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Фото", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2
                    )
                    Text(
                        text = "${itemTotal.toInt()} ₽",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${formatQuantity(item.quantity)} $unitLabel • ${item.product.price.toInt()} ₽ / $unitLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newQuantity = item.quantity - step
                                    if (newQuantity <= 0.0) {
                                        onRemoveFromCart(item.product.id)
                                    } else {
                                        onUpdateQuantity(item.product.id, newQuantity)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Уменьшить"
                                )
                            }
                            Text(
                                text = formatQuantity(item.quantity),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    onUpdateQuantity(
                                        item.product.id,
                                        item.quantity + step
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Увеличить"
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun CartSummaryCard(
    itemsSubtotal: Double,
    discount: Double,
    deliveryFee: Double,
    totalPrice: Double,
    isFreeDelivery: Boolean,
    onCheckout: () -> Unit,
    paymentMethod: PaymentMethod,
    onPaymentMethodChange: (PaymentMethod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Способ оплаты",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = paymentMethod == PaymentMethod.CASH,
                    onClick = { onPaymentMethodChange(PaymentMethod.CASH) }
                )
                Text(PaymentMethod.CASH.label)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = paymentMethod == PaymentMethod.CARD,
                    onClick = { onPaymentMethodChange(PaymentMethod.CARD) }
                )
                Text(PaymentMethod.CARD.label)
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Сумма")
                Text("${itemsSubtotal.toInt()} ₽")
            }

            if (discount > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Скидка за наличные")
                    Text("-${discount.toInt()} ₽")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Доставка")
                Text(if (deliveryFee > 0.0) "${deliveryFee.toInt()} ₽" else "Бесплатно")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Итого",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "${totalPrice.toInt()} ₽",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            if (!isFreeDelivery) {
                val remaining = (1500.0 - itemsSubtotal).coerceAtLeast(0.0)
                Text(
                    text = "Добавьте еще ${remaining.toInt()} ₽ для бесплатной доставки.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Фактическая сумма может немного отличаться из-за точного веса (+/− ~100 г).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Оформить заказ")
            }
        }
    }
}