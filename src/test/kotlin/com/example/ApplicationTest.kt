package com.example

import com.example.dto.CreateUserDTO
import com.example.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.*

class ApplicationTest {
    private fun truncateTable(table: String) {
        val databaseUrl = System.getenv("DATABASE_URL")
        val tablesWithoutConstraint = arrayOf("bans","approvedSuggestions","likes","sessions")
        val tablesWithConstraint = arrayOf("suggestions","categories","users","schools")
        try{
            val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
            val statement = connection!!.createStatement()
            if(tablesWithoutConstraint.contains(table)){
                statement.executeQuery("truncate table $table;")
            }
            if(tablesWithConstraint.contains(table)){
                statement.executeQuery("delete from $table where id > 1;")
            }
        }catch (e: Exception){
            throw e
        }
    }
    @Test
    fun testCreateAccount() = testApplication {
        truncateTable("users")
        environment {
            config = ApplicationConfig("application-test.conf")
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        application {
            configureRouting()
        }
        val responseCitizen = client.post("/account/create"){
            contentType(ContentType.Application.Json)
            setBody(
                //CreateUserDTO("Тест Тестов","test@example.com","Test1Password","student","0888654321",1)
                CreateUserDTO("Гражданин Тестов","citizen@test.com","Test1Password","citizen","0888654321")
            )
        }
        val responseStudent = client.post("/account/create"){
            contentType(ContentType.Application.Json)
            setBody(
                CreateUserDTO("Ученик Тестов","student@test.com","Test1Password","student","0888654321",1)
            )
        }
        assertEquals(HttpStatusCode.Created, responseCitizen.status)
        assertEquals(HttpStatusCode.Created, responseStudent.status)
    }
    @Test
    fun loginAndGetUser() = testApplication {
        truncateTable("users")
        environment {
            config = ApplicationConfig("application-test.conf")
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
            install(HttpCookies)
        }
        application {
            configureRouting()
        }
        val responseLogin = client.post("/account/login"){
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(listOf("email" to "permanent@test.com", "password" to "Test1Password").formUrlEncode())
        }
        assertEquals(HttpStatusCode.OK, responseLogin.status)
        val responseGetUser = client.get("/account")
        assertEquals(HttpStatusCode.OK, responseGetUser.status)
    }
}
