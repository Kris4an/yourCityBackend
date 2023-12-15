package com.example.entities

import kotlinx.serialization.Serializable

@Serializable
data class Like(val id: Int, val siggestionId: Int, val likedById: Int, val date: String)