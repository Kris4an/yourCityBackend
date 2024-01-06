package com.example.plugins

import com.example.controllers.accountRoutes
import com.example.controllers.schoolRoutes
import com.example.controllers.suggestionRoutes
import com.example.entities.Category
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement


fun Application.configureRouting() {
    routing {
        accountRoutes()
        schoolRoutes()
        suggestionRoutes()
    }
}
