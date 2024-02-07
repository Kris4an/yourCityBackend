package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendSuggestionDTO(val id: Int, val user: PublicUserDTO?, val title: String, val content: String,
                             val category: String, val isAnon: Boolean, val likes: Int, val postDate: String,
                             val isLikedByUser: Boolean?)
