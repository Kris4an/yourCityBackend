package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminSendBanDTO(val startDate: String, val banLength: Int, val reason: String, val adminName: String?,
                           val adminEmail: String?)
