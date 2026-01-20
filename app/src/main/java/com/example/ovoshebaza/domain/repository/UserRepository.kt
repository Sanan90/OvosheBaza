package com.example.ovoshebaza.domain.repository

import com.example.ovoshebaza.domain.model.OrderSummary
import com.example.ovoshebaza.domain.model.UserProfile

interface UserRepository {
    fun loadUserProfile(
        onResult: (UserProfile?) -> Unit,
        onError: (String) -> Unit = {}
    )

    fun loadUserOrders(
        limit: Int = 10,
        onResult: (List<OrderSummary>) -> Unit,
        onError: (String) -> Unit = {}
    )
}