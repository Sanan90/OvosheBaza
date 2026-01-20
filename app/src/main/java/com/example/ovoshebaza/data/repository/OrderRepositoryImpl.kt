package com.example.ovoshebaza.data.repository

import com.example.ovoshebaza.CartItem
import com.example.ovoshebaza.PaymentMethod
import com.example.ovoshebaza.buildOrderMap
import com.example.ovoshebaza.domain.repository.OrderRepository

class OrderRepositoryImpl : OrderRepository {
    override fun buildOrder(
        cartItems: List<CartItem>,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        comment: String,
        paymentMethod: PaymentMethod,
        deliveryFee: Double,
        discount: Double,
        total: Double
    ): Map<String, Any> {
        return buildOrderMap(
            cartItems = cartItems,
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            comment = comment,
            paymentMethod = paymentMethod,
            deliveryFee = deliveryFee,
            discount = discount,
            total = total
        )
    }
}