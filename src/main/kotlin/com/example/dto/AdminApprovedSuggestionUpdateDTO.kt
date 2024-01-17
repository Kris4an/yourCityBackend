package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminApprovedSuggestionUpdateDTO(val id: Int, val moreInfo: String)
