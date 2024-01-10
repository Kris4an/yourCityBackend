package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateSuggestionDTO(val title: String, val content: String, val categoryId: Int, val isAnon: Boolean)
