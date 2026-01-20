package com.example.ovoshebaza.domain.repository

import com.example.ovoshebaza.domain.model.Product

interface ProductRepository {
    fun getProducts(): List<Product>

    fun loadProducts(
        onResult: (List<Product>) -> Unit,
        onError: (String) -> Unit = {}
    )
}