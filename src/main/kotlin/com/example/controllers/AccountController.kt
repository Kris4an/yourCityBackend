package com.example.controllers

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.dto.*
import com.example.entities.User
import com.example.security.LoginPrincipal
import com.example.security.UserSession
import com.example.security.validateCredentialsById
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.sql.Connection
import java.sql.DriverManager

fun getUser(databaseUrl: String, user: UserSession): User? {
    try {
        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
        val sql = "select * from users where id=?"
        val preparedStatement = connection!!.prepareStatement(sql)
        preparedStatement.setInt(1, user.userId)
        val result = preparedStatement.executeQuery()
        if (!result.next()) {
            return null
        }

        preparedStatement.close()
        connection.close()

        return User(
            result.getInt("id"),
            result.getString("name"),
            result.getString("password"),
            result.getString("email"),
            result.getString("phone"),
            result.getString("role"),
            result.getInt("schoolId"),
            result.getInt("isVerified")
        )

    } catch (e: Exception) {
        println(e.message)
        return null
    }
}

private fun hashPassword(password: String): String {
    return BCrypt.withDefaults().hashToString(12, password.toCharArray())
}

fun Route.accountRoutes() {
    route("/account") {
        val databaseUrl = environment?.config?.propertyOrNull("ktor.database.url")?.getString() ?: ""
        post("/create") {
            val body = call.receive<CreateUserDTO>()
            if (!Validator.validatePassword(body.password)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid password")
                return@post
            }
            if (!Validator.validateEmail(body.email)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid email")
                return@post
            }
            if (!Validator.validatePhone(body.phone)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid phone")
                return@post
            }
            if (!Validator.validateNonAdminRole(body.role)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid role")
                return@post
            }
            if (!Validator.validateName(body.name)) {
                call.respond(HttpStatusCode.BadRequest, "Invalid name")
                return@post
            }
            val bcryptHashPassword = hashPassword(body.password)

            try {
                val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                val sql = "insert into users (name, email, phone, role, schoolId, password ) values (?,?,?,?,?,?)"

                val preparedStatement = connection!!.prepareStatement(sql)
                preparedStatement.setString(1, body.name)
                preparedStatement.setString(2, body.email)
                preparedStatement.setString(3, body.phone)
                preparedStatement.setString(4, body.role)
                if (body.schoolId != null) preparedStatement.setInt(5, body.schoolId)
                else preparedStatement.setNull(5, 4)
                preparedStatement.setString(6, bcryptHashPassword)
                preparedStatement.executeUpdate()
                connection.close()

                call.respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
                throw e
            }
        }
        authenticate("auth-form") {
            post("/login") {
                val user = call.principal<LoginPrincipal>()
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                call.sessions.set(UserSession(userId = user.userId))
                call.respond(HttpStatusCode.OK)
            }
            delete("/delete") {
                val user = call.principal<UserSession>()
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@delete
                }
                val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                var preparedStatement = connection!!.prepareStatement("delete from likes where likedById = ?")
                preparedStatement.setInt(1, user.userId)
                preparedStatement.executeUpdate()

                preparedStatement = connection.prepareStatement("update suggestions set userId = null where userId = ?")
                preparedStatement.setInt(1, user.userId)
                preparedStatement.executeUpdate()

                preparedStatement = connection.prepareStatement("update bans set adminId = null where adminId = ?")
                preparedStatement.setInt(1, user.userId)
                preparedStatement.executeUpdate()

                preparedStatement = connection.prepareStatement("delete from users where id=?")
                preparedStatement.setInt(1, user.userId)
                call.sessions.clear<UserSession>()
                preparedStatement.executeUpdate()
                connection.close()
                call.respond(HttpStatusCode.OK)
            }
        }
        authenticate("auth-logout") {
            post("/logout") {
                call.sessions.clear<UserSession>()
                call.respond(HttpStatusCode.OK)
            }
        }
        authenticate("auth-session") {
            get {
                val user = call.sessions.get<UserSession>()
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                try {
                    val userEntity = getUser(databaseUrl, user)
                    if (userEntity == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
                    val res = SendUserDTO(
                        userEntity.name,
                        userEntity.email,
                        userEntity.phone,
                        userEntity.role,
                        getSchoolById(userEntity.schoolId, databaseUrl)?.name,
                        userEntity.isVerified
                    )
                    call.respond(HttpStatusCode.OK, res)
                } catch (e: Exception) {
                    throw e
                }
            }
            get("/email") {
                val user = call.sessions.get<UserSession>()
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                try {
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    val sql = "select email from users where id=?"
                    val preparedStatement = connection!!.prepareStatement(sql)
                    preparedStatement.setInt(1, user.userId)
                    val result = preparedStatement.executeQuery()
                    result.next()
                    call.respond(HttpStatusCode.OK, EmailDTO(result.getString("email")))
                    result.close()
                    preparedStatement.close()
                    connection.close()
                } catch (e: Exception) {
                    throw e
                }
            }
        }
        route("settings") {
            authenticate("auth-session") {
                put("/name") {
                    val user = call.sessions.get<UserSession>()
                    val body = call.receive<NameDTO>()
                    if (user == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@put
                    }
                    if (!Validator.validateName(body.name)) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid name")
                        return@put
                    }
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val sql = "update users set name = ? where id = ?"
                        val preparedStatement = connection!!.prepareStatement(sql)
                        preparedStatement.setString(1, body.name)
                        preparedStatement.setInt(2, user.userId)
                        val res = preparedStatement.executeUpdate()
                        if (res == 0) {
                            call.respond(HttpStatusCode.NotFound)
                            return@put
                        }
                        call.respond(HttpStatusCode.OK)
                        preparedStatement.close()
                        connection.close()
                    } catch (e: Exception) {
                        throw e
                    }
                }
                put("/phone") {
                    val user = call.sessions.get<UserSession>()
                    val body = call.receive<PhoneDTO>()
                    if (user == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@put
                    }
                    if (!Validator.validatePhone(body.phone)) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid phone")
                        return@put
                    }
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val sql = "update users set phone = ? where id = ?"
                        val preparedStatement = connection!!.prepareStatement(sql)
                        preparedStatement.setString(1, body.phone)
                        preparedStatement.setInt(2, user.userId)
                        val res = preparedStatement.executeUpdate()
                        if (res == 0) {
                            call.respond(HttpStatusCode.NotFound)
                            return@put
                        }
                        call.respond(HttpStatusCode.OK)
                        preparedStatement.close()
                        connection.close()
                    } catch (e: Exception) {
                        throw e
                    }
                }
                put("/role") {
                    val body = call.receive<ChangeRoleDTO>()
                    val user = call.sessions.get<UserSession>()
                    if (user == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@put
                    }
                    if (!Validator.validateNonAdminRole(body.role)) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid role")
                        return@put
                    }
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        var preparedStatement = connection!!.prepareStatement("select * from users where id = ?")
                        preparedStatement.setInt(1, user.userId)
                        val resCheckRole = preparedStatement.executeQuery()
                        if (resCheckRole.next()) {
                            if (resCheckRole.getString("role") == "admin") {
                                call.respond(HttpStatusCode.Conflict, "Could not remove admin role")
                            }
                        } else {
                            call.respond(HttpStatusCode(400, "Error"))
                            return@put
                        }

                        preparedStatement =
                            connection.prepareStatement("update users set role = ?, schoolId = ? where id = ?")
                        preparedStatement.setString(1, body.role)
                        when (body.role) {
                            "student" -> if (body.schoolId != null) preparedStatement.setInt(
                                2,
                                body.schoolId
                            ) else preparedStatement.setNull(2, 4)
                            "citizen" -> preparedStatement.setNull(2, 4)
                        }
                        preparedStatement.setInt(3, user.userId)
                        val res = preparedStatement.executeUpdate()
                        if (res == 0) {
                            call.respond(HttpStatusCode.NotFound)
                            return@put
                        }
                        call.respond(HttpStatusCode.OK)
                        preparedStatement.close()
                        connection.close()
                    } catch (e: Exception) {
                        throw e
                    }
                }
                put("/school") {
                    val body = call.receive<UpdateSchoolDTO>()
                    val user = call.sessions.get<UserSession>()
                    if (user == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@put
                    }
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val sql = "update users set schoolId = ? where id = ? AND role!='citizen' "
                        val preparedStatement = connection!!.prepareStatement(sql)
                        if(body.schoolId != null) preparedStatement.setInt(1, body.schoolId)
                        else preparedStatement.setNull(1, 4)
                        preparedStatement.setInt(2, user.userId)
                        preparedStatement.executeUpdate()

                        connection.close()
                        preparedStatement.close()
                        call.respond(HttpStatusCode.OK)
                    } catch (e: Exception) {
                        println(e)
                        call.respond(HttpStatusCode(400,"Error"))
                    }
                }
                put("/password") {
                    val user = call.sessions.get<UserSession>()
                    if (user == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@put
                    }
                    val body = call.receive<ChangePasswordDTO>()
                    if (!validateCredentialsById(databaseUrl, user.userId, body.oldPassword)) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@put
                    }
                    val hashedPassword = hashPassword(body.newPassword)
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val sql = "update users set password = ? where id = ?"
                        val preparedStatement = connection!!.prepareStatement(sql)
                        preparedStatement.setString(1, hashedPassword)
                        preparedStatement.setInt(2, user.userId)
                        val res = preparedStatement.executeUpdate()
                        if (res == 0) {
                            call.respond(HttpStatusCode.NotFound)
                            return@put
                        }
                        call.respond(HttpStatusCode.OK)
                        preparedStatement.close()
                        connection.close()
                    } catch (e: Exception) {
                        throw e
                    }
                }
            }
        }

    }

}
