package com.example.ovoshebaza.domain.model

data class UserProfile(
    val name: String,
    val phone: String,
    val addresses: List<String>,
    val lastAddress: String,
    val hasPassword: Boolean
)