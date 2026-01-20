package com.example.ovoshebaza.domain.model

data class OrderItem(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val sum: Double
)

data class OrderSummary(
    val orderId: String,
    val createdAt: Long,
    val total: Double,
    val itemsCount: Int,
    val channel: String,
    val status: String,
    val items: List<OrderItem>
)