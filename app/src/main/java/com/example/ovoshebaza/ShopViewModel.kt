package com.example.ovoshebaza

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

import com.google.firebase.firestore.DocumentSnapshot


class ShopViewModel : ViewModel() {

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
    }

    fun removeFromCart(productId: String) {
        cartItems = cartItems.filterNot { it.product.id == productId }
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
