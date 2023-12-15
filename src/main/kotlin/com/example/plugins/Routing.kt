package com.example.plugins

import com.example.controllers.accountRoutes
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

        get("/db") {
            val url = "jdbc:mariadb://localhost:3306/yourcitydb"//System.getenv("DATABASE_URL")//environment?.config?.property("ktor.database.url")?.getString()
            val connection: Connection? = DriverManager.getConnection(url, "root", "")
            val statement: Statement = connection!!.createStatement()

            val sql = "select * from categories"

            val result: ResultSet = statement.executeQuery(sql)
            val res = mutableListOf<Category>()
            while(result.next()){
                val category = Category(result.getInt("id"), result.getString("name"), result.getString("color"))
                res.add(category)
            }

            call.respond(res)

            result.close()
            statement.close()
        }
    }
}
