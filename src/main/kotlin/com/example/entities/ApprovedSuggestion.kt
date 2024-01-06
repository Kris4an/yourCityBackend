package com.example.entities

import kotlinx.serialization.Serializable

@Serializable
data class ApprovedSuggestion(val id: Int, val suggestionId: Int, val moreInfo: String)