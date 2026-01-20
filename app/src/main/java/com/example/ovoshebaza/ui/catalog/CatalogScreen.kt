package com.example.ovoshebaza.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ovoshebaza.CartItem
import com.example.ovoshebaza.Product
import com.example.ovoshebaza.ProductCategory
import com.example.ovoshebaza.R
import com.example.ovoshebaza.UnitType
import com.example.ovoshebaza.formatQuantity
import com.example.ovoshebaza.ui.components.CartButton
import com.example.ovoshebaza.ui.components.QuickStep
import com.example.ovoshebaza.ui.components.QuickStepButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect


sealed class CatalogFilter {
    // Показываем только популярные товары
    object Popular : CatalogFilter()
    // Показываем товары конкретной категории (овощи, фрукты и т.д.)
    data class Category(val category: ProductCategory) : CatalogFilter()
    // Показываем все товары
    object All : CatalogFilter()

    // Показываем только новинки
    object New : CatalogFilter()

    // Показываем товары, которых нет в наличии
    object OutOfStock : CatalogFilter()
}

@OptIn(ExperimentalFoundationApi::class)

@Composable
fun CatalogScreen(
    products: List<Product>,
    cartItems: List<CartItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddToCart: (Product, Double) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
    onOpenDetails: (Product) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<CatalogFilter>(CatalogFilter.All) }
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
            is CatalogFilter.New -> inStockProducts.filter { it.isNew }
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

    var expandedProductId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedFilter, searchQuery) {
        expandedProductId = null
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
                .padding(horizontal = 4.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
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
                        onValueChange = onSearchQueryChange,
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
                    onSelectNew = {
                        selectedFilter = CatalogFilter.New
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
                val isExpanded = expandedProductId == product.id
                ProductCardLarge(
                    product = product,
                    currentQuantity = currentQuantity,
                    onAddToCart = onAddToCart,
                    onUpdateQuantity = onUpdateQuantity,
                    onOpenDetails = {
                        expandedProductId = null
                        onOpenDetails(product)
                    },
                    isQuickAddExpanded = isExpanded,
                    onQuickAddToggle = {
                        expandedProductId = if (isExpanded) null else product.id
                    }
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
                        modifier = Modifier.size(56.dp) // ⬅️ чуть меньше для текста
                    )

                    Text(
                        text = item.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 14.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
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
    onSelectNew: () -> Unit,
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

        // Новинки
        item {
            FilterChip(
                selected = selectedFilter is CatalogFilter.New,
                onClick = onSelectNew,
                label = { Text("Новинки") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                            ProductCategory.OTHER -> "✨ Другое"                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

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
        CatalogFilter.New,
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
                is CatalogFilter.New -> "Новинки"
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
                selectedFilter is CatalogFilter.New && filter is CatalogFilter.New -> true
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
    onOpenDetails: () -> Unit,
    isQuickAddExpanded: Boolean,
    onQuickAddToggle: () -> Unit
) {
    var showQuantityDialog by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(22.dp)
    val cardBackground = Color(0xFFEFF6EF)
    val cardBorder = Color(0xFFD7E6D1)
    val priceBadgeBackground = Color(0xFFDDEBD2)
    val priceBadgeBorder = Color(0xFFBFD4B2)
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
        val cardMinHeight = if (isCompact) 170.dp else 200.dp
        val imageHeight = if (isCompact) 140.dp else 140.dp
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
        val controlsHeight = cartButtonSize + outerOffset - 20.dp
        val buttonInsetRatio = 0.8f  // Высота кнопки корзины
        val buttonYOffset = cartButtonSize * (1f - buttonInsetRatio)

        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = cardMinHeight)
                    .border(1.dp, cardBorder, cardShape)
                    .clickable { onOpenDetails() },
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .padding(bottom = controlsHeight),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imageHeight)
                            .clip(RoundedCornerShape(16.dp))
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        ),
                        maxLines = 2,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                    Box(
                        modifier = Modifier
                            .border(1.dp, priceBadgeBorder, RoundedCornerShape(12.dp))
                            .background(priceBadgeBackground, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = buildString {
                                append(product.price.toInt())
                                append(" ₽ / ")
                                append(if (product.unit == UnitType.KG) "кг" else "шт")
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        )

                        }
                    }

                }

            if (product.isNew) {
                Image(
                    painter = painterResource(id = R.drawable.new_position3),
                    contentDescription = "Новинка",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .offset(x = 12.dp, y = (-12).dp)
                        .size(44.dp)
                )
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
                    onClick = { onQuickAddToggle() },
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
