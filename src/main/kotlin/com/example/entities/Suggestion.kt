package com.example.entities

import kotlinx.serialization.Serializable

@Serializable
data class Suggestion(val id: Int, val userId: Int, val title: String, val content: String, val categoryId: Int, val postDate: String, val isAnon: Int, val status: String)