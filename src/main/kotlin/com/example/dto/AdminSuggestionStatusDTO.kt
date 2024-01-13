package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminSuggestionStatusDTO(val suggestionId: Int, val status: String)
