package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminSuggestionDTO(val suggestionId: Int, val title: String, val content: String, val postDate: String,
                              val isAnon: Boolean, val status: String, val category: String, val user: AdminUserDTO?,
                              val likes: Int){
    constructor(suggestionId: Int, title: String, content: String, postDate: String, isAnon: Int, status: String,
        category: String, user: AdminUserDTO?, likes: Int) : this(suggestionId, title, content, postDate, isAnon==1,
            status, category, user, likes)
}
