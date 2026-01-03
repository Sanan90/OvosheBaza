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
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import kotlinx.coroutines.launch

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.interaction.MutableInteractionSource

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.functions.ktx.functions
import kotlin.math.round

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import com.example.ovoshebaza.ui.theme.VeggieTheme



// Главная Activity — точка входа в приложение
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContent — запускаем Compose UI
        setContent {
            // Можно потом сделать свою тему, пока используем Material3 по умолчанию
            VeggieTheme {
                VeggieShopApp()
            }
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val noRippleInteraction = remember { MutableInteractionSource() }


                                Column(
                                    modifier = Modifier.clickable(
                                        interactionSource = noRippleInteraction,
                                        indication = null
                                    ) {

                                        logoClickCount++

                                        if (logoClickCount >= 7) {
                                            logoClickCount = 0
                                            // показываем диалог ввода PIN
                                            showAdminPinDialog = true
                                            adminPin = ""
                                            adminPinError = null
                                        }
                                    }
                            ) {
                                Text(
                                    text = "🍎 Овощная база",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "свежие продукты каждый день",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
        },
        bottomBar = {

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
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
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
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





@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatalogScreen(
    products: List<Product>,
    onAddToCart: (Product, Double) -> Unit,
    onOpenDetails: (Product) -> Unit
) {
    // Фильтр: All / Category / Popular (как у тебя уже сделано)
    var selectedFilter by remember { mutableStateOf<CatalogFilter>(CatalogFilter.All) }
    var searchQuery by remember { mutableStateOf("") }

    // Состояние грида (чтобы при нажатии "Все популярные" прокрутить вверх)
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // --- Данные ---
    val inStockProducts = remember(products) { products.filter { it.inStock } }

    val popularProducts = remember(inStockProducts) {
        inStockProducts.filter { it.isPopular }
    }

    // 5–6 популярных для верхней ленты
    val popularPreview = remember(popularProducts) { popularProducts.take(6) }

    // Категории: сначала "Все", потом остальные
    val categories = remember(inStockProducts) {
        inStockProducts
            .mapNotNull { it.category }
            .distinct()
            .sorted()
    }

    // Фильтрация каталога
    val filteredProducts = remember(selectedFilter, searchQuery, inStockProducts) {
        val base = when (selectedFilter) {
            is CatalogFilter.Popular -> inStockProducts.filter { it.isPopular }
            is CatalogFilter.Category -> {
                val cat = (selectedFilter as CatalogFilter.Category).category
                inStockProducts.filter { it.category == cat }
            }
            is CatalogFilter.All -> inStockProducts
        }

        if (searchQuery.isBlank()) base
        else base.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // ---------- 1) Популярные сверху (лента) ----------
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (popularPreview.isNotEmpty()) {
                PopularRow(
                    items = popularPreview,
                    onOpenDetails = onOpenDetails,
                    onOpenAllPopular = {
                        selectedFilter = CatalogFilter.Popular
                        // прокрутим к началу каталога (чтобы видеть результаты)
                        scope.launch {
                            gridState.animateScrollToItem(2) // примерно туда, где начинается сетка
                        }
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // ---------- 2) Поиск ----------
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск по названию") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ---------- 3) Категории: сначала Все, потом остальные ----------
        // (Если хочешь “липкую” строку категорий — скажи, включим stickyHeader)
        item(span = { GridItemSpan(maxLineSpan) }) {
            CategoryChipsRow(
                categories = categories,
                selectedFilter = selectedFilter,
                onSelectAll = {
                    selectedFilter = CatalogFilter.All
                    scope.launch { gridState.animateScrollToItem(2) }
                },
                onSelectPopular = {
                    selectedFilter = CatalogFilter.Popular
                    scope.launch { gridState.animateScrollToItem(2) }
                },
                onSelectCategory = { cat ->
                    selectedFilter = CatalogFilter.Category(cat)
                    scope.launch { gridState.animateScrollToItem(2) }
                }
            )
        }

        // ---------- 4) Сетка товаров ----------
        items(filteredProducts, key = { it.id }) { product ->
            ProductCardLarge(
                product = product,
                onAddToCart = onAddToCart,
                onOpenDetails = { onOpenDetails(product) }
            )
        }

        // низ отступ
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}




@Composable
fun PopularRow(
    items: List<Product>,
    onOpenDetails: (Product) -> Unit,
    onOpenAllPopular: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Популярные", style = MaterialTheme.typography.titleMedium)

            TextButton(onClick = onOpenAllPopular) {
                Text("Все популярные")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 12.dp)
        ) {
            items(items, key = { it.id }) { p ->
                PopularMiniCard(
                    product = p,
                    onClick = { onOpenDetails(p) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}



@Composable
fun PopularMiniCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // --- Фото ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                val url = product.imageUrl

                if (!url.isNullOrBlank()) {
                    AsyncImage(
                        model = url,
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

            // --- Текст ---
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = product.name,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                val unitText = if (product.unit == UnitType.KG) "кг" else "шт"
                Text(
                    text = "${product.price.toInt()} ₽ / $unitText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



@Composable
fun CategoryChipsRow(
    categories: List<ProductCategory>,
    selectedFilter: CatalogFilter,
    onSelectAll: () -> Unit,
    onSelectPopular: () -> Unit,
    onSelectCategory: (ProductCategory) -> Unit
)
 {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFilter is CatalogFilter.All,
                onClick = onSelectAll,
                label = { Text("Все") }
            )
        }

        item {
            FilterChip(
                selected = selectedFilter is CatalogFilter.Popular,
                onClick = onSelectPopular,
                label = { Text("Популярные") }
            )
        }

        items(categories) { cat ->
            FilterChip(
                selected = selectedFilter is CatalogFilter.Category &&
                        (selectedFilter as CatalogFilter.Category).category == cat,
                onClick = { onSelectCategory(cat) },
                label = {
                    Text(
                        when (cat) {
                            ProductCategory.VEGETABLES -> "Овощи"
                            ProductCategory.FRUITS -> "Фрукты"
                            ProductCategory.BERRIES -> "Ягоды"
                            ProductCategory.GREENS -> "Зелень"
                            ProductCategory.NUTS -> "Орехи / сухофрукты"
                            ProductCategory.OTHER -> "Другое"
                        }
                    )
                }
            )
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
    // --- helpers ---
    fun kgToGrams(kg: Double): Int = kotlin.math.round(kg * 1000.0).toInt()
    fun gramsToKg(grams: Int): Double = grams / 1000.0

    fun formatButtonValue(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    val unitLabel = if (unit == UnitType.KG) "кг" else "шт"

    val options: List<Double> =
        if (unit == UnitType.KG) listOf(0.1, 0.5, 1.0, 5.0, 10.0)
        else listOf(1.0, 2.0, 3.0, 5.0, 10.0)

    // Внутреннее хранение:
    // KG -> граммы (Int)
    // PIECE -> штуки (Int)
    var tempGrams by remember(unit) {
        mutableStateOf(if (unit == UnitType.KG) kgToGrams(initialQuantity.coerceAtLeast(0.0)) else 0)
    }
    var tempPieces by remember(unit) {
        mutableStateOf(if (unit == UnitType.KG) 0 else initialQuantity.coerceAtLeast(0.0).toInt())
    }

    val tempQuantity: Double =
        if (unit == UnitType.KG) gramsToKg(tempGrams) else tempPieces.toDouble()

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Выбор количества") },
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rows = options.chunked(3)

                    rows.forEach { rowOptions ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowOptions.forEach { value ->
                                Button(
                                    onClick = {
                                        if (unit == UnitType.KG) tempGrams += kgToGrams(value)
                                        else tempPieces += value.toInt()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    // ✅ без пробела, чтобы не переносилось: 0.5кг / 10шт
                                    Text(
                                        text = "${formatButtonValue(value)}$unitLabel",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Добиваем пустыми, чтобы кнопки не "плясали"
                            if (rowOptions.size < 3) {
                                repeat(3 - rowOptions.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        if (unit == UnitType.KG) tempGrams = 0 else tempPieces = 0
                    }
                ) {
                    Text("Обнулить")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempQuantity.coerceAtLeast(0.0)) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
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
            .clickable { onOpenDetails() }, // ✅ клик по карточке → детали
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // ✅ сделал фото покрупнее
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
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
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // ✅ чтобы клик по корзине НЕ открывал детали:
                FilledTonalIconButton(
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
                Text(
                    country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                Row {
                    TextButton(
                        onClick = {
                            // проверка полей (как у тебя)
                            when {
                                customerName.isBlank() -> errorText = "Пожалуйста, укажите имя."
                                customerPhone.isBlank() -> errorText = "Пожалуйста, укажите телефон."
                                customerAddress.isBlank() -> errorText = "Пожалуйста, укажите адрес доставки."
                                else -> {
                                    errorText = null

                                    val message = buildOrderMessage(
                                        cartItems = cartItems,
                                        customerName = customerName,
                                        customerPhone = customerPhone,
                                        customerAddress = customerAddress,
                                        comment = customerComment
                                    )

                                    val order = buildOrderMap(
                                        cartItems = cartItems,
                                        customerName = customerName,
                                        customerPhone = customerPhone,
                                        customerAddress = customerAddress,
                                        comment = customerComment
                                    )

                                    sendOrderViaFirebaseTelegram(
                                        context = context,
                                        order = order,
                                        onSuccess = {
                                            Toast.makeText(context, "Заказ отправлен в Telegram ✅", Toast.LENGTH_LONG).show()
                                            showOrderDialog = false

                                            // (по желанию) очистка полей после успешной отправки:
                                            customerName = ""
                                            customerPhone = ""
                                            customerAddress = ""
                                            customerComment = ""
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, "Ошибка отправки: $err", Toast.LENGTH_LONG).show()
                                        }
                                    )

                                }
                            }
                        }
                    ) {
                        Text("Telegram")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            when {
                                customerName.isBlank() -> errorText = "Пожалуйста, укажите имя."
                                customerPhone.isBlank() -> errorText = "Пожалуйста, укажите телефон."
                                customerAddress.isBlank() -> errorText = "Пожалуйста, укажите адрес доставки."
                                else -> {
                                    errorText = null

                                    val message = buildOrderMessage(
                                        cartItems = cartItems,
                                        customerName = customerName,
                                        customerPhone = customerPhone,
                                        customerAddress = customerAddress,
                                        comment = customerComment
                                    )

                                    // WhatsApp на твой номер
                                    sendOrderViaWhatsApp(context, message, "+79687008070")

                                    showOrderDialog = false
                                }
                            }
                        }
                    ) {
                        Text("WhatsApp")
                    }
                }
            }
            ,
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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products.size) { index ->
                val product = products[index]
                AdminProductRow(
                    product = product,
                    onEditClick = { showEditDialog = product },
                    onQuickPriceChange = { updated ->
                        onUpdateProduct(updated)
                    }
                )
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
                isPopular = false,
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
}



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

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Левая часть (вся инфа)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ✅ Цена кликабельна -> быстрый ввод
                Text(
                    text = "Цена: ${product.price.toInt()} ₽ / $unitLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { showPriceDialog = true }
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
                    text = "Категория: " + when (product.category) {
                        ProductCategory.VEGETABLES -> "Овощи"
                        ProductCategory.FRUITS -> "Фрукты"
                        ProductCategory.BERRIES -> "Ягоды"
                        ProductCategory.GREENS -> "Зелень"
                        ProductCategory.NUTS -> "Орехи/сухофрукты"
                        ProductCategory.OTHER -> "Другое"
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Популярный: ${if (product.isPopular) "да" else "нет"}, в наличии: ${if (product.inStock) "да" else "нет"}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Нажмите на цену, чтобы быстро изменить.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Правая часть: кнопка "Изменить" напротив данных
            TextButton(onClick = onEditClick) {
                Text("Изменить")
            }
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


fun buildOrderMap(
    cartItems: List<CartItem>,
    customerName: String,
    customerPhone: String,
    customerAddress: String,
    comment: String
): Map<String, Any> {
    val items = cartItems.map { item ->
        mapOf(
            "id" to item.product.id,
            "name" to item.product.name,
            "quantity" to item.quantity,
            "unit" to item.product.unit.name,   // "KG" или "PIECE"
            "price" to item.product.price,
            "sum" to (item.product.price * item.quantity)
        )
    }

    val total = cartItems.sumOf { it.product.price * it.quantity }

    return mapOf(
        "type" to "ORDER",
        "createdAt" to System.currentTimeMillis(),
        "customerName" to customerName,
        "customerPhone" to customerPhone,
        "customerAddress" to customerAddress,
        "comment" to comment,
        "total" to total,
        "items" to items
    )
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


fun sendOrderViaWhatsApp(context: Context, message: String, phoneE164: String) {
    try {
        // WhatsApp использует номер без "+"
        val phone = phoneE164.replace("+", "").trim()

        val encodedText = Uri.encode(message)
        val uri = Uri.parse("https://wa.me/$phone?text=$encodedText")

        val intent = Intent(Intent.ACTION_VIEW, uri)

        // Попробуем открыть именно WhatsApp (если установлен)
        intent.setPackage("com.whatsapp")

        context.startActivity(intent)
    } catch (e: Exception) {
        // Если WhatsApp не открылся, пробуем через браузер (wa.me откроет WhatsApp если может)
        try {
            val phone = phoneE164.replace("+", "").trim()
            val encodedText = Uri.encode(message)
            val uri = Uri.parse("https://wa.me/$phone?text=$encodedText")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось открыть WhatsApp или браузер", Toast.LENGTH_SHORT).show()
        }
    }
}

fun sendOrderViaFirebaseTelegram(
    context: Context,
    order: Map<String, Any>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val functions = com.google.firebase.ktx.Firebase.functions

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


