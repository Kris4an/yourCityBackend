package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendSuggestionDTO(val id: Int, val user: PublicUserDTO?, val title: String, val content: String, val category: String, val isAnon: Boolean){
    constructor(id: Int, user: PublicUserDTO?, title: String, content: String, category: String, isAnon: Int)
            :this (id, user, title, content, category, isAnon==1)
}
