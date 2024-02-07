package com.example.security

import io.ktor.server.auth.*

data class LoginPrincipal(val userId: Int): Principal
