package com.example.ovoshebaza

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import android.widget.Toast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material3.AlertDialog

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ProfileScreen(
    products: List<Product>,
    onAddToCart: (Product, Double) -> Unit,
    cartItems: List<CartItem>,
    orders: List<OrderSummary>,
    isOrdersLoading: Boolean,
    ordersError: String?,
    onClearCart: () -> Unit,
    onAuthRequested: () -> Unit,
    onOpenProduct: (String) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val dateFormat = remember {
        SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
    }
    val profileSaver = remember {
        listSaver<UserProfile?, Any>(
            save = { stored ->
                if (stored == null) {
                    emptyList()
                } else {
                    listOf(
                        stored.name,
                        stored.phone,
                        stored.addresses,
                        stored.lastAddress
                    )
                }
            },
            restore = { restored ->
                if (restored.isEmpty()) {
                    null
                } else {
                    @Suppress("UNCHECKED_CAST")
                    UserProfile(
                        name = restored[0] as String,
                        phone = restored[1] as String,
                        addresses = restored[2] as List<String>,
                        lastAddress = restored[3] as String
                    )
                }
            }
        )
    }
    val context = LocalContext.current
    val cachedProfile = getCachedUserProfile(user?.uid)
    var profile by rememberSaveable(user?.uid, stateSaver = profileSaver) {
        mutableStateOf<UserProfile?>(cachedProfile)
    }
    var isLoading by remember { mutableStateOf(profile == null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var addressExpanded by rememberSaveable(user?.uid) { mutableStateOf(false) }
    var newAddressInput by rememberSaveable(user?.uid) { mutableStateOf("") }
    var ordersExpanded by rememberSaveable(user?.uid) { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var pendingOrder by remember { mutableStateOf<OrderSummary?>(null) }
    var expandedOrderIds by rememberSaveable(user?.uid) { mutableStateOf(setOf<String>()) }

    DisposableEffect(user?.uid) {
        if (user == null) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        isLoading = profile == null
        errorText = null

        val profileRegistration = listenUserProfile(
            onResult = { loaded ->
                profile = loaded
                isLoading = false
            },
            onError = { error ->
                errorText = error
                isLoading = false
            }
        )

        onDispose {
            profileRegistration?.remove()
        }
    }

    LaunchedEffect(user?.uid, profile) {
        setCachedUserProfile(user?.uid, profile)
    }

    if (user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Вы не авторизованы",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Авторизуйтесь, чтобы увидеть профиль и заказы",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onAuthRequested) {
                    Text("Авторизоваться")
                }
            }
        }
        return
    }

    var savedIndex by rememberSaveable(user?.uid) { mutableStateOf(0) }
    var savedOffset by rememberSaveable(user?.uid) { mutableStateOf(0) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedIndex,
        initialFirstVisibleItemScrollOffset = savedOffset
    )
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collectLatest { (index, offset) ->
                savedIndex = index
                savedOffset = offset
            }
    }


    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ava),
                    contentDescription = "Аватар",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
                Text(
                    text = profile?.name?.ifBlank { "Без имени" } ?: "Без имени",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.alpha(if (profile == null && isLoading) 0f else 1f)
                )
                if (profile == null && isLoading) {
                    SkeletonLine(
                        modifier = Modifier
                            .height(22.dp)
                            .width(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        val combinedErrorText = errorText ?: ordersError
        if (combinedErrorText != null) {
            item {
                Text(
                    text = combinedErrorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    val phoneText = profile?.phone?.ifBlank { "Телефон не указан" } ?: "Телефон не указан"
                    val addressText = profile?.lastAddress?.ifBlank { "Адрес не указан" } ?: "Адрес не указан"
                    MenuRow(
                        icon = Icons.Default.Phone,
                        title = phoneText,
                        titleContent = if (profile == null && isLoading) {
                            {
                                SkeletonLine(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .width(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                )
                            }
                        } else {
                            null
                        }
                    )
                    Divider()
                    MenuRow(
                        icon = Icons.Default.LocationOn,
                        title = addressText,
                        titleContent = if (profile == null && isLoading) {
                            {
                                SkeletonLine(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .width(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                )
                            }
                        } else {
                            null
                        },
                        onClick = { addressExpanded = !addressExpanded }
                    )
                    AnimatedVisibility(
                        visible = addressExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val addresses = profile?.addresses.orEmpty()
                            if (addresses.isEmpty()) {
                                Text(
                                    text = "Адресов пока нет",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                addresses.forEach { address ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = address,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Удалить",
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .clickable {
                                                    deleteUserAddress(
                                                        address = address,
                                                        onDone = { updated, last ->
                                                            profile = (profile ?: UserProfile()).copy(
                                                                addresses = updated,
                                                                lastAddress = last
                                                            )
                                                        },
                                                        onError = { errorText = it }
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = newAddressInput,
                                onValueChange = { newAddressInput = it },
                                label = { Text("Новый адрес") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(
                                onClick = {
                                    if (newAddressInput.isBlank()) {
                                        errorText = "Введите адрес"
                                        return@TextButton
                                    }
                                    addUserAddress(
                                        address = newAddressInput,
                                        onDone = {
                                            val updated = profile?.addresses.orEmpty().toMutableList()
                                            val trimmed = newAddressInput.trim()
                                            if (trimmed.isNotBlank() && !updated.contains(trimmed)) {
                                                updated.add(trimmed)
                                            }
                                            profile = (profile ?: UserProfile()).copy(
                                                addresses = updated,
                                                lastAddress = trimmed
                                            )
                                            newAddressInput = ""
                                            errorText = null
                                        },
                                        onError = { errorText = it }
                                    )
                                }
                            ) {
                                Text("Добавить адрес")
                            }
                        }
                    }
                    Divider()
                    val rotation by animateFloatAsState(
                        targetValue = if (ordersExpanded) 180f else 0f,
                        label = "orders-rotation"
                    )
                    MenuRow(
                        icon = Icons.Default.ListAlt,
                        title = "Мои заказы",
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.rotate(rotation)
                            )
                        },
                        onClick = { ordersExpanded = !ordersExpanded }
                    )
                        AnimatedVisibility(
                            visible = ordersExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isOrdersLoading) {
                                    Text(
                                        text = "Загрузка заказов...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else if (orders.isEmpty()) {
                                    Text(
                                        text = "Пока нет заказов",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    orders.forEach { order ->
                                        val isExpanded = expandedOrderIds.contains(order.orderId)
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateContentSize(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expandedOrderIds = if (isExpanded) {
                                                            expandedOrderIds - order.orderId
                                                        } else {
                                                            expandedOrderIds + order.orderId
                                                        }
                                                    }
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val dateText = if (order.createdAt > 0L) {
                                                    dateFormat.format(order.createdAt)
                                                } else {
                                                    "Дата неизвестна"
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "Заказ #${order.orderId.takeLast(6)}",
                                                            style = MaterialTheme.typography.titleSmall
                                                        )
                                                        Text(
                                                            text = dateText,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    Text(
                                                        text = "${order.total.toInt()} ₽",
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                }
                                                Text(
                                                    text = "Товаров: ${order.itemsCount}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                val statusLabel = when (order.status.uppercase(Locale.getDefault())) {
                                                    "RECEIVED" -> "Получен"
                                                    "ACCEPTED" -> "Принят / собирается"
                                                    "IN_TRANSIT" -> "В пути"
                                                    "DONE" -> "Завершён"
                                                    "CANCELLED" -> "Отменён"
                                                    else -> "Получен"
                                                }
                                                Text(
                                                    text = "Статус: $statusLabel",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                if (order.channel.isNotBlank()) {
                                                    Text(
                                                        text = "Канал: ${order.channel}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                AnimatedVisibility(
                                                    visible = isExpanded,
                                                    enter = expandVertically(),
                                                    exit = shrinkVertically()
                                                ) {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Divider()
                                                        Card(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                            )
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(12.dp),
                                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                order.items.forEach { item ->
                                                                    val product = products.find { it.id == item.id }
                                                                    val unitLabel = when (item.unit.uppercase(Locale.getDefault())) {
                                                                        "KG" -> "кг"
                                                                        "PIECE" -> "шт"
                                                                        else -> item.unit
                                                                    }
                                                                    Text(
                                                                        text = "${item.name} — ${item.quantity} $unitLabel",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        modifier = Modifier.clickable(enabled = product != null) {
                                                                            if (product == null) {
                                                                                Toast.makeText(
                                                                                    context,
                                                                                    "Товар недоступен",
                                                                                    Toast.LENGTH_SHORT
                                                                                ).show()
                                                                            } else {
                                                                                onOpenProduct(product.id)
                                                                            }
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        TextButton(onClick = {
                                                            if (cartItems.isNotEmpty()) {
                                                                pendingOrder = order
                                                                showReplaceDialog = true
                                                            } else {
                                                                addOrderToCart(order, products, onAddToCart, context)
                                                            }
                                                        }) {
                                                            Text("Повторить заказ")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    Divider()
                    MenuRow(
                        icon = Icons.Default.ExitToApp,
                        title = "Выйти",
                        onClick = { FirebaseAuth.getInstance().signOut() }
                    )
                }
            }
        }
    }


    if (showReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("Корзина не пуста") },
            text = { Text("Очистить корзину и добавить товары из заказа?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val order = pendingOrder
                        showReplaceDialog = false
                        pendingOrder = null
                        if (order != null) {
                            onClearCart()
                            addOrderToCart(order, products, onAddToCart, context)
                        }
                    }
                ) {
                    Text("Да, заменить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) {
                    Text("Нет")
                }
            }
        )
    }
}

private fun addOrderToCart(
    order: OrderSummary,
    products: List<Product>,
    onAddToCart: (Product, Double) -> Unit,
    context: android.content.Context
) {
    val missing = mutableListOf<String>()
    order.items.forEach { item ->
        val product = products.find { it.id == item.id }
        if (product == null || !product.inStock) {
            missing.add(item.name)
        } else {
            onAddToCart(product, item.quantity)
        }
    }
    if (missing.isNotEmpty()) {
        Toast.makeText(
            context,
            "Нет в наличии: ${missing.joinToString()}",
            Toast.LENGTH_LONG
        ).show()
    } else {
        Toast.makeText(
            context,
            "Заказ добавлен в корзину",
            Toast.LENGTH_SHORT
        ).show()
    }

}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    titleContent: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            if (titleContent != null) {
                titleContent()
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun SkeletonLine(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier = modifier.background(color)
    )
}