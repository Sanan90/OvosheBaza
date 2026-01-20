package com.example.ovoshebaza.domain.repository

import com.example.ovoshebaza.CartItem
import com.example.ovoshebaza.PaymentMethod

interface OrderRepository {
    fun buildOrder(
        cartItems: List<CartItem>,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        comment: String,
        paymentMethod: PaymentMethod,
        deliveryFee: Double,
        discount: Double,
        total: Double
    ): Map<String, Any>
}