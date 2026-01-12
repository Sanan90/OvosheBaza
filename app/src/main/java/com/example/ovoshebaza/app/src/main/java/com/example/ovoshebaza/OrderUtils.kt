package com.example.ovoshebaza

// ============== ЭКРАНЫ ==============

enum class PaymentMethod(val label: String) {
    CASH("Наличные при получении"),
    CARD("Картой при получении")
}

// Красиво показываем количество:
// 1.0 -> "1", 1.5 -> "1.5"
fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

// Собираем данные заказа
fun buildOrderMap(
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
    val items = cartItems.map { item ->
        mapOf(
            "id" to item.product.id,
            "name" to item.product.name,
            "quantity" to item.quantity,
            "unit" to item.product.unit.name,   // "KG" или "PIECE"
            "price" to item.product.price,
            "sum" to (item.product.price * item.quantity)
        )
    }

    val subtotal = cartItems.sumOf { it.product.price * it.quantity }
    val now = System.currentTimeMillis()

    return mapOf(
        "type" to "ORDER",
        "createdAt" to now,
        "customerName" to customerName,
        "customerPhone" to customerPhone,
        "customerAddress" to customerAddress,
        "comment" to comment,
        "paymentMethod" to paymentMethod.name,
        "deliveryFee" to deliveryFee,
        "discount" to discount,
        "subtotal" to subtotal,
        "total" to total,
        "status" to "RECEIVED",
        "statusUpdatedAt" to now,
        "items" to items
    )
}

fun buildSupportMap(
    question: String,
    phone: String
): Map<String, Any> {
    return mapOf(
        "type" to "SUPPORT",
        "createdAt" to System.currentTimeMillis(),
        "phone" to phone.trim(),
        "question" to question.trim()
    )
}

fun buildRequestMap(
    customerName: String,
    customerPhone: String,
    requestedProduct: String,
    requestedQuantity: String,
    comment: String
): Map<String, Any> {
    return mapOf(
        "type" to "REQUEST",
        "createdAt" to System.currentTimeMillis(),
        "customerName" to customerName.trim(),
        "customerPhone" to customerPhone.trim(),
        "requestedProduct" to requestedProduct.trim(),
        "requestedQuantity" to requestedQuantity.trim(),
        "comment" to comment.trim()
    )
}

fun buildRequestMessage(
    customerName: String,
    customerPhone: String,
    requestedProduct: String,
    requestedQuantity: String,
    comment: String
): String {
    return buildString {
        appendLine("📝 НОВАЯ ЗАЯВКА НА ТОВАР")
        appendLine()
        appendLine("Имя: $customerName")
        appendLine("Телефон: $customerPhone")
        appendLine()
        appendLine("Что нужно заказать:")
        appendLine(requestedProduct)
        appendLine()
        appendLine("Желаемое количество:")
        appendLine(requestedQuantity)
        if (comment.isNotBlank()) {
            appendLine()
            appendLine("Комментарий:")
            appendLine(comment)
        }
    }
}