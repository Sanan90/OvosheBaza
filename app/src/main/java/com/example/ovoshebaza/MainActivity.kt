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
import androidx.compose.foundation.Image
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

import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.delay
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


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

@Composable
fun AppRoot() {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    if (user == null) {
        AuthScreen(
            onSignedIn = {
                ensureUserDocExists(
                    onDone = { /* ничего не нужно */ },
                    onError = { msg ->
                        android.widget.Toast
                            .makeText(context, msg, android.widget.Toast.LENGTH_LONG)
                            .show()
                    }
                )
            }
        )
    } else {
        VeggieShopApp()
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

    val context = LocalContext.current
    var showSupportDialog by remember { mutableStateOf(false) }
    var supportQuestion by remember { mutableStateOf("") }
    var supportPhone by remember { mutableStateOf("") }
    var supportError by remember { mutableStateOf<String?>(null) }
    var isSendingSupport by remember { mutableStateOf(false) }
    val helperIconRes = remember {
        listOf(
            R.drawable.helper,
            R.drawable.helper2,
            R.drawable.helper3,
            R.drawable.helper4
        ).random()
    }
    val hideSupportIcon =
        currentRoute == Screen.Cart.route ||
                currentRoute == Screen.Admin.route ||
                currentRoute == Screen.Request.route ||
                currentRoute.startsWith("product/")
    val supportIconOffsetX by animateDpAsState(
        targetValue = if (hideSupportIcon) 96.dp else 0.dp,
        animationSpec = tween(durationMillis = 550),
        label = "supportIconOffsetX"
    )
    val supportIconAlpha by animateFloatAsState(
        targetValue = if (hideSupportIcon) 0f else 1f,
        animationSpec = tween(durationMillis = 550),
        label = "supportIconAlpha"
    )
    val supportIconOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            repeat(3) {
                supportIconOffset.animateTo(1f, animationSpec = tween(durationMillis = 280))
                supportIconOffset.animateTo(0f, animationSpec = tween(durationMillis = 280))
                delay(120)
            }
            delay(15_000)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val topBarBrush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBrush)
            ) {
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "🍎 Овощная база",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                                Text(
                                    text = "свежие продукты каждый день",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        bottomBar = {
            if (currentRoute != Screen.Admin.route) {
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
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 0.dp
                    )
                    .size(75.dp)
                    .offset(x = supportIconOffsetX)
                    .graphicsLayer(alpha = supportIconAlpha)
                    .offset(y = lerp(0.dp, (-6).dp, supportIconOffset.value))
                    .clickable {
                        showSupportDialog = true
                        supportQuestion = ""
                        supportPhone = ""
                        supportError = null
                    },
                contentAlignment = Alignment.Center

            ) {
                Image(
                    painter = painterResource(id = helperIconRes),                    contentDescription = "Связь с поддержкой",
                    modifier = Modifier.size(125.dp)                )
            }
        }
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Связь с поддержкой") },
            text = {
                Column {
                    OutlinedTextField(
                        value = supportQuestion,
                        onValueChange = { supportQuestion = it },
                        label = { Text("Ваш вопрос") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = supportPhone,
                        onValueChange = { supportPhone = it },
                        label = { Text("Ваш номер телефона") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    if (supportError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = supportError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (supportQuestion.isBlank()) {
                            supportError = "Введите вопрос"
                            return@TextButton
                        }
                        if (supportPhone.isBlank()) {
                            supportError = "Введите номер телефона"
                            return@TextButton
                        }
                        supportError = null
                        isSendingSupport = true
                        val supportPayload = buildSupportMap(
                            question = supportQuestion,
                            phone = supportPhone
                        )
                        sendOrderViaFirebaseTelegram(
                            context = context,
                            order = supportPayload,
                            onSuccess = {
                                isSendingSupport = false
                                Toast.makeText(
                                    context,
                                    "Вопрос отправлен в поддержку ✅",
                                    Toast.LENGTH_LONG
                                ).show()
                                showSupportDialog = false
                            },
                            onError = { err ->
                                isSendingSupport = false
                                Toast.makeText(
                                    context,
                                    "Ошибка отправки: $err",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                ) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (isSendingSupport) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Отправка вопроса") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Отправляем сообщение, пожалуйста подождите…")
                }
            },
            confirmButton = {}
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


    // Показываем товары, которых нет в наличии
    object OutOfStock : CatalogFilter()
}

enum class PaymentMethod(val label: String) {
    CASH("Наличные при получении"),
    CARD("Картой при получении")
}




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatalogScreen(
    products: List<Product>,
    onAddToCart: (Product, Double) -> Unit,
    onOpenDetails: (Product) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<CatalogFilter>(CatalogFilter.All) }
    var searchQuery by remember { mutableStateOf("") }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val inStockProducts = remember(products) { products.filter { it.inStock } }
    val outOfStockProducts = remember(products) { products.filter { !it.inStock } }

    val popularProducts = remember(inStockProducts) {
        inStockProducts.filter { it.isPopular }
    }

    // 5–6 популярных для верхней ленты
    val popularPreview = remember(popularProducts) { popularProducts.shuffled().take(6) }

    // категории из товаров "в наличии"
    val categories = remember(inStockProducts) {
        inStockProducts
            .mapNotNull { it.category }
            .distinct()
            .sorted()
    }

    val filteredProducts = remember(selectedFilter, searchQuery, inStockProducts, outOfStockProducts) {
        val base = when (selectedFilter) {
            is CatalogFilter.Popular -> inStockProducts.filter { it.isPopular }
            is CatalogFilter.Category -> {
                val cat = (selectedFilter as CatalogFilter.Category).category
                inStockProducts.filter { it.category == cat }
            }
            is CatalogFilter.OutOfStock -> outOfStockProducts
            is CatalogFilter.All -> inStockProducts
        }

        if (searchQuery.isBlank()) base
        else base.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // 1) Популярные сверху (лента)
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (popularPreview.isNotEmpty()) {
                    PopularRow(
                        items = popularPreview,
                        onOpenDetails = onOpenDetails,
                        onOpenAllPopular = {
                            selectedFilter = CatalogFilter.Popular
                            scope.launch {
                                gridState.animateScrollToItem(2)
                            }
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // 2) Поиск
            item(span = { GridItemSpan(maxLineSpan) }) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск по названию") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
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

            // 3) Категории + Нет в наличии (без "Популярные")
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryChipsRow(
                    categories = categories,
                    selectedFilter = selectedFilter,
                    onSelectAll = {
                        selectedFilter = CatalogFilter.All
                        scope.launch { gridState.animateScrollToItem(2) }
                    },
                    onSelectCategory = { cat ->
                        selectedFilter = CatalogFilter.Category(cat)
                        scope.launch { gridState.animateScrollToItem(2) }
                    },
                    onSelectOutOfStock = {
                        selectedFilter = CatalogFilter.OutOfStock
                        scope.launch { gridState.animateScrollToItem(2) }
                    }
                )
            }

            // 4) Сетка товаров
            items(filteredProducts, key = { it.id }) { product ->
                ProductCardLarge(
                    product = product,
                    onAddToCart = onAddToCart,
                    onOpenDetails = { onOpenDetails(product) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(6.dp))
            }
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
            Text(
                "Популярные",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                    Box {
                        AsyncImage(
                            model = url,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.18f)
                                        )
                                    )
                                )
                        )
                    }
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
    onSelectCategory: (ProductCategory) -> Unit,
    onSelectOutOfStock: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        // Все (в наличии)
        item {
            FilterChip(
                selected = selectedFilter is CatalogFilter.All,
                onClick = onSelectAll,
                label = { Text("Все") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        // Категории (в наличии)
        items(categories) { cat ->
            FilterChip(
                selected = selectedFilter is CatalogFilter.Category &&
                        (selectedFilter as CatalogFilter.Category).category == cat,
                onClick = { onSelectCategory(cat) },
                label = {
                    Text(
                        when (cat) {
                            ProductCategory.VEGETABLES -> "🥕 Овощи"
                            ProductCategory.FRUITS -> "🍊 Фрукты"
                            ProductCategory.BERRIES -> "🍓 Ягоды"
                            ProductCategory.GREENS -> "🌿 Зелень"
                            ProductCategory.NUTS -> "🥜 Орехи / сухофрукты"
                            ProductCategory.OTHER -> "✨ Другое"
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        // Нет в наличии
        item {
            FilterChip(
                selected = selectedFilter is CatalogFilter.OutOfStock,
                onClick = onSelectOutOfStock,
                label = { Text("Нет в наличии") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}




// Ряд кнопок-фильтров: Популярные, Овощи, Фрукты, ... , Все
// Ряд кнопок-фильтров (старый вариант). Сейчас в каталоге используется CategoryChipsRow.
// Оставляем, чтобы не мешал, но убираем TODO(), чтобы не было риска краша.
@Composable
fun CategoryFilterRow(
    selectedFilter: CatalogFilter,
    onFilterSelected: (CatalogFilter) -> Unit
) {
    val filters = listOf<CatalogFilter>(
        CatalogFilter.All,
        CatalogFilter.Popular,
        CatalogFilter.Category(ProductCategory.VEGETABLES),
        CatalogFilter.Category(ProductCategory.FRUITS),
        CatalogFilter.Category(ProductCategory.BERRIES),
        CatalogFilter.Category(ProductCategory.GREENS),
        CatalogFilter.Category(ProductCategory.NUTS),
        CatalogFilter.Category(ProductCategory.OTHER),
        CatalogFilter.OutOfStock
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
                is CatalogFilter.OutOfStock -> "Нет в наличии"
            }

            val isSelected = when {
                selectedFilter is CatalogFilter.Popular && filter is CatalogFilter.Popular -> true
                selectedFilter is CatalogFilter.All && filter is CatalogFilter.All -> true
                selectedFilter is CatalogFilter.OutOfStock && filter is CatalogFilter.OutOfStock -> true
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // ✅ сделал фото покрупнее
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                if (product.imageUrl != null) {
                    Box {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.16f)
                                        )
                                    )
                                )
                        )
                    }
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
                maxLines = 2,
                modifier = Modifier.heightIn(min = 40.dp)            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = buildString {
                            append(product.price.toInt())
                            append(" ₽ / ")
                            append(if (product.unit == UnitType.KG) "кг" else "шт")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

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

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = product.originCountry ?: " ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val itemsSubtotal = cartItems.sumOf { it.product.price * it.quantity }
    val isFreeDelivery = itemsSubtotal >= 1500.0
    val deliveryFee = if (isFreeDelivery) 0.0 else 200.0
    val paymentDiscountPercent = 0.05


    // Показывать ли диалог с формой оформления заказа
    var showOrderDialog by remember { mutableStateOf(false) }
    var isSendingOrder by remember { mutableStateOf(false) }

    // Поля клиента (для диалога)
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var customerComment by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }

    LaunchedEffect(showOrderDialog) {
        if (!showOrderDialog) return@LaunchedEffect

        // 1) Телефон из авторизации (если поле пустое)
        val authPhone = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber
        if (customerPhone.isBlank() && !authPhone.isNullOrBlank()) {
            customerPhone = authPhone
        }

        // 2) Имя/адрес из Firestore (только если поля пустые)
        loadUserProfile(
            onResult = { profile ->
                if (profile == null) return@loadUserProfile
                if (customerName.isBlank() && profile.name.isNotBlank()) customerName = profile.name
                if (customerAddress.isBlank() && profile.address.isNotBlank()) customerAddress = profile.address
                if (customerPhone.isBlank() && profile.phone.isNotBlank()) customerPhone = profile.phone
            }
        )
    }


    // Текст ошибки в диалоге
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
            val discount = if (paymentMethod == PaymentMethod.CASH) {
                itemsSubtotal * paymentDiscountPercent
            } else {
                0.0
            }
            val totalPrice = itemsSubtotal - discount + deliveryFee
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(cartItems.size) { index ->
                    val item = cartItems[index]
                    CartItemRow(
                        item = item,
                        onUpdateQuantity = onUpdateQuantity,
                        onRemoveFromCart = onRemoveFromCart
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Способ оплаты",
                        style = MaterialTheme.typography.bodyMedium
                    )

            // Примерная сумма

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = paymentMethod == PaymentMethod.CASH,
                            onClick = { paymentMethod = PaymentMethod.CASH }
                        )
                        Text(PaymentMethod.CASH.label)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = paymentMethod == PaymentMethod.CARD,
                            onClick = { paymentMethod = PaymentMethod.CARD }
                        )
                        Text(PaymentMethod.CARD.label)
                    }
                }

                item {
                    Text(
                        text = "Товары: ${itemsSubtotal.toInt()} ₽",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (discount > 0.0) {
                        Text(
                            text = "Скидка за наличные: -${discount.toInt()} ₽",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (deliveryFee > 0.0) "Доставка: 200 ₽" else "Доставка: бесплатно",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Итого: ~ ${totalPrice.toInt()} ₽",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Фактическая сумма может немного отличаться из-за точного веса (+/− ~100 г).",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (!isFreeDelivery) {
                        val remaining = (1500.0 - itemsSubtotal).coerceAtLeast(0.0)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Добавьте еще ${remaining.toInt()} ₽ для бесплатной доставки.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Button(
                        onClick = { showOrderDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Оформить заказ")
                    }
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

                                    val message = buildOrderMessage(
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



                                    sendOrderViaFirebaseTelegram(
                                        context = context,
                                        order = order,
                                        onSuccess = {
                                            saveOrderToHistory(order, "TELEGRAM")

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

                                }
                            }
                        },
                        enabled = !isSendingOrder
                    ) {
                        Text("Telegram")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
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

                                    val message = buildOrderMessage(
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

                                    // ✅ 1) Собираем orderMap (как для Telegram)
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

                                    // ✅ 2) Сохраняем профиль пользователя (имя/телефон/адрес)
                                    saveUserProfileFromOrder(
                                        name = customerName,
                                        phone = customerPhone,
                                        address = customerAddress
                                    )

                                    // ✅ 3) Сохраняем заказ в историю
                                    saveOrderToHistory(order, "WHATSAPP")

                                    // ✅ 4) Отправляем в WhatsApp (как раньше)
                                    sendOrderViaWhatsApp(context, message, "+79687008070")

                                    showOrderDialog = false
                                    isSendingOrder = false

                                    // Не очищаем имя/телефон/адрес — они должны остаться для следующего заказа
                                    customerComment = ""
                                }
                            }
                        },
                        enabled = !isSendingOrder
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

                Row {
                    IconButton(
                        onClick = { showDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Изменить количество"
                        )
                    }

                    IconButton(
                        onClick = { onRemoveFromCart(item.product.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить"
                        )
                    }
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
    val unitText = if (product.unit == UnitType.KG) "кг" else "шт"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("О товаре") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Цена",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${product.price.toInt()} ₽ / $unitText",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(onClick = { showQtyDialog = true }) {
                        Text("В корзину")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    product.originCountry?.takeIf { it.isNotBlank() }?.let { country ->
                        Text(
                            text = "Страна: $country",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Описание",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = product.description?.takeIf { it.isNotBlank() }
                                ?: "Описание пока не заполнено.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Характеристики",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Единица: $unitText",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        product.originCountry?.takeIf { it.isNotBlank() }?.let { country ->
                            Text(
                                text = "Производство: $country",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }            }
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
            modifier = Modifier.fillMaxWidth()
            ,
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
    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
        }
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Text("Добавить новый товар")
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
    comment: String,
    paymentMethod: PaymentMethod,
    deliveryFee: Double,
    discount: Double,
    total: Double
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

    val subtotal = cartItems.sumOf { it.product.price * it.quantity }

    sb.append("\nТовары: ${subtotal.toInt()} ₽\n")
    if (discount > 0.0) {
        sb.append("Скидка за наличные: -${discount.toInt()} ₽\n")
    }
    sb.append(if (deliveryFee > 0.0) "Доставка: 200 ₽\n" else "Доставка: бесплатно\n")
    sb.append("Итого: ~ ${total.toInt()} ₽\n")
    sb.append("(Фактическая сумма может немного отличаться из-за точного веса товара)\n\n")

    sb.append("Данные клиента:\n")
    sb.append("Имя: $customerName\n")
    sb.append("Телефон: $customerPhone\n")
    sb.append("Адрес: $customerAddress\n")
    sb.append("Оплата: ${paymentMethod.label}\n")

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
    comment: String,
    paymentMethod: PaymentMethod,
    deliveryFee: Double,
    discount: Double,
    total: Double
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

    val subtotal = cartItems.sumOf { it.product.price * it.quantity }

    return mapOf(
        "type" to "ORDER",
        "createdAt" to System.currentTimeMillis(),
        "customerName" to customerName,
        "customerPhone" to customerPhone,
        "customerAddress" to customerAddress,
        "comment" to comment,
        "paymentMethod" to paymentMethod.name,
        "deliveryFee" to deliveryFee,
        "discount" to discount,
        "subtotal" to subtotal,
        "total" to total,
        "items" to items
    )
}

fun buildSupportMap(
    question: String,
    phone: String
): Map<String, Any> {
    return mapOf(
        "type" to "SUPPORT",
        "createdAt" to System.currentTimeMillis(),
        "phone" to phone.trim(),
        "question" to question.trim()
    )
}



fun buildRequestMap(
    customerName: String,
    customerPhone: String,
    requestedProduct: String,
    requestedQuantity: String,
    comment: String
): Map<String, Any> {
    return mapOf(
        "type" to "REQUEST",
        "createdAt" to System.currentTimeMillis(),
        "customerName" to customerName.trim(),
        "customerPhone" to customerPhone.trim(),
        "requestedProduct" to requestedProduct.trim(),
        "requestedQuantity" to requestedQuantity.trim(),
        "comment" to comment.trim()
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
    "description" to description,
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
    val description = getString("description")
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
        description = description,
        isPopular = isPopular,
        inStock = inStock
    )
}



fun ensureUserDocExists(
    onDone: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    val db = FirebaseFirestore.getInstance()
    val ref = db.collection("users").document(user.uid)
    val now = System.currentTimeMillis()
    val phone = user.phoneNumber ?: ""

    // Транзакция: чтобы createdAt не перетирался и пустые поля не ломали существующие
    db.runTransaction { tr ->
        val snap = tr.get(ref)

        if (!snap.exists()) {
            // создаём нового с пустыми полями
            val data = mapOf(
                "name" to "",
                "address" to "",
                "phone" to phone,
                "createdAt" to now,
                "updatedAt" to now
            )
            tr.set(ref, data, SetOptions.merge())
        } else {
            // пользователь уже был — только обновляем phone/updatedAt
            val data = mapOf(
                "phone" to phone,
                "updatedAt" to now
            )
            tr.set(ref, data, SetOptions.merge())
        }
        null
    }.addOnSuccessListener {
        onDone()
    }.addOnFailureListener { e ->
        onError(e.message ?: "Не удалось создать пользователя в базе")
    }
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


