package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminDeleteCategoryDTO(val id: Int, val newCategoryId: Int)
