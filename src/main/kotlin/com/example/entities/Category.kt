package com.example.entities

import kotlinx.serialization.Serializable

@Serializable
data class Category(val id: Int, val name: String, val color: String)