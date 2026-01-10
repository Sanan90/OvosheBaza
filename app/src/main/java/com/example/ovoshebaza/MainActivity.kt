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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import com.example.ovoshebaza.ui.theme.VeggieTheme

import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.example.ovoshebaza.loadUserProfile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import java.util.Locale

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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeggieShopApp() {
    NotificationSetup()
    val navController = rememberNavController()

    val shopViewModel: ShopViewModel = viewModel()
    val products = shopViewModel.products
    val cartItems = shopViewModel.cartItems
    val context = LocalContext.current

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: Screen.Catalog.route

    // 👉 стейты для логотипа и PIN-диалога
    var logoClickCount by remember { mutableStateOf(0) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var adminPin by remember { mutableStateOf("") }
    var adminPinError by remember { mutableStateOf<String?>(null) }

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

            if (currentRoute == Screen.Cart.route) {

                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Корзина",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            } else {
                val topBarBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF55C76B),
                        Color(0xFF7DDC7B),
                        Color(0xFFF6B24C),
                        Color(0xFFF0932B)
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
                                    Text(
                                        text = "Клинская Овощебаза",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
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
            }
        },
        bottomBar = {
            if (currentRoute != Screen.Admin.route) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    listOf(Screen.Catalog, Screen.Cart, Screen.Request, Screen.Profile).forEach { screen ->

                        val icon = when (screen) {
                            Screen.Catalog -> Icons.Default.Store
                            Screen.Cart -> Icons.Default.ShoppingCart
                            Screen.Request -> Icons.Default.NoteAdd
                            Screen.Profile -> Icons.Default.Person
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
                onClearCart = {
                    shopViewModel.clearCart()
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
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_screen),
            contentDescription = "Экран загрузки",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
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
    onClearCart: () -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onUpdateProduct: (Product) -> Unit,
    onAddProduct: (Product) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    var orders by remember { mutableStateOf<List<OrderSummary>>(emptyList()) }
    var ordersError by remember { mutableStateOf<String?>(null) }
    var isOrdersLoading by remember { mutableStateOf(false) }

    DisposableEffect(user?.uid) {
        if (user == null) {
            orders = emptyList()
            ordersError = null
            isOrdersLoading = false
            return@DisposableEffect onDispose {}
        }

        ordersError = null
        isOrdersLoading = true
        val registration = listenUserOrders(
            onResult = { loaded ->
                orders = loaded
                isOrdersLoading = false
            },
            onError = { error ->
                ordersError = error
                isOrdersLoading = false
            }
        )
        onDispose {
            registration?.remove()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Catalog.route,
        modifier = modifier
    ) {
        composable(Screen.Catalog.route) {
            CatalogScreen(
                products = products,
                cartItems = cartItems,
                onAddToCart = onAddToCart,
                onUpdateQuantity = onUpdateQuantity,
                onOpenDetails = { product ->
                    navController.navigate("product/${product.id}")
                }
            )
        }

        composable(Screen.Cart.route) {
            CartScreen(
                cartItems = cartItems,
                onUpdateQuantity = onUpdateQuantity,
                onRemoveFromCart = onRemoveFromCart,
                onClearCart = onClearCart
            )
        }

        composable(Screen.Request.route) {
            RequestProductScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                products = products,
                onAddToCart = onAddToCart,
                cartItems = cartItems,
                orders = orders,
                isOrdersLoading = isOrdersLoading,
                ordersError = ordersError,
                onClearCart = onClearCart,
                onAuthRequested = { navController.navigate("auth") },
                onOpenProduct = { productId ->
                    navController.navigate("product/$productId")
                }
            )
        }

        composable("auth") {
            AuthScreen(
                onSignedIn = {
                    ensureUserDocExists(
                        onDone = { navController.popBackStack() },
                        onError = { msg ->
                            android.widget.Toast
                                .makeText(
                                    navController.context,
                                    msg,
                                    android.widget.Toast.LENGTH_LONG
                                )
                                .show()
                        }
                    )
                }
            )
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
                    cartItems = cartItems,
                    onAddToCart = onAddToCart,
                    onUpdateQuantity = onUpdateQuantity
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
    cartItems: List<CartItem>,
    onAddToCart: (Product, Double) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
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

            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeHeroCard()
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryPromoRow(
                    selectedFilter = selectedFilter,
                    onSelectCategory = { category ->
                        selectedFilter = CatalogFilter.Category(category)
                        scope.launch { gridState.animateScrollToItem(3) }
                    }
                )
            }


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
                        scope.launch { gridState.animateScrollToItem(3) }
                    },
                    onSelectCategory = { cat ->
                        selectedFilter = CatalogFilter.Category(cat)
                        scope.launch { gridState.animateScrollToItem(3) }
                    },
                    onSelectOutOfStock = {
                        selectedFilter = CatalogFilter.OutOfStock
                        scope.launch { gridState.animateScrollToItem(3) }
                    }
                )
            }

            // 4) Сетка товаров
            items(filteredProducts, key = { it.id }) { product ->
                val currentQuantity = cartItems.firstOrNull { it.product.id == product.id }?.quantity ?: 0.0
                ProductCardLarge(
                    product = product,
                    currentQuantity = currentQuantity,
                    onAddToCart = onAddToCart,
                    onUpdateQuantity = onUpdateQuantity,
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
fun HomeHeroCard() {
    val cardShape = RoundedCornerShape(32.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(8.dp, cardShape)
                .clip(cardShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFDF7E7),
                            Color(0xFFF6EBD4)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Свежие овощи\nи фрукты",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Поставка товара каждый день",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class PromoCategory(
    val title: String,
    val iconRes: Int,
    val category: ProductCategory
)

@Composable
fun CategoryPromoRow(
    selectedFilter: CatalogFilter,
    onSelectCategory: (ProductCategory) -> Unit
) {
    val categories = listOf(
        PromoCategory("Овощи", R.drawable.vegetables, ProductCategory.VEGETABLES),
        PromoCategory("Фрукты", R.drawable.fruits, ProductCategory.FRUITS),
        PromoCategory("Зелень", R.drawable.green, ProductCategory.GREENS),
        PromoCategory("Ягоды", R.drawable.berries, ProductCategory.BERRIES)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categories.forEach { item ->
            val isSelected = selectedFilter is CatalogFilter.Category &&
                    (selectedFilter as CatalogFilter.Category).category == item.category

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp) // ✅ карточки ниже, не вытянутые
                    .clickable { onSelectCategory(item.category) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    // 🔥 КРУПНАЯ ИКОНКА
                    Image(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(62.dp) // ⬅️ заметно больше
                    )

                    Text(
                        text = item.title,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
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
    currentQuantity: Double,
    onAddToCart: (Product, Double) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
    onOpenDetails: () -> Unit
) {
    var showQuantityDialog by remember { mutableStateOf(false) }
    var isQuickAddExpanded by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(28.dp)
    val quickSteps = remember(product.unit) {
        if (product.unit == UnitType.KG) {
            listOf(
                QuickStep(label = "-0.5", delta = -0.5),
                QuickStep(label = "-1 кг", delta = -1.0),
                QuickStep(label = "+0.5", delta = 0.5),
                QuickStep(label = "+1 кг", delta = 1.0)
            )
        } else {
            listOf(
                QuickStep(label = "-1 шт", delta = -1.0),
                QuickStep(label = "+1 шт", delta = 1.0)
            )
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 180.dp
        val cardMinHeight = if (isCompact) 190.dp else 220.dp
        val imageHeight = if (isCompact) 110.dp else 130.dp
        val cartButtonSize = if (isCompact) 52.dp else 64.dp
        val quickButtonCountForLayout = 5
        val quickButtonSpacing = 0.dp
        val overlaySidePadding = if (isCompact) 4.dp else 8.dp
        val perSideCount = quickButtonCountForLayout / 2
        val availablePerSide = (maxWidth - overlaySidePadding * 2 - cartButtonSize - quickButtonSpacing * 2) / 2
        val desiredQuickButtonSize = cartButtonSize * 0.82f
        val quickButtonSize = minOf(
            desiredQuickButtonSize,
            ((availablePerSide - quickButtonSpacing * (perSideCount - 1)) / perSideCount)
                .coerceAtLeast(cartButtonSize * 0.7f)
        )
        // Смещения по вертикали для внутренних и внешних кнопок (дуга)
        val innerOffset = quickButtonSize * 1f   // ближние к корзине кнопки - небольшой подъем
        val outerOffset = quickButtonSize * 1.1f   // крайние кнопки - больший подъем
        // Высота нижней области, увеличена с учетом дуги, чтобы кнопки не перекрывали текст
        val controlsHeight = cartButtonSize + outerOffset + 6.dp
        val buttonInsetRatio = 0.8f  // Высота кнопки корзины
        val buttonYOffset = cartButtonSize * (1f - buttonInsetRatio)

        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = cardMinHeight)
                    .clickable { onOpenDetails() },
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6EBD4)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .padding(bottom = controlsHeight),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imageHeight)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.heightIn(min = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = buildString {
                            append(product.price.toInt())
                            append(" ₽ / ")
                            append(if (product.unit == UnitType.KG) "кг" else "шт")
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Блок кнопок корзины и изменения веса
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = buttonYOffset)
                    .fillMaxWidth()
                    .padding(horizontal = overlaySidePadding)
            ) {
                // Центральная кнопка корзины (без изменений)
                CartButton(
                    enabled = product.inStock,
                    onClick = { isQuickAddExpanded = !isQuickAddExpanded },
                    size = cartButtonSize,
                    currentQuantity = currentQuantity,
                    unit = product.unit,
                    modifier = Modifier.align(Alignment.Center)
                )

                if (isQuickAddExpanded) {
                    val leftButtons = quickSteps.take(quickSteps.size / 2)
                    val rightButtons = quickSteps.takeLast(quickSteps.size / 2)
                    val sidePadding = cartButtonSize / 2 + 6.dp

                    // Левая группа кнопок (убавление веса)
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(end = sidePadding),
                        horizontalArrangement = Arrangement.spacedBy(quickButtonSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        leftButtons.forEachIndexed { index, step ->
                            QuickStepButton(
                                step = step,
                                product = product,
                                enabled = product.inStock,
                                currentQuantity = currentQuantity,
                                onAddToCart = onAddToCart,
                                onUpdateQuantity = onUpdateQuantity,
                                size = quickButtonSize,
                                modifier = if (leftButtons.size > 1) {
                                    if (index == 0) Modifier.offset(y = -outerOffset) else Modifier.offset(y = -innerOffset)
                                } else {
                                    Modifier.offset(y = -innerOffset)
                                }
                            )
                        }
                    }

                    // Правая группа кнопок (прибавление веса)
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(start = sidePadding),
                        horizontalArrangement = Arrangement.spacedBy(quickButtonSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rightButtons.forEachIndexed { index, step ->
                            QuickStepButton(
                                step = step,
                                product = product,
                                enabled = product.inStock,
                                currentQuantity = currentQuantity,
                                onAddToCart = onAddToCart,
                                onUpdateQuantity = onUpdateQuantity,
                                size = quickButtonSize,
                                modifier = if (rightButtons.size > 1) {
                                    if (index == rightButtons.lastIndex) Modifier.offset(y = -outerOffset) else Modifier.offset(y = -innerOffset)
                                } else {
                                    Modifier.offset(y = -innerOffset)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Описание шага быстрого изменения веса
private data class QuickStep(val label: String, val delta: Double)

// Компонент кнопки изменения веса (круглая, выпуклая)
@Composable
private fun QuickStepButton(
    step: QuickStep,
    product: Product,
    enabled: Boolean,
    currentQuantity: Double,
    onAddToCart: (Product, Double) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFE7C4),
            Color(0xFFF5B56A),
            Color(0xFFE3903B)
        )
    )
    val highlight = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.8f),
            Color.Transparent
        ),
        center = Offset(40f, 40f),
        radius = 180f
    )
    val shadowOverlay = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.18f)
        )
    )
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled) {
                if (step.delta > 0) {
                    onAddToCart(product, step.delta)
                } else {
                    val nextQuantity = (currentQuantity + step.delta).coerceAtLeast(0.0)
                    onUpdateQuantity(product.id, nextQuantity)
                }
            },
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, CircleShape)
                .border(1.dp, Color(0xFFFFF5E7), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(highlight, CircleShape)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(shadowOverlay, CircleShape)
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val iconRes = when {
                    step.label.contains("шт") && step.delta == -1.0 -> R.drawable.minus_1st
                    step.label.contains("шт") && step.delta == 1.0 -> R.drawable.plus_1st
                    step.delta == -1.0 -> R.drawable.minus_1
                    step.delta == -0.5 -> R.drawable.minus_05
                    step.delta == 0.5 -> R.drawable.plus_05
                    step.delta == 1.0 -> R.drawable.plus_1
                    else -> null
                }
                if (iconRes != null) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = step.label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Text(
                        text = step.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }
                    )
                }
            }
        }
    }
}

// Компонент кнопки корзины (без изменений)
@Composable
private fun CartButton(
    enabled: Boolean,
    onClick: () -> Unit,
    size: Dp,
    currentQuantity: Double,
    unit: UnitType,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFE3B8),
            Color(0xFFF6B25C),
            Color(0xFFE48C35)
        )
    )
    val highlight = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.85f),
            Color.Transparent
        ),
        center = Offset(50f, 50f),
        radius = 220f
    )
    val shadowOverlay = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.2f)
        )
    )
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 16.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, CircleShape)
                .border(1.dp, Color(0xFFFFF7EB), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(highlight, CircleShape)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(shadowOverlay, CircleShape)
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.order_button),
                    contentDescription = "Корзина",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
            if (currentQuantity > 0) {
                val quantityText = if (unit == UnitType.KG) {
                    if (currentQuantity % 1.0 == 0.0) {
                        currentQuantity.toInt().toString()
                    } else {
                        String.format(Locale.US, "%.1f", currentQuantity)
                    }
                } else {
                    currentQuantity.toInt().toString()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF6E3B1F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quantityText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}








private fun categoryLabel(category: ProductCategory): String =
    when (category) {
        ProductCategory.VEGETABLES -> "Овощи"
        ProductCategory.FRUITS -> "Фрукты"
        ProductCategory.BERRIES -> "Ягоды"
        ProductCategory.GREENS -> "Зелень"
        ProductCategory.NUTS -> "Орехи/сухофрукты"
        ProductCategory.OTHER -> "Другое"
    }








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
        val authPhone = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    product: Product,
    onBack: () -> Unit,
    cartItems: List<CartItem>,
    onAddToCart: (Product, Double) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit
) {
    var isQuickAddExpanded by remember { mutableStateOf(false) }
    val unitText = if (product.unit == UnitType.KG) "кг" else "шт"
    val currentQuantity = cartItems.firstOrNull { it.product.id == product.id }?.quantity ?: 0.0
    val quickSteps = remember(product.unit) {
        if (product.unit == UnitType.KG) {
            listOf(
                QuickStep(label = "-500г", delta = -0.5),
                QuickStep(label = "-1 кг", delta = -1.0),
                QuickStep(label = "+500г", delta = 0.5),
                QuickStep(label = "+1 кг", delta = 1.0)
            )
        } else {
            listOf(
                QuickStep(label = "-1 шт", delta = -1.0),
                QuickStep(label = "+1 шт", delta = 1.0)
            )
        }
    }

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

                    val canAddToCart = product.inStock
                    val leftButtons = quickSteps.filter { it.delta < 0 }
                    val rightButtons = quickSteps.filter { it.delta > 0 }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isQuickAddExpanded) {
                            leftButtons.forEach { step ->
                                QuickStepButton(
                                    step = step,
                                    product = product,
                                    enabled = canAddToCart,
                                    currentQuantity = currentQuantity,
                                    onAddToCart = onAddToCart,
                                    onUpdateQuantity = onUpdateQuantity,
                                    size = 44.dp
                                )
                            }
                        }
                        CartButton(
                            enabled = canAddToCart,
                            onClick = { isQuickAddExpanded = !isQuickAddExpanded },
                            size = 56.dp,
                            currentQuantity = currentQuantity,
                            unit = product.unit
                        )
                        if (isQuickAddExpanded) {
                            rightButtons.forEach { step ->
                                QuickStepButton(
                                    step = step,
                                    product = product,
                                    enabled = canAddToCart,
                                    currentQuantity = currentQuantity,
                                    onAddToCart = onAddToCart,
                                    onUpdateQuantity = onUpdateQuantity,
                                    size = 44.dp
                                )
                            }
                        }
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


// Красиво показываем количество:
// 1.0 -> "1", 1.5 -> "1.5"
fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

// Собираем данные заказа
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
    val now = System.currentTimeMillis()

    return mapOf(
        "type" to "ORDER",
        "createdAt" to now,
        "customerName" to customerName,
        "customerPhone" to customerPhone,
        "customerAddress" to customerAddress,
        "comment" to comment,
        "paymentMethod" to paymentMethod.name,
        "deliveryFee" to deliveryFee,
        "discount" to discount,
        "subtotal" to subtotal,
        "total" to total,
        "status" to "RECEIVED",
        "statusUpdatedAt" to now,
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


