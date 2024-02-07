package com.example.security

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import java.sql.Connection
import java.sql.DriverManager
fun validateCredentialsById(databaseUrl: String, id: Int, password: String):Boolean{
    return validateCredentials(databaseUrl, id.toString(), password, false) != null
}
private fun validateCredentialsByEmail(databaseUrl: String, email: String, password: String):LoginPrincipal? {
    return validateCredentials(databaseUrl, email, password, true)
}

private fun validateCredentials(databaseUrl: String, searchParam: String, password: String, byEmail: Boolean):LoginPrincipal? {
    try{
        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
        val sql = if(byEmail) "select id, password, role from users where email=?"
        else "select id, password, role from users where id = ?"
        val preparedStatement = connection!!.prepareStatement(sql)
        if(byEmail) preparedStatement.setString(1, searchParam)
        else preparedStatement.setInt(1, searchParam.toInt())
        val result = preparedStatement.executeQuery()
        connection.close()
        if(result == null) return null
        if(!result.next()) return null
        val hashedUserPassword = result.getString("password")
        if(BCrypt.verifyer().verify(password.toCharArray(), hashedUserPassword).verified){
            return LoginPrincipal(result.getInt("id"))
        }
        return null
    }catch (e:Exception){
        println(e.message)
        return null
    }
}
private fun validateAdmin(databaseUrl: String, userId: Int): Boolean {
    try {
        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
        val sql = "select role from users where id = ?"
        val preparedStatement = connection!!.prepareStatement(sql)
        preparedStatement.setInt(1, userId)
        val result = preparedStatement.executeQuery()
        preparedStatement.close()
        connection.close()
        if(!result.next()) return false
        return result.getString("role") == "admin"
    }catch (e:Exception){
        println(e.message)
        return false
    }
}
private fun validateUserBanStatus(databaseUrl: String, user: UserSession): UserSession?{
    try {
        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
        val sql = "select Count(*) as banCount from bans where userId = ? AND endDate>=CURDATE()"
        val preparedStatement = connection!!.prepareStatement(sql)
        preparedStatement.setInt(1,user.userId)
        val resultSet = preparedStatement.executeQuery()
        if(resultSet.next()){
            return if(resultSet.getInt(1) <= 0) user
            else null
        }
        return null
    }catch (e:Exception){
        println(e.message)
        return null
    }
}
fun Application.configureSecurity() {
    val databaseUrl = environment.config.propertyOrNull("ktor.database.url")?.getString() ?:""
    install(Sessions) {
        cookie<UserSession>("user_session", SessionStorageDatabase()) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 2592000
            cookie.extensions["SameSite"] = "lax"
        }
    }
    install(Authentication) {
        form("auth-form") {
            userParamName = "email"
            passwordParamName = "password"
            validate { credentials ->
                validateCredentialsByEmail(databaseUrl, credentials.name, credentials.password)
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
        session<UserSession>("auth-session") {
            validate { session ->
                    validateUserBanStatus(databaseUrl, session)
            }
            challenge {
                call.respond(HttpStatusCode(401, "Banned"))
            }
        }
        session<UserSession>("auth-logout") {
            validate { session ->
                session
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
        session<UserSession>("auth-admin") {
            validate { session ->
                if(validateAdmin(databaseUrl, session.userId)) session
                else null
            }
            challenge{
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}