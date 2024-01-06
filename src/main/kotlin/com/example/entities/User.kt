package com.example.entities

import kotlinx.serialization.*

@Serializable
data class User(val id: Int, val name: String, val password: String, val email: String, val phone: String?, val role: String, val schoolId: Int, val isVerified: Int)