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

private fun validateCredentials(databaseUrl: String, email: String, password: String):UserIntIdPrincipal? {
    try{
        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
        val sql = "select id, email, password from users where email=?"
        val preparedStatement = connection!!.prepareStatement(sql)
        preparedStatement.setString(1, email)
        val result = preparedStatement.executeQuery()
        connection.close()
        if(result == null) return null
        result.next()
        val hashedUserPassword = result.getString("password")
        if(BCrypt.verifyer().verify(password.toCharArray(), hashedUserPassword).verified){
            return UserIntIdPrincipal(result.getInt("id"))
        }
        return null
    }catch (_:Exception){
        throw Exception("Invalid database url")
    }
}
fun Application.configureSecurity() {
    val secretEncryptKey = hex(environment.config.propertyOrNull("ktor.sessions.secretEncryptKey")?.getString() ?:throw Exception("Can't access environment config"))
    val secretSignKey =  hex(environment.config.propertyOrNull("ktor.sessions.secretSignKey")?.getString() ?:throw Exception("Can't access environment config"))
    val databaseUrl = environment.config.propertyOrNull("ktor.database.url")?.getString() ?:""
    install(Sessions) {
        cookie<UserSession>("user_session", SessionStorageDatabase()) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 2592000
            cookie.extensions["SameSite"] = "lax"
            transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretSignKey))
        }
    }
    install(Authentication) {
        form("auth-form") {
            userParamName = "email"
            passwordParamName = "password"
            validate { credentials ->
                validateCredentials(databaseUrl, credentials.name, credentials.password)
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
        session<UserSession>("auth-session") {
            validate { session ->
                session
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}