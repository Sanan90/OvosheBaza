package com.example.ovoshebaza.data.repository

import com.example.ovoshebaza.UserProfile as AppUserProfile
import com.example.ovoshebaza.OrderSummary as AppOrderSummary
import com.example.ovoshebaza.domain.model.OrderSummary as DomainOrderSummary
import com.example.ovoshebaza.domain.model.UserProfile as DomainUserProfile
import com.example.ovoshebaza.domain.repository.UserRepository
import com.example.ovoshebaza.data.mappers.toDomain
import com.example.ovoshebaza.loadUserOrders
import com.example.ovoshebaza.loadUserProfile

class UserRepositoryImpl : UserRepository {
    override fun loadUserProfile(
        onResult: (DomainUserProfile?) -> Unit,
        onError: (String) -> Unit
    ) {
        loadUserProfile(
            onResult = { profile: AppUserProfile? -> onResult(profile?.toDomain()) },
            onError = onError
        )
    }

    override fun loadUserOrders(
        limit: Int,
        onResult: (List<DomainOrderSummary>) -> Unit,
        onError: (String) -> Unit
    ) {
        loadUserOrders(
            limit = limit,
            onResult = { orders: List<AppOrderSummary> ->
                onResult(orders.map { it.toDomain() })
            },
            onError = onError
        )
    }
}