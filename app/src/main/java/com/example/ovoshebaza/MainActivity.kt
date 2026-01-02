package com.example.ovoshebaza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Edit
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import com.google.firebase.firestore.DocumentSnapshot

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.runtime.*

import androidx.navigation.NavType
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.material.icons.filled.ArrowBack
import androidx.navigation.navArgument

import androidx.compose.material.icons.filled.ArrowBack
import coil.compose.AsyncImage


// Главная Activity — точка входа в приложение
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContent — запускаем Compose UI
        setContent {
            // Можно потом сделать свою тему, пока используем Material3 по умолчанию
            VeggieShopApp()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeggieShopApp() {
    val navController = rememberNavController()

    val shopViewModel: ShopViewModel = viewModel()
    val products = shopViewModel.products
    val cartItems = shopViewModel.cartItems

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: Screen.Catalog.route

    // 👉 стейты для логотипа и PIN-диалога
    var logoClickCount by remember { mutableStateOf(0) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var adminPin by remember { mutableStateOf("") }
    var adminPinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🍎 Мой овощной магазин",
                            modifier = Modifier
                                .clickable {
                                    logoClickCount++

                                    if (logoClickCount >= 7) {
                                        logoClickCount = 0
                                        // показываем диалог ввода PIN
                                        showAdminPinDialog = true
                                        adminPin = ""
                                        adminPinError = null
                                    }
                                }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(Screen.Catalog, Screen.Cart, Screen.Request).forEach { screen ->

                    val icon = when (screen) {
                        Screen.Catalog -> Icons.Default.Store
                        Screen.Cart -> Icons.Default.ShoppingCart
                        Screen.Request -> Icons.Default.NoteAdd
                        Screen.Admin -> Icons.Default.Settings
                        Screen.ProductDetails -> Icons.Default.Store // просто заглушка, в меню он не будет
                    }


                    NavigationBarItem(
                        selected = (currentRoute == screen.route),
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Catalog.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        label = { Text(screen.label) },
                        icon = {
                            if (screen == Screen.Cart) {
                                BadgedBox(
                                    badge = {
                                        val count = cartItems.size
                                        if (count > 0) {
                                            Badge {
                                                Text(
                                                    text = if (count > 99) "99+" else count.toString()
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.label
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            products = products,
            cartItems = cartItems,
            onAddToCart = { product, quantity ->
                shopViewModel.addToCart(product, quantity)
            },
            onUpdateQuantity = { productId, quantity ->
                shopViewModel.updateCartItemQuantity(productId, quantity)
            },
            onRemoveFromCart = { productId ->
                shopViewModel.removeFromCart(productId)
            },
            onUpdateProduct = { updated ->
                shopViewModel.updateProduct(updated)
            },
            onAddProduct = { newProduct ->
                shopViewModel.addProduct(newProduct)
            }
        )
    }

    // ----- Диалог ввода PIN для админки -----
    if (showAdminPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminPinDialog = false
                adminPin = ""
                adminPinError = null
            },
            title = { Text("Вход в админ-панель") },
            text = {
                Column {
                    OutlinedTextField(
                        value = adminPin,
                        onValueChange = { newText ->
                            val digitsOnly = newText.filter { it.isDigit() }
                            if (digitsOnly.length <= 4) {
                                adminPin = digitsOnly
                            }
                        },
                        label = { Text("PIN-код") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        )
                    )
                    if (adminPinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = adminPinError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (adminPin == "2009") {
                            showAdminPinDialog = false
                            adminPin = ""
                            adminPinError = null
                            navController.navigate(Screen.Admin.route)
                        } else {
                            adminPinError = "Неверный код"
                        }
                    }
                ) {
                    Text("Войти")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdminPinDialog = false
                        adminPin = ""
                        adminPinError = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}




@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    products: List<Product>,
    cartItems: List<CartItem>,
    onAddToCart: (Product, Double) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onUpdateProduct: (Product) -> Unit,
    onAddProduct: (Product) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Catalog.route,
        modifier = modifier
    ) {
        composable(Screen.Catalog.route) {
            CatalogScreen(
                products = products,
                onAddToCart = onAddToCart,
                onOpenDetails = { product ->
                    navController.navigate("product/${product.id}")
                }
            )
        }

        composable(Screen.Cart.route) {
            CartScreen(
                cartItems = cartItems,
                onUpdateQuantity = onUpdateQuantity,
                onRemoveFromCart = onRemoveFromCart
            )
        }

        composable(Screen.Request.route) {
            RequestProductScreen()
        }

        composable(Screen.Admin.route) {
            AdminScreen(
                products = products,
                onUpdateProduct = onUpdateProduct,
                onAddProduct = onAddProduct
            )
        }

        // ✅ Экран деталей товара
        composable(
            route = "product/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            val product = products.find { it.id == productId }

            if (product != null) {
                ProductDetailsScreen(
                    product = product,
                    onBack = { navController.popBackStack() },
                    onAddToCart = onAddToCart
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
    }
}





// ============== ЭКРАНЫ ==============



// Какой фильтр сейчас выбран в каталоге
sealed class CatalogFilter {
    // Показываем только популярные товары
    object Popular : CatalogFilter()

    // Показываем товары конкретной категории (овощи, фрукты и т.д.)
    data class Category(val category: ProductCategory) : CatalogFilter()

    // Показываем все товары
    object All : CatalogFilter()
}





@Composable
fun CatalogScreen(
    products: List<Product>,
    onAddToCart: (Product, Double) -> Unit,
    onOpenDetails: (Product) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<CatalogFilter>(CatalogFilter.Popular) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(selectedFilter, searchQuery, products) {
        val baseList = when (selectedFilter) {
            is CatalogFilter.Popular -> products.filter { it.inStock && it.isPopular }
            is CatalogFilter.Category -> {
                val cat = (selectedFilter as CatalogFilter.Category).category
                products.filter { it.inStock && it.category == cat }
            }
            is CatalogFilter.All -> products.filter { it.inStock }
        }

        if (searchQuery.isBlank()) baseList
        else baseList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Каталог", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск по названию") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        CategoryFilterRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredProducts) { product ->
                ProductCardLarge(
                    product = product,
                    onAddToCart = onAddToCart,
                    onOpenDetails = { onOpenDetails(product) } // ✅ кликаем карточку → детали
                )
            }
        }
    }
}








// Ряд кнопок-фильтров: Популярные, Овощи, Фрукты, ... , Все
@Composable
fun CategoryFilterRow(
    selectedFilter: CatalogFilter,
    onFilterSelected: (CatalogFilter) -> Unit
) {
    val filters = listOf<CatalogFilter>(
        CatalogFilter.Popular,
        CatalogFilter.Category(ProductCategory.VEGETABLES),
        CatalogFilter.Category(ProductCategory.FRUITS),
        CatalogFilter.Category(ProductCategory.BERRIES),
        CatalogFilter.Category(ProductCategory.GREENS),
        CatalogFilter.Category(ProductCategory.NUTS),
        CatalogFilter.Category(ProductCategory.OTHER),
        CatalogFilter.All
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters.size) { index ->
            val filter = filters[index]

            val label = when (filter) {
                is CatalogFilter.Popular -> "Популярные"
                is CatalogFilter.All -> "Все"
                is CatalogFilter.Category -> when (filter.category) {
                    ProductCategory.VEGETABLES -> "Овощи"
                    ProductCategory.FRUITS -> "Фрукты"
                    ProductCategory.BERRIES -> "Ягоды"
                    ProductCategory.GREENS -> "Зелень"
                    ProductCategory.NUTS -> "Орехи/сухофрукты"
                    ProductCategory.OTHER -> "Другое"
                }
            }

            val isSelected = when {
                selectedFilter is CatalogFilter.Popular && filter is CatalogFilter.Popular -> true
                selectedFilter is CatalogFilter.All && filter is CatalogFilter.All -> true
                selectedFilter is CatalogFilter.Category && filter is CatalogFilter.Category &&
                        selectedFilter.category == filter.category -> true
                else -> false
            }

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(label) }
            )
        }
    }
}


@Composable
fun QuantityPickerDialog(
    unit: UnitType,
    initialQuantity: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var tempQuantity by remember { mutableStateOf(initialQuantity.coerceAtLeast(0.0)) }

    val unitLabel = if (unit == UnitType.KG) "кг" else "шт"

    val options: List<Double> =
        if (unit == UnitType.KG) listOf(0.1, 0.5, 1.0, 2.0, 5.0)
        else listOf(1.0, 2.0, 3.0, 5.0, 10.0)

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text("Выбор количества")
        },
        text = {
            Column {
                Text(
                    text = "Нажимайте на кнопки, чтобы добавить количество.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Единица измерения: $unitLabel",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Сейчас выбрано: ${formatQuantity(tempQuantity)} $unitLabel",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопки с вариантами
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chunked = options.chunked(3)
                    chunked.forEach { rowOptions ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowOptions.forEach { value ->
                                Button(
                                    onClick = {
                                        tempQuantity += value
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (value % 1.0 == 0.0) {
                                            value.toInt().toString()
                                        } else {
                                            value.toString()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { tempQuantity = 0.0 }
                ) {
                    Text("Обнулить")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(tempQuantity.coerceAtLeast(0.0))
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text("Отмена")
            }
        }
    )
}




@Composable
fun ProductCardLarge(
    product: Product,
    onAddToCart: (Product, Double) -> Unit,
    onOpenDetails: () -> Unit
) {
    var showQuantityDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .clickable { onOpenDetails() } // ✅ клик по карточке → детали
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // ✅ сделал фото покрупнее
            ) {
                if (product.imageUrl != null) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Фото", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buildString {
                        append(product.price.toInt())
                        append(" ")
                        append(if (product.unit == UnitType.KG) "кг" else "шт")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                // ✅ чтобы клик по корзине НЕ открывал детали:
                IconButton(
                    onClick = { showQuantityDialog = true },
                    modifier = Modifier
                        .padding(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Добавить в корзину"
                    )
                }
            }

            product.originCountry?.let { country ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(country, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showQuantityDialog) {
        QuantityPickerDialog(
            unit = product.unit,
            initialQuantity = 0.0,
            onConfirm = { quantity ->
                if (quantity > 0.0) onAddToCart(product, quantity)
                showQuantityDialog = false
            },
            onDismiss = { showQuantityDialog = false }
        )
    }
}








@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onUpdateQuantity: (String, Double) -> Unit,
    onRemoveFromCart: (String) -> Unit
) {
    val context = LocalContext.current

    // Считаем примерную сумму заказа
    val totalPrice = cartItems.sumOf { it.product.price * it.quantity }


    // Показывать ли диалог с формой оформления заказа
    var showOrderDialog by remember { mutableStateOf(false) }

    // Поля клиента (для диалога)
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var customerComment by remember { mutableStateOf("") }

    // Текст ошибки в диалоге
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Корзина",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = true)
            ) {
                items(cartItems.size) { index ->
                    val item = cartItems[index]
                    CartItemRow(
                        item = item,
                        onUpdateQuantity = onUpdateQuantity,
                        onRemoveFromCart = onRemoveFromCart
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Примерная сумма
            Text(
                text = "Ориентировочная сумма: ~ ${totalPrice.toInt()} ₽",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Фактическая сумма может немного отличаться из-за точного веса (+/− ~100 г).",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка "Оформить заказ" — только открывает диалог с формой
            Button(
                onClick = {
                    if (cartItems.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Корзина пуста.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        showOrderDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Оформить заказ")
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
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text("Адрес доставки") },
                        modifier = Modifier.fillMaxWidth()
                    )

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
                TextButton(
                    onClick = {
                        // Простая проверка полей
                        when {
                            customerName.isBlank() -> {
                                errorText = "Пожалуйста, укажите имя."
                            }
                            customerPhone.isBlank() -> {
                                errorText = "Пожалуйста, укажите телефон."
                            }
                            customerAddress.isBlank() -> {
                                errorText = "Пожалуйста, укажите адрес доставки."
                            }
                            else -> {
                                errorText = null

                                // Собираем текст заказа
                                val message = buildOrderMessage(
                                    cartItems = cartItems,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    customerAddress = customerAddress,
                                    comment = customerComment
                                )

                                // Отправляем через Telegram (или через шаринг, если Telegram не найден)
                                sendOrderViaTelegram(context, message)

                                // Закрываем диалог
                                showOrderDialog = false

                                // (по желанию позже можем очищать поля формы и корзину)
                            }
                        }
                    }
                ) {
                    Text("Отправить в Telegram")
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
}




@Composable
fun CartItemRow(
    item: CartItem,
    onUpdateQuantity: (String, Double) -> Unit,
    onRemoveFromCart: (String) -> Unit
) {
    // Показывать ли диалог редактирования количества
    var showDialog by remember { mutableStateOf(false) }

    val unitLabel = when (item.product.unit) {
        UnitType.KG -> "кг"
        UnitType.PIECE -> "шт"
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp) // было 12.dp
        ) {
            // ----- ВЕРХНЯЯ СТРОКА: КОЛИЧЕСТВО + ИКОНКА РЕДАКТИРОВАНИЯ -----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${formatQuantity(item.quantity)} $unitLabel",
                    style = MaterialTheme.typography.bodyMedium   // было titleMedium
                )

                IconButton(
                    onClick = { showDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Изменить количество"
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.product.name,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildString {
                    append(item.product.price.toInt())
                    append(" ₽ / ")
                    append(unitLabel)
                },
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(2.dp))

            val itemTotal = item.product.price * item.quantity
            Text(
                text = "Примерно: ~ ${itemTotal.toInt()} ₽",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { onRemoveFromCart(item.product.id) }
                ) {
                    Text("Удалить")
                }
            }
        }
    }

    // ----- ДИАЛОГ РЕДАКТИРОВАНИЯ КОЛИЧЕСТВА -----
    if (showDialog) {
        QuantityPickerDialog(
            unit = item.product.unit,
            initialQuantity = item.quantity,
            onConfirm = { newQuantity ->
                if (newQuantity <= 0.0) {
                    onRemoveFromCart(item.product.id)
                } else {
                    onUpdateQuantity(item.product.id, newQuantity)
                }
                showDialog = false
            },
            onDismiss = {
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    product: Product,
    onBack: () -> Unit,
    onAddToCart: (Product, Double) -> Unit
) {
    var showQtyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                if (product.imageUrl != null) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Фото товара")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(product.name, style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(8.dp))

            val unitText = if (product.unit == UnitType.KG) "кг" else "шт"
            Text(
                text = "Цена: ${product.price.toInt()} ₽ / $unitText",
                style = MaterialTheme.typography.titleMedium
            )

            product.originCountry?.takeIf { it.isNotBlank() }?.let { country ->
                Spacer(Modifier.height(8.dp))
                Text("Страна: $country", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { showQtyDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить в корзину")
            }
        }
    }

    if (showQtyDialog) {
        // ⚠️ Используем твой существующий диалог выбора количества
        QuantityPickerDialog(
            unit = product.unit,
            initialQuantity = 0.0,
            onConfirm = { qty ->
                if (qty > 0.0) onAddToCart(product, qty)
                showQtyDialog = false
            },
            onDismiss = { showQtyDialog = false }
        )
    }
}



// 3. Экран заявки на редкий товар (сейчас — заглушка)
@Composable
fun RequestProductScreen() {
    val context = LocalContext.current

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var requestedProduct by remember { mutableStateOf("") }
    var requestedQuantity by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

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
            singleLine = true
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

                        val message = buildRequestMessage(
                            customerName = customerName,
                            customerPhone = customerPhone,
                            requestedProduct = requestedProduct,
                            requestedQuantity = requestedQuantity.ifBlank { "Не указано" },
                            comment = comment
                        )

                        // Используем ту же функцию, что и для заказа
                        sendOrderViaTelegram(context, message)

                        Toast.makeText(
                            context,
                            "Заявка сформирована. Текст скопирован, откройте Telegram.",
                            Toast.LENGTH_LONG
                        ).show()

                        // По желанию — очищаем поля после отправки
                        requestedProduct = ""
                        requestedQuantity = ""
                        customerName = ""
                        customerPhone = ""
                        comment = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Отправить заявку в Telegram")
        }
    }
}


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
    var isPopular by remember { mutableStateOf(initialProduct.isPopular) }
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
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                isPopular = isPopular,
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


// 4. Админ-экран (пока пустой)
@Composable
fun AdminScreen(
    products: List<Product>,
    onUpdateProduct: (Product) -> Unit,
    onAddProduct: (Product) -> Unit
) {
    var showEditDialog by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Админ-панель",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Здесь можно изменить товары, цены, единицы, популярность и наличие.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Добавить новый товар")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Список товаров
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products.size) { index ->
                val product = products[index]
                AdminProductRow(
                    product = product,
                    onEditClick = { showEditDialog = product }
                )
            }
        }
    }

    // Диалог редактирования существующего товара
    val productToEdit = showEditDialog
    if (productToEdit != null) {
        ProductEditDialog(
            initialProduct = productToEdit,
            onConfirm = { updated ->
                onUpdateProduct(updated)
                showEditDialog = null
            },
            onDismiss = {
                showEditDialog = null
            }
        )
    }

    // Диалог добавления нового товара
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
                isPopular = false,
                inStock = true
            ),
            isNew = true,
            onConfirm = { newProduct ->
                onAddProduct(newProduct)
                showAddDialog = false
            },
            onDismiss = {
                showAddDialog = false
            }
        )
    }
}


@Composable
fun AdminProductRow(
    product: Product,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildString {
                    append(product.price.toInt())
                    append(" ")
                    append(
                        when (product.unit) {
                            UnitType.KG -> "кг"
                            UnitType.PIECE -> "шт"
                        }
                    )
                },
                style = MaterialTheme.typography.bodyMedium
            )

            product.originCountry?.let { country ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Страна: $country",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildString {
                    append("Категория: ")
                    append(
                        when (product.category) {
                            ProductCategory.VEGETABLES -> "Овощи"
                            ProductCategory.FRUITS -> "Фрукты"
                            ProductCategory.BERRIES -> "Ягоды"
                            ProductCategory.GREENS -> "Зелень"
                            ProductCategory.NUTS -> "Орехи/сухофрукты"
                            ProductCategory.OTHER -> "Другое"
                        }
                    )
                },
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Популярный: ${if (product.isPopular) "да" else "нет"}, в наличии: ${if (product.inStock) "да" else "нет"}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onEditClick) {
                    Text("Изменить")
                }
            }
        }
    }
}


// Красиво показываем количество:
// 1.0 -> "1", 1.5 -> "1.5"
fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

// Собираем текст заказа для отправки в WhatsApp
fun buildOrderMessage(
    cartItems: List<CartItem>,
    customerName: String,
    customerPhone: String,
    customerAddress: String,
    comment: String
): String {
    val sb = StringBuilder()

    sb.append("Заказ из приложения \"Мой овощной магазин\"\n\n")

    sb.append("Товары:\n")

    cartItems.forEachIndexed { index, item ->
        val lineNumber = index + 1
        val unitLabel = when (item.product.unit) {
            UnitType.KG -> "кг"
            UnitType.PIECE -> "шт"
        }

        sb.append("$lineNumber) ${item.product.name} — ${item.quantity} $unitLabel × ${item.product.price.toInt()} ₽\n")
    }

    val totalPrice = cartItems.sumOf { it.product.price * it.quantity }

    sb.append("\nОриентировочная сумма: ~ ${totalPrice.toInt()} ₽\n")
    sb.append("(Фактическая сумма может немного отличаться из-за точного веса товара)\n\n")

    sb.append("Данные клиента:\n")
    sb.append("Имя: $customerName\n")
    sb.append("Телефон: $customerPhone\n")
    sb.append("Адрес: $customerAddress\n")

    if (comment.isNotBlank()) {
        sb.append("Комментарий: $comment\n")
    }

    return sb.toString()
}




fun buildRequestMessage(
    customerName: String,
    customerPhone: String,
    requestedProduct: String,
    requestedQuantity: String,
    comment: String
): String {
    return buildString {
        appendLine("📝 НОВАЯ ЗАЯВКА НА ТОВАР")
        appendLine()
        appendLine("Имя: $customerName")
        appendLine("Телефон: $customerPhone")
        appendLine()
        appendLine("Что нужно заказать:")
        appendLine(requestedProduct)
        appendLine()
        appendLine("Желаемое количество:")
        appendLine(requestedQuantity)
        if (comment.isNotBlank()) {
            appendLine()
            appendLine("Комментарий:")
            appendLine(comment)
        }
    }
}


// Переводим Product -> Map для Firestore
fun Product.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "category" to category.name,
    "price" to price,
    "unit" to unit.name,
    "imageUrl" to imageUrl,
    "originCountry" to originCountry,
    "isPopular" to isPopular,
    "inStock" to inStock
)

// Переводим документ Firestore -> Product
fun DocumentSnapshot.toProduct(): Product? {
    val id = getString("id") ?: id              // если поля id нет, берём id документа
    val name = getString("name") ?: return null
    val categoryStr = getString("category") ?: ProductCategory.OTHER.name
    val unitStr = getString("unit") ?: UnitType.KG.name

    val price = getDouble("price") ?: 0.0
    val imageUrl = getString("imageUrl")
    val originCountry = getString("originCountry")
    val isPopular = getBoolean("isPopular") ?: false
    val inStock = getBoolean("inStock") ?: true

    val category = try {
        ProductCategory.valueOf(categoryStr)
    } catch (_: Exception) {
        ProductCategory.OTHER
    }

    val unit = try {
        UnitType.valueOf(unitStr)
    } catch (_: Exception) {
        UnitType.KG
    }

    return Product(
        id = id,
        name = name,
        category = category,
        price = price,
        unit = unit,
        imageUrl = imageUrl,
        originCountry = originCountry,
        isPopular = isPopular,
        inStock = inStock
    )
}





fun sendOrderViaTelegram(context: android.content.Context, message: String) {
    // 1. Кладём текст заказа в буфер обмена
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
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



