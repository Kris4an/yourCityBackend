package com.example.entities

import kotlinx.serialization.Serializable

@Serializable
data class Ban(val id: Int, val userId: Int, val banDate: String, val reason: String, val duration: Int, val moderatorId: Int)