package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangeRoleDTO(val role: String, val schoolId: Int?)
