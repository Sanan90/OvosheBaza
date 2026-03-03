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
    private val prefs =
        application.getSharedPreferences("cart_storage", Context.MODE_PRIVATE)
    private val cartKey = "cart_items"

    // Firestore
    private val db = FirebaseFirestore.getInstance()

    // Подписка на коллекцию products
    private var productsListener: ListenerRegistration? = null

    // Все товары (живут в Firebase)
    var products by mutableStateOf<List<Product>>(emptyList())
        private set

    // Корзина всё ещё локальная (можно потом тоже вынести)
    var cartItems by mutableStateOf(listOf<CartItem>())
        private set

    init {
        // при создании VM начинаем слушать Firestore
        observeProducts()
        loadCartFromStorage()
    }

    private fun observeProducts() {
        // если вдруг уже была подписка — снимаем
        productsListener?.remove()

        productsListener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Можно добавить лог/обработку, пока просто игнорируем
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toProduct()
                    }

                    // Если коллекция пустая — можно один раз залить стартовые товары
                    if (list.isEmpty()) {
                        seedInitialProducts()
                    } else {
                        products = list
                    }
                }
            }
    }

    // Первый раз заполняем Firestore начальными товарами
    private fun seedInitialProducts() {
        sampleProducts.forEach { product ->
            db.collection("products")
                .document(product.id)
                .set(product.toMap())
        }
        // После этого сработает listener и products обновится
    }

    // ---------- КОРЗИНА (локально) ----------

    fun addToCart(product: Product, quantity: Double) {
        if (quantity <= 0.0) return

        val existing = cartItems.find { it.product.id == product.id }

        cartItems = if (existing == null) {
            cartItems + CartItem(product = product, quantity = quantity)
        } else {
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
            cartItems.filterNot { it.product.id == productId }
        } else {
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

    fun updateProduct(updated: Product) {
        db.collection("products")
            .document(updated.id)
            .set(updated.toMap())
        // listener сам обновит products
    }

    fun addProduct(newProduct: Product) {
        db.collection("products")
            .document(newProduct.id)
            .set(newProduct.toMap())
        // тоже обновится через listener
    }

    override fun onCleared() {
        super.onCleared()
        productsListener?.remove()
    }
}
