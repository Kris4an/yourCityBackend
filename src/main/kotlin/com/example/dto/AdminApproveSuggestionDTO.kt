package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminApproveSuggestionDTO(val suggestionId: Int, val moreInfo: String)
