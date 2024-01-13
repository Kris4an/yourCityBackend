package com.example.plugins

import com.example.controllers.adminRoutes
import com.example.controllers.accountRoutes
import com.example.controllers.schoolRoutes
import com.example.controllers.suggestionRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    routing {
        accountRoutes()
        schoolRoutes()
        suggestionRoutes()
        adminRoutes()
    }
}
