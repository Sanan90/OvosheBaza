package com.example.ovoshebaza

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot

class ShopViewModel(application: Application) : AndroidViewModel(application) {

    // SharedPreferences — локальное хранилище на телефоне
    // Здесь храним и корзину, и кэш товаров
    private val prefs =
        application.getSharedPreferences("cart_storage", Context.MODE_PRIVATE)

    private val cartKey = "cart_items"       // ключ для корзины
    private val productsKey = "cached_products" // ключ для кэша товаров

    // Firestore — наша база данных в облаке
    private val db = FirebaseFirestore.getInstance()

    // Подписка на коллекцию products в Firestore
    private var productsListener: ListenerRegistration? = null

    // Список товаров — сначала из кэша, потом обновится из Firestore
    var products by mutableStateOf<List<Product>>(emptyList())
        private set

    // Корзина — хранится локально на телефоне
    var cartItems by mutableStateOf(listOf<CartItem>())
        private set

    init {
        // 1. Сразу загружаем товары из кэша — каталог покажется мгновенно
        loadProductsFromCache()

        // 2. Подписываемся на Firestore — данные обновятся как только придут из сети
        observeProducts()

        // 3. Восстанавливаем корзину из локального хранилища
        loadCartFromStorage()
    }

    // Начинаем слушать изменения товаров в Firestore в реальном времени
    private fun observeProducts() {
        // Если уже была подписка — снимаем чтобы не было дублей
        productsListener?.remove()

        productsListener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // При ошибке — ничего не делаем, кэш уже показан
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toProduct()
                    }

                    if (list.isNotEmpty()) {
                        // Обновляем список товаров на экране
                        products = list

                        // Сохраняем свежие товары в кэш —
                        // при следующем открытии покажутся мгновенно
                        saveProductsToCache(list)
                    }
                }
            }
    }

    // Сохраняем список товаров в SharedPreferences как JSON
    private fun saveProductsToCache(list: List<Product>) {
        val array = org.json.JSONArray()
        list.forEach { product ->
            val obj = org.json.JSONObject()
            obj.put("id", product.id)
            obj.put("name", product.name)
            obj.put("category", product.category.name)
            obj.put("price", product.price)
            obj.put("unit", product.unit.name)
            obj.put("originCountry", product.originCountry ?: "")
            obj.put("imageUrl", product.imageUrl ?: "")
            obj.put("description", product.description ?: "")
            obj.put("isPopular", product.isPopular)
            obj.put("isNew", product.isNew)
            obj.put("inStock", product.inStock)
            array.put(obj)
        }
        prefs.edit().putString(productsKey, array.toString()).apply()
    }

    // Загружаем товары из кэша при старте приложения
    // Работает мгновенно — не нужен интернет
    private fun loadProductsFromCache() {
        val raw = prefs.getString(productsKey, null) ?: return
        val array = runCatching { org.json.JSONArray(raw) }.getOrNull() ?: return
        val cached = mutableListOf<Product>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            val name = obj.optString("name")
            if (id.isBlank() || name.isBlank()) continue

            val category = runCatching {
                ProductCategory.valueOf(obj.optString("category"))
            }.getOrDefault(ProductCategory.OTHER)

            val unit = runCatching {
                UnitType.valueOf(obj.optString("unit"))
            }.getOrDefault(UnitType.KG)

            cached.add(
                Product(
                    id = id,
                    name = name,
                    category = category,
                    price = obj.optDouble("price", 0.0),
                    unit = unit,
                    originCountry = obj.optString("originCountry").ifBlank { null },
                    imageUrl = obj.optString("imageUrl").ifBlank { null },
                    description = obj.optString("description").ifBlank { null },
                    isPopular = obj.optBoolean("isPopular", false),
                    isNew = obj.optBoolean("isNew", false),
                    inStock = obj.optBoolean("inStock", true)
                )
            )
        }

        // Показываем кэшированные товары сразу, не дожидаясь Firestore
        if (cached.isNotEmpty()) {
            products = cached
        }
    }

    // ---------- КОРЗИНА (хранится локально) ----------

    fun addToCart(product: Product, quantity: Double) {
        if (quantity <= 0.0) return

        val existing = cartItems.find { it.product.id == product.id }

        cartItems = if (existing == null) {
            // Товара нет в корзине — добавляем
            cartItems + CartItem(product = product, quantity = quantity)
        } else {
            // Товар уже есть — увеличиваем количество
            cartItems.map {
                if (it.product.id == product.id) {
                    it.copy(quantity = it.quantity + quantity)
                } else it
            }
        }
        saveCartToStorage()
    }

    fun updateCartItemQuantity(productId: String, newQuantity: Double) {
        cartItems = if (newQuantity <= 0.0) {
            // Количество стало 0 — удаляем из корзины
            cartItems.filterNot { it.product.id == productId }
        } else {
            // Обновляем количество
            cartItems.map {
                if (it.product.id == productId) {
                    it.copy(quantity = newQuantity)
                } else it
            }
        }
        saveCartToStorage()
    }

    fun removeFromCart(productId: String) {
        cartItems = cartItems.filterNot { it.product.id == productId }
        saveCartToStorage()
    }

    fun clearCart() {
        cartItems = emptyList()
        saveCartToStorage()
    }

    // Сохраняем корзину в SharedPreferences как JSON
    private fun saveCartToStorage() {
        val array = org.json.JSONArray()
        cartItems.forEach { item ->
            val product = item.product
            val obj = org.json.JSONObject()
            obj.put("id", product.id)
            obj.put("name", product.name)
            obj.put("category", product.category.name)
            obj.put("price", product.price)
            obj.put("unit", product.unit.name)
            obj.put("originCountry", product.originCountry ?: "")
            obj.put("imageUrl", product.imageUrl ?: "")
            obj.put("description", product.description ?: "")
            obj.put("isPopular", product.isPopular)
            obj.put("isNew", product.isNew)
            obj.put("inStock", product.inStock)
            obj.put("quantity", item.quantity)
            array.put(obj)
        }
        prefs.edit().putString(cartKey, array.toString()).apply()
    }

    // Восстанавливаем корзину при запуске приложения
    private fun loadCartFromStorage() {
        val raw = prefs.getString(cartKey, null) ?: return
        val array = runCatching { org.json.JSONArray(raw) }.getOrNull() ?: return
        val restored = mutableListOf<CartItem>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            val name = obj.optString("name")
            if (id.isBlank() || name.isBlank()) continue

            val category = runCatching {
                ProductCategory.valueOf(obj.optString("category"))
            }.getOrDefault(ProductCategory.OTHER)

            val unit = runCatching {
                UnitType.valueOf(obj.optString("unit"))
            }.getOrDefault(UnitType.KG)

            val product = Product(
                id = id,
                name = name,
                category = category,
                price = obj.optDouble("price", 0.0),
                unit = unit,
                originCountry = obj.optString("originCountry").ifBlank { null },
                imageUrl = obj.optString("imageUrl").ifBlank { null },
                description = obj.optString("description").ifBlank { null },
                isPopular = obj.optBoolean("isPopular", false),
                isNew = obj.optBoolean("isNew", false),
                inStock = obj.optBoolean("inStock", true)
            )
            val quantity = obj.optDouble("quantity", 0.0)
            if (quantity > 0.0) {
                restored.add(CartItem(product = product, quantity = quantity))
            }
        }

        if (restored.isNotEmpty()) {
            cartItems = restored
        }
    }

    // ---------- РАБОТА С ТОВАРАМИ В FIRESTORE ----------

    // Обновляем существующий товар — listener сам обновит список на экране
    fun updateProduct(updated: Product) {
        db.collection("products")
            .document(updated.id)
            .set(updated.toMap())
    }

    // Добавляем новый товар — тоже обновится через listener
    fun addProduct(newProduct: Product) {
        db.collection("products")
            .document(newProduct.id)
            .set(newProduct.toMap())
    }

    // Когда ViewModel уничтожается — снимаем подписку на Firestore
    override fun onCleared() {
        super.onCleared()
        productsListener?.remove()
    }
}