package com.example.ovoshebaza.data.repository

import com.example.ovoshebaza.Constants
import com.example.ovoshebaza.data.mappers.toDomain
import com.example.ovoshebaza.domain.model.Product
import com.example.ovoshebaza.domain.repository.ProductRepository
import com.example.ovoshebaza.toProduct
import com.google.firebase.firestore.FirebaseFirestore

class ProductRepositoryImpl : ProductRepository {

    // Загружаем товары из Firestore
    override fun loadProducts(
        onResult: (List<Product>) -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_PRODUCTS)
            .get()
            .addOnSuccessListener { snapshot ->
                val products = snapshot.documents.mapNotNull { it.toProduct() }
                // Возвращаем список товаров (пустой если ничего нет)
                onResult(products.map { it.toDomain() })
            }
            .addOnFailureListener { e ->
                // При ошибке сообщаем об этом и возвращаем пустой список
                onError(e.message ?: "Не удалось загрузить товары")
                onResult(emptyList())
            }
    }

    // Метод getProducts() удалён — sampleProducts больше не используются,
    // все товары хранятся в Firestore
}