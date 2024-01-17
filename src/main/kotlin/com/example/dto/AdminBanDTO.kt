package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminBanDTO(val userId: Int, val reason: String, val banLength: Int)
