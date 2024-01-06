package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordDTO(val oldPassword: String, val newPassword: String)
