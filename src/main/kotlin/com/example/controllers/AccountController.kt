package com.example.controllers

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.dto.UserDTO
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection
import java.sql.DriverManager


fun Route.accountRoutes() {
    route("/account") {
        post("/create") {
            val account = call.receive<UserDTO>()
            if (!Validator.validatePassword(account.password)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid password")
                return@post
            }
            if (!Validator.validateEmail(account.email)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid email")
                return@post
            }
            if (!Validator.validatePhone(account.phone)){
                call.respond(HttpStatusCode.BadRequest, "Invalid phone")
                return@post
            }
            if (!Validator.validateNonAdminRole(account.role)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid role")
                return@post
            }
            if(account.role == "student" && account.schoolId == null){
                call.respond(HttpStatusCode.BadRequest, "School id can't be null when role is student")
                return@post
            }
            val bcryptHashPassword = BCrypt.withDefaults().hashToString(12, account.password.toCharArray())

            try {
                val url =
                    "jdbc:mariadb://localhost:3306/yourcitydb"//environment?.config?.property("ktor.database.url")?.getString()
                val connection: Connection? = DriverManager.getConnection(url, "root", "")
                val sql = "insert into users (name, email, phone, role, schoolId, password ) values (?,?,?,?,?,?)"

                val preparedStatement = connection!!.prepareStatement(sql)
                preparedStatement.setString(1, account.name)
                preparedStatement.setString(2, account.email)
                preparedStatement.setString(3, account.phone)
                preparedStatement.setString(4, account.role)
                if (account.schoolId != null) preparedStatement.setInt(5, account.schoolId)
                else preparedStatement.setNull(5, 4)
                preparedStatement.setString(6, bcryptHashPassword)

                val rowsAffected = preparedStatement.executeUpdate()
                connection.close()
                call.respond(HttpStatusCode.Created)
            }catch (e: Exception){
                call.respond(HttpStatusCode.InternalServerError)
                //call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
            }

        }
    }

}
