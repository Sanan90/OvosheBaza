package com.example.ovoshebaza.data.mappers

import com.example.ovoshebaza.OrderItem
import com.example.ovoshebaza.OrderSummary
import com.example.ovoshebaza.Product
import com.example.ovoshebaza.UserProfile
import com.example.ovoshebaza.domain.model.OrderItem as DomainOrderItem
import com.example.ovoshebaza.domain.model.OrderSummary as DomainOrderSummary
import com.example.ovoshebaza.domain.model.Product as DomainProduct
import com.example.ovoshebaza.domain.model.UserProfile as DomainUserProfile

fun Product.toDomain(): DomainProduct = this

fun UserProfile.toDomain(): DomainUserProfile = DomainUserProfile(
    name = name,
    phone = phone,
    addresses = addresses,
    lastAddress = lastAddress,
    hasPassword = hasPassword
)

fun OrderItem.toDomain(): DomainOrderItem = DomainOrderItem(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit,
    price = price,
    sum = sum
)

fun OrderSummary.toDomain(): DomainOrderSummary = DomainOrderSummary(
    orderId = orderId,
    createdAt = createdAt,
    total = total,
    itemsCount = itemsCount,
    channel = channel,
    status = status,
    items = items.map { it.toDomain() }
)