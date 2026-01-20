package com.example.ovoshebaza.ui.catalog

import androidx.lifecycle.ViewModel
import com.example.ovoshebaza.data.repository.ProductRepositoryImpl
import com.example.ovoshebaza.domain.model.Product
import com.example.ovoshebaza.domain.repository.ProductRepository
import com.example.ovoshebaza.sampleProducts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CatalogViewModel(
    private val repository: ProductRepository = ProductRepositoryImpl()
) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(sampleProducts)
    val products: StateFlow<List<Product>> = _products

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadProducts()
    }

    fun loadProducts() {
        _isLoading.value = true
        _errorMessage.value = null
        repository.loadProducts(
            onResult = { items ->
                _products.value = items
                _isLoading.value = false
            },
            onError = { message ->
                _errorMessage.value = message
                _isLoading.value = false
            }
        )
    }
}