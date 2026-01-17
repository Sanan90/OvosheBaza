package com.example.ovoshebaza.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ovoshebaza.Product
import com.example.ovoshebaza.ProductCategory
import com.example.ovoshebaza.UnitType
import com.example.ovoshebaza.sendBroadcastNotification
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun ProductEditDialog(
    initialProduct: Product,
    isNew: Boolean = false,
    onConfirm: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct.name) }
    var priceText by remember { mutableStateOf(initialProduct.price.toString()) }
    var originCountry by remember { mutableStateOf(initialProduct.originCountry ?: "") }
    var imageUrl by remember { mutableStateOf(initialProduct.imageUrl ?: "") }
    var description by remember { mutableStateOf(initialProduct.description ?: "") }
    var isPopular by remember { mutableStateOf(initialProduct.isPopular) }
    var isNewFlag by remember { mutableStateOf(initialProduct.isNew) }
    var inStock by remember { mutableStateOf(initialProduct.inStock) }
    var category by remember { mutableStateOf(initialProduct.category) }
    var unit by remember { mutableStateOf(initialProduct.unit) }

    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(if (isNew) "Новый товар" else "Редактирование товара")
        },
        text = {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)     // чтобы диалог не был бесконечным
                    .verticalScroll(scrollState)
                    .padding(end = 6.dp)       // чтобы полоса/скролл не наезжал на текст
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Цена") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Единица измерения
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Единица: ")
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = unit == UnitType.KG,
                        onClick = { unit = UnitType.KG },
                        label = { Text("кг") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = unit == UnitType.PIECE,
                        onClick = { unit = UnitType.PIECE },
                        label = { Text("шт") }
                    )
                }

                // Категория (простыми кнопками)
                Column {
                    Text("Категория:")
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(
                            ProductCategory.VEGETABLES,
                            ProductCategory.FRUITS,
                            ProductCategory.BERRIES,
                            ProductCategory.GREENS,
                            ProductCategory.NUTS,
                            ProductCategory.OTHER
                        )) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = {
                                    Text(
                                        when (cat) {
                                            ProductCategory.VEGETABLES -> "Овощи"
                                            ProductCategory.FRUITS -> "Фрукты"
                                            ProductCategory.BERRIES -> "Ягоды"
                                            ProductCategory.GREENS -> "Зелень"
                                            ProductCategory.NUTS -> "Орехи/сухофрукты"
                                            ProductCategory.OTHER -> "Другое"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = originCountry,
                    onValueChange = { originCountry = it },
                    label = { Text("Страна происхождения (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL картинки (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание товара") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 4
                )


                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPopular,
                        onCheckedChange = { isPopular = it }
                    )
                    Text("Популярный товар")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isNewFlag,
                        onCheckedChange = { isNewFlag = it }
                    )
                    Text("Новинка")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = inStock,
                        onCheckedChange = { inStock = it }
                    )
                    Text("В наличии")
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val price = priceText.replace(",", ".").toDoubleOrNull()
                    if (name.isBlank()) {
                        errorText = "Введите название"
                    } else if (price == null || price <= 0.0) {
                        errorText = "Введите корректную цену"
                    } else {
                        errorText = null
                        onConfirm(
                            initialProduct.copy(
                                name = name,
                                price = price,
                                originCountry = originCountry.ifBlank { null },
                                imageUrl = imageUrl.ifBlank { null },
                                description = description.ifBlank { null },
                                isPopular = isPopular,
                                isNew = isNewFlag,
                                inStock = inStock,
                                category = category,
                                unit = unit
                            )
                        )
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

enum class AdminTab { PRODUCTS, USERS }
// 4. Админ-экран (пока пустой)
@Composable
fun AdminScreen(
    products: List<Product>,
    onUpdateProduct: (Product) -> Unit,
    onAddProduct: (Product) -> Unit
) {


    var showEditDialog by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(AdminTab.PRODUCTS) }
    var isLoadingUsers by remember { mutableStateOf(false) }
    var usersError by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<AdminUserSummary>>(emptyList()) }
    var broadcastMessage by remember { mutableStateOf("") }
    var broadcastError by remember { mutableStateOf<String?>(null) }
    var broadcastSuccess by remember { mutableStateOf<String?>(null) }
    var isBroadcastSending by remember { mutableStateOf(false) }
    val expandedUsers = remember { mutableStateMapOf<String, Boolean>() }
    val userOrders = remember { mutableStateMapOf<String, List<AdminOrderSummary>>() }
    val loadingOrders = remember { mutableStateMapOf<String, Boolean>() }
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != AdminTab.USERS) return@LaunchedEffect
        isLoadingUsers = true
        usersError = null
        FirebaseFirestore.getInstance()
            .collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                users = snapshot.documents.map { doc ->
                    val addresses = (doc.get("addresses") as? List<*>)?.mapNotNull { it?.toString() }
                        ?: emptyList()
                    AdminUserSummary(
                        uid = doc.id,
                        name = (doc.getString("name") ?: "").trim(),
                        phone = (doc.getString("phone") ?: "").trim(),
                        lastAddress = (doc.getString("lastAddress") ?: "").trim(),
                        addresses = addresses
                    )
                }.sortedBy { it.name.ifBlank { it.phone } }
                isLoadingUsers = false
            }
            .addOnFailureListener { e ->
                usersError = e.message ?: "Не удалось загрузить пользователей"
                isLoadingUsers = false
            }
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AdminBottomTab(
                            label = "Пользователи",
                            icon = Icons.Default.People,
                            selected = selectedTab == AdminTab.USERS,
                            onClick = { selectedTab = AdminTab.USERS }
                        )

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .offset(y = (-8).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingActionButton(
                                onClick = { showAddDialog = true },
                                containerColor = Color(0xFF2ECC71),
                                contentColor = Color.White
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Добавить товар"
                                )
                            }
                        }

                        AdminBottomTab(
                            label = "Товары",
                            icon = Icons.Default.Store,
                            selected = selectedTab == AdminTab.PRODUCTS,
                            onClick = { selectedTab = AdminTab.PRODUCTS }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "Админ-панель",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Уведомления пользователям",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(
                                onClick = {
                                    broadcastError = null
                                    broadcastSuccess = null
                                    isBroadcastSending = true
                                    sendBroadcastNotification(
                                        message = "У нас свежий завоз!!!",
                                        onSuccess = {
                                            isBroadcastSending = false
                                            broadcastSuccess = "Уведомление отправлено"
                                        },
                                        onError = { msg ->
                                            isBroadcastSending = false
                                            broadcastError = msg
                                        }
                                    )
                                },
                                enabled = !isBroadcastSending,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Получили новый завоз")
                            }
                            OutlinedTextField(
                                value = broadcastMessage,
                                onValueChange = { broadcastMessage = it },
                                label = { Text("Свое сообщение") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val trimmed = broadcastMessage.trim()
                                    if (trimmed.isBlank()) {
                                        broadcastError = "Введите сообщение"
                                        return@Button
                                    }
                                    broadcastError = null
                                    broadcastSuccess = null
                                    isBroadcastSending = true
                                    sendBroadcastNotification(
                                        message = trimmed,
                                        onSuccess = {
                                            isBroadcastSending = false
                                            broadcastSuccess = "Уведомление отправлено"
                                            broadcastMessage = ""
                                        },
                                        onError = { msg ->
                                            isBroadcastSending = false
                                            broadcastError = msg
                                        }
                                    )
                                },
                                enabled = !isBroadcastSending,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Отправить")
                            }
                            if (broadcastSuccess != null) {
                                Text(
                                    text = broadcastSuccess ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (broadcastError != null) {
                                Text(
                                    text = broadcastError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }


                if (selectedTab == AdminTab.PRODUCTS) {
                    item {
                        Text(
                            text = "Здесь можно изменить товары, цены, единицы, популярность и наличие.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Поиск по названию") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    items(filteredProducts.size) { index ->
                        val product = filteredProducts[index]
                        AdminProductRow(
                            product = product,
                            onEditClick = { showEditDialog = product },
                            onQuickPriceChange = { updated ->
                                onUpdateProduct(updated)
                            }
                        )
                    }
                } else {
                    item {
                        if (isLoadingUsers) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                Text("Загружаем пользователей…")
                            }
                        }
                        if (usersError != null) {
                            Text(
                                text = usersError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    items(users) { user ->
                        val isExpanded = expandedUsers[user.uid] == true
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .clickable {
                                    val next = !isExpanded
                                    expandedUsers[user.uid] = next
                                    if (next && !userOrders.containsKey(user.uid)) {
                                        loadingOrders[user.uid] = true
                                        FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(user.uid)
                                            .collection("orders")
                                            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                            .get()
                                            .addOnSuccessListener { snapshot ->
                                                val orders = snapshot.documents.map { doc ->
                                                    val items = doc.get("items") as? List<*>
                                                    AdminOrderSummary(
                                                        orderId = doc.id,
                                                        createdAt = doc.getLong("createdAt") ?: 0L,
                                                        total = doc.getDouble("total") ?: 0.0,
                                                        itemsCount = items?.size ?: 0
                                                    )
                                                }
                                                userOrders[user.uid] = orders
                                                loadingOrders[user.uid] = false
                                            }
                                            .addOnFailureListener {
                                                loadingOrders[user.uid] = false
                                            }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val title = listOfNotNull(
                                    user.name.takeIf { it.isNotBlank() },
                                    user.phone.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                Text(
                                    text = if (title.isNotBlank()) title else "Без имени",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (user.lastAddress.isNotBlank()) {
                                    Text(
                                        text = user.lastAddress,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Divider()
                                        if (user.addresses.isNotEmpty()) {
                                            Text(
                                                text = "Адреса:",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            user.addresses.forEach { address ->
                                                Text(
                                                    text = "• $address",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        val orders = userOrders[user.uid] ?: emptyList()
                                        if (loadingOrders[user.uid] == true) {
                                            Text(
                                                text = "Загружаем заказы…",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        } else if (orders.isEmpty()) {
                                            Text(
                                                text = "Заказов нет",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        } else {
                                            Text(
                                                text = "Заказы:",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            orders.forEach { order ->
                                                Text(
                                                    text = "• #${order.orderId.takeLast(6)} · ${order.total.toInt()} ₽ · ${order.itemsCount} поз.",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val productToEdit = showEditDialog
    if (productToEdit != null) {
        ProductEditDialog(
            initialProduct = productToEdit,
            onConfirm = { updated ->
                onUpdateProduct(updated)
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null }
        )
    }

    if (showAddDialog) {
        ProductEditDialog(
            initialProduct = Product(
                id = "product_${System.currentTimeMillis()}",
                name = "",
                category = ProductCategory.VEGETABLES,
                price = 0.0,
                unit = UnitType.KG,
                imageUrl = null,
                originCountry = null,
                description = null,
                isPopular = false,
                isNew = false,
                inStock = true
            ),
            isNew = true,
            onConfirm = { newProduct ->
                onAddProduct(newProduct)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }


    if (isBroadcastSending) {
        AlertDialog(
            onDismissRequest = {},
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text("Отправляем уведомление…")
                }
            },
            confirmButton = {}
        )
    }
}



@Composable
private fun AdminBottomTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .widthIn(min = 88.dp)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = tint
        )
    }
}


data class AdminUserSummary(
    val uid: String,
    val name: String,
    val phone: String,
    val lastAddress: String,
    val addresses: List<String>
)

data class AdminOrderSummary(
    val orderId: String,
    val createdAt: Long,
    val total: Double,
    val itemsCount: Int
)



@Composable
fun AdminProductRow(
    product: Product,
    onEditClick: () -> Unit,
    onQuickPriceChange: (Product) -> Unit
) {
    var showPriceDialog by remember { mutableStateOf(false) }

    val unitLabel = when (product.unit) {
        UnitType.KG -> "кг"
        UnitType.PIECE -> "шт"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ✅ Цена кликабельна -> быстрый ввод
            Text(
                text = "${product.price.toInt()} ₽ / $unitLabel",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { showPriceDialog = true }
            )
        }
    }

    if (showPriceDialog) {
        QuickPriceDialog(
            currentPrice = product.price,
            onConfirm = { newPrice ->
                onQuickPriceChange(product.copy(price = newPrice))
                showPriceDialog = false
            },
            onDismiss = { showPriceDialog = false }
        )
    }
}


@Composable
fun QuickPriceDialog(
    currentPrice: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf(currentPrice.toInt().toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Быстрая смена цены") },
        text = {
            Column {
                Text("Введите новую цену (в рублях):")

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Цена") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newPrice = priceText.replace(",", ".").toDoubleOrNull()
                    if (newPrice == null || newPrice <= 0.0) {
                        errorText = "Введите корректную цену"
                    } else {
                        errorText = null
                        onConfirm(newPrice)
                    }
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Отмена")
            }
        }
    )
}

