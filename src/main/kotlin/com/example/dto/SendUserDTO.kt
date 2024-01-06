package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendUserDTO(val name: String, val email: String, val phone: String?, val role: String, val school: String?, val isVerified: Boolean){
    constructor(name: String, email: String, phone: String?, role: String, school: String?, isVerified: Int)
            : this(name, email, phone, role, school,isVerified==1)
}
