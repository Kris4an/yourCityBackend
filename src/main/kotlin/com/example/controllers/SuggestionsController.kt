package com.example.controllers

import com.example.dto.CreateSuggestionDTO
import com.example.dto.PublicUserDTO
import com.example.dto.SendSuggestionDTO
import com.example.security.UserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate

fun Route.suggestionRoutes() {
    route("/suggestion"){
        val databaseUrl = environment?.config?.propertyOrNull("ktor.database.url")?.getString() ?:""
        authenticate("auth-session") {
            post("/new"){
                val user = call.sessions.get<UserSession>()
                if(user == null){
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                val suggestion = call.receive<CreateSuggestionDTO>()
                if(Validator.validateCreateSuggestionDTO(suggestion)){
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val postDate = LocalDate.now()
                val status = "waiting"
                try{
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    val sql = "insert into suggestions (userId, title, content, categoryId, postDate, isAnon, status) " +
                            "values (?,?,?,?,?,?,?)"
                    val preparedStatement = connection!!.prepareStatement(sql)
                    preparedStatement.setInt(1, user.userId)
                    preparedStatement.setString(2, suggestion.title)
                    preparedStatement.setString(3,suggestion.content)
                    preparedStatement.setInt(4, suggestion.categoryId)
                    preparedStatement.setString(5, postDate.toString())
                    preparedStatement.setInt(6, if(suggestion.isAnon) 1; else 0)
                    preparedStatement.setString(7, status)

                    preparedStatement.executeUpdate()
                    preparedStatement.close()
                    connection.close()

                    call.respond(HttpStatusCode.Created)
                }catch (e: Exception){
                    println(e.message)
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
        get("/all"){
            val page = call.request.queryParameters["page"]?.toInt()
            if(page == null || page < 1) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            try{
                val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                val sql = "select s.id, title, content, postDate, isAnon, " +
                        "categories.name as categoryName, u.name as userName, role, schools.name as schoolName from " +
                        "suggestions as s left join users as u on s.userId = u.id join categories on " +
                        "s.categoryId = categories.id left join schools on u.schoolId = schools.id " +
                        "where status='accepted' limit ? offset ?"
                //TODO return likes count
                val preparedStatement = connection!!.prepareStatement(sql)

                preparedStatement.setInt(1,page*10)
                preparedStatement.setInt(2,page*10-10)
                val resultSet = preparedStatement.executeQuery()

                val res = mutableListOf<SendSuggestionDTO>()
                while(resultSet.next()){
                    val user: PublicUserDTO?
                    var isAnon:Boolean
                    if(resultSet.getString("userName").isNullOrEmpty()){
                        user = null
                        isAnon = false
                    }
                    else {
                        if(resultSet.getInt("isAnon")==0){
                            user = PublicUserDTO(
                                resultSet.getString("userName"),
                                resultSet.getString("role"),
                                resultSet.getString("schoolName")
                            )
                            isAnon = false
                        }
                        else {
                            user = null
                            isAnon = true
                        }
                    }
                    res.add(SendSuggestionDTO(
                        resultSet.getInt("id"),
                        user,
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        resultSet.getString("categoryName"),
                        isAnon
                    ))
                }
                resultSet.close()
                preparedStatement.close()
                connection.close()


                call.respond(HttpStatusCode.OK, res)
            }catch (e: Exception){
                println(e.message)
                call.respond(HttpStatusCode.BadRequest)
            }
        }

    }
}
