package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginUserDTO(val email: String, val password: String)
