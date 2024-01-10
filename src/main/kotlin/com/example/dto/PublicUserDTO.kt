package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class PublicUserDTO(val name: String, val role: String, val school: String?)
