package com.example.ovoshebaza.ui.root

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ovoshebaza.NotificationSetup
import com.example.ovoshebaza.R
import com.example.ovoshebaza.Screen
import com.example.ovoshebaza.ShopViewModel
import com.example.ovoshebaza.buildSupportMap
import com.example.ovoshebaza.sendOrderViaFirebaseTelegram
import com.example.ovoshebaza.ui.navigation.AppNavHost
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeggieShopApp() {
    NotificationSetup()
    val navController = rememberNavController()

    val shopViewModel: ShopViewModel = viewModel()
    val products = shopViewModel.products
    val cartItems = shopViewModel.cartItems
    val context = androidx.compose.ui.platform.LocalContext.current

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


                Box(
                    modifier = Modifier
                        .fillMaxWidth()

                ) {
                    Image(
                        painter = painterResource(id = R.drawable.header_fruit),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )


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

            SupportIcon(
                iconRes = helperIconRes,
                isVisible = !hideSupportIcon,
                iconOffsetX = supportIconOffsetX,
                iconAlpha = supportIconAlpha,
                bounceProgress = supportIconOffset.value,
                bottomPadding = innerPadding.calculateBottomPadding(),
                onClick = {
                    showSupportDialog = true
                    supportQuestion = ""
                    supportPhone = ""
                    supportError = null
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }

    SupportDialog(
        visible = showSupportDialog,
        question = supportQuestion,
        onQuestionChange = { supportQuestion = it },
        phone = supportPhone,
        onPhoneChange = { supportPhone = it },
        errorText = supportError,
        isSending = isSendingSupport,
        onDismiss = { showSupportDialog = false },
        onSend = {
            if (supportQuestion.isBlank()) {
                supportError = "Введите вопрос"
                return@SupportDialog
            }
            if (supportPhone.isBlank()) {
                supportError = "Введите номер телефона"
                return@SupportDialog
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
    )


    AdminPinDialog(
        visible = showAdminPinDialog,
        pin = adminPin,
        onPinChange = { newText ->
            val digitsOnly = newText.filter { it.isDigit() }
            if (digitsOnly.length <= 4) {
                adminPin = digitsOnly
            }
        },
        errorText = adminPinError,
        onDismiss = {
            showAdminPinDialog = false
            adminPin = ""
            adminPinError = null
        },
        onConfirm = {
            if (adminPin == "2009") {
                showAdminPinDialog = false
                adminPin = ""
                navController.navigate(Screen.Admin.route)
            } else {
                adminPinError = "Неверный код"
            }
        }
    )
}