package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendApprovedSuggestionDTO(val id: Int, val suggestion: AdminSuggestionDTO, val moreInfo: String, val date: String)
