package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendOwnSuggestionDTO(
    val id: Int, val title: String, val content: String, val category: String,
    val postDate: String, val isAnon: Boolean, val status: String, val likes: Int
) {
    constructor(
        id: Int, title: String, content: String, category: String, postDate: String, isAnon: Int,
        status: String, likes: Int
    )
            : this(id, title, content, category, postDate, isAnon == 1, status, likes)
}
