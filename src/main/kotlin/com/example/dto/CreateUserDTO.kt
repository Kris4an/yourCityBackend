package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserDTO(val name: String, val email: String, val password: String, val role: String, val phone: String?, val schoolId: Int?){
    constructor(name: String, email: String, password: String, role: String) : this(name, email, password, role, null, null)
    constructor(name: String, email: String, password: String, role: String, phone: String) : this(name, email, password, role, phone, null)
    constructor(name: String, email: String, password: String, role: String, schoolId: Int) : this(name, email, password, role,null, schoolId)
}
