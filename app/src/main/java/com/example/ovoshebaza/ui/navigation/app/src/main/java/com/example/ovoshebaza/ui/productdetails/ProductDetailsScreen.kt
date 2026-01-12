package com.example.ovoshebaza.ui.productdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ovoshebaza.CartItem
import com.example.ovoshebaza.Product
import com.example.ovoshebaza.UnitType
import com.example.ovoshebaza.ui.components.CartButton
import com.example.ovoshebaza.ui.components.QuickStep
import com.example.ovoshebaza.ui.components.QuickStepButton

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
                                    size = 36.dp
                                )
                            }
                        }
                        CartButton(
                            enabled = canAddToCart,
                            onClick = { isQuickAddExpanded = !isQuickAddExpanded },
                            size = 48.dp,
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
                                    size = 36.dp
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
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
                }
            }
        }
    }
}