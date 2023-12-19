package com.example.security

import io.ktor.server.auth.*

data class UserIntIdPrincipal(val id: Int) : Principal
