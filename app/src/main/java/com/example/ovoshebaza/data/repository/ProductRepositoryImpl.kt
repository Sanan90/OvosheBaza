package com.example.ovoshebaza.data.repository

import com.example.ovoshebaza.Constants
import com.example.ovoshebaza.data.mappers.toDomain
import com.example.ovoshebaza.domain.model.Product
import com.example.ovoshebaza.domain.repository.ProductRepository
import com.example.ovoshebaza.sampleProducts
import com.example.ovoshebaza.toProduct
import com.google.firebase.firestore.FirebaseFirestore

class ProductRepositoryImpl : ProductRepository {
    override fun getProducts(): List<Product> = sampleProducts.map { it.toDomain() }

    override fun loadProducts(
        onResult: (List<Product>) -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection(Constants.COLLECTION_PRODUCTS)
            .get()
            .addOnSuccessListener { snapshot ->
                val products = snapshot.documents.mapNotNull { it.toProduct() }
                if (products.isEmpty()) {
                    onResult(getProducts())
                } else {
                    onResult(products.map { it.toDomain() })
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Не удалось загрузить товары")
                onResult(getProducts())
            }
    }
}