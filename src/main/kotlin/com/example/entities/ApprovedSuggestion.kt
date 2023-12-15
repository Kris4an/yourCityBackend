package com.example.entities

import kotlinx.serialization.Serializable

@Serializable
data class ApprovedSuggestion(val id: Int, val suggestionid: Int, val moreInfo: String)