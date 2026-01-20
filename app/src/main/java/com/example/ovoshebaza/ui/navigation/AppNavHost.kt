package com.example.ovoshebaza.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ovoshebaza.ui.admin.AdminScreen
import com.example.ovoshebaza.CartItem
import com.example.ovoshebaza.ui.cart.CartScreen
import com.example.ovoshebaza.ui.catalog.CatalogScreen
import com.example.ovoshebaza.OrderSummary
import com.example.ovoshebaza.Product
import com.example.ovoshebaza.ui.productdetails.ProductDetailsScreen
import com.example.ovoshebaza.ui.request.RequestProductScreen
import com.example.ovoshebaza.Screen
import com.example.ovoshebaza.ensureUserDocExists
import com.example.ovoshebaza.listenUserOrders
import com.google.firebase.auth.FirebaseAuth
import com.example.ovoshebaza.ui.auth.AuthScreen
import com.example.ovoshebaza.ui.profile.ProfileScreen

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
            var catalogSearchQuery by rememberSaveable { mutableStateOf("") }
            CatalogScreen(
                products = products,
                cartItems = cartItems,
                searchQuery = catalogSearchQuery,
                onSearchQueryChange = { catalogSearchQuery = it },
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