package com.example.security

import io.ktor.server.auth.*

data class UserSession(val userId: Int) : Principal
