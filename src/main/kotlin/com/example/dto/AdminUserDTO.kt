package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminUserDTO(val userId: Int, val userName: String, val role: String, val email: String, val school: String?, val phone: String?)
