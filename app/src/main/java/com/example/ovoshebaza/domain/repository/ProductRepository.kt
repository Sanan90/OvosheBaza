package com.example.ovoshebaza.domain.repository

import com.example.ovoshebaza.domain.model.Product

interface ProductRepository {

    // Загружает товары из Firestore
    // onResult — вызывается когда товары загружены
    // onError — вызывается если произошла ошибка
    fun loadProducts(
        onResult: (List<Product>) -> Unit,
        onError: (String) -> Unit = {}
    )
}