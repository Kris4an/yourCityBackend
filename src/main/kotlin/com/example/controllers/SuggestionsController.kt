package com.example.controllers

import com.example.dto.*
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
    route("/suggestion") {
        val databaseUrl = environment?.config?.propertyOrNull("ktor.database.url")?.getString() ?: ""
        authenticate("auth-session") {
            post("/new") {
                val user = call.sessions.get<UserSession>()
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                val suggestion = call.receive<CreateSuggestionDTO>()
                if (!Validator.validateCreateSuggestionDTO(suggestion)) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid title or content length")
                    return@post
                }
                val postDate = LocalDate.now()
                val status = "waiting"
                try {
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    val sql =
                        "insert into suggestions (userId, title, content, categoryId, postDate, isAnon, status) " +
                                "values (?,?,?,?,?,?,?)"
                    val preparedStatement = connection!!.prepareStatement(sql)
                    preparedStatement.setInt(1, user.userId)
                    preparedStatement.setString(2, suggestion.title)
                    preparedStatement.setString(3, suggestion.content)
                    preparedStatement.setInt(4, suggestion.categoryId)
                    preparedStatement.setString(5, postDate.toString())
                    preparedStatement.setInt(6, if (suggestion.isAnon) 1; else 0)
                    preparedStatement.setString(7, status)

                    preparedStatement.executeUpdate()
                    preparedStatement.close()
                    connection.close()

                    call.respond(HttpStatusCode.Created)
                } catch (e: Exception) {
                    println(e.message)
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            delete("/{suggestionId}") {
                val suggestionId = call.parameters["suggestionId"]?.toInt()
                val user = call.sessions.get<UserSession>()
                if (suggestionId == null || suggestionId < 0 || user == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
                try {
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    var preparedStatement = connection!!.prepareStatement(
                        "select COUNT(*) from approvedsuggestions where suggestionId = ?"
                    )
                    preparedStatement.setInt(1, suggestionId)
                    val resCheck = preparedStatement.executeQuery()
                    if (resCheck.next()) {
                        if (resCheck.getInt(1) > 0) {
                            call.respond(HttpStatusCode(403, "Could not delete suggestion because it's approved"))
                            return@delete
                        } else {
                            call.respond(HttpStatusCode(400, "Error"))
                            return@delete
                        }
                    }
                    preparedStatement = connection.prepareStatement(
                        "delete from suggestions where id = ? AND userId = ?"
                    )
                    preparedStatement.setInt(1, suggestionId)
                    preparedStatement.setInt(2, user.userId)

                    val result = preparedStatement.executeUpdate()
                    if (result < 1) {
                        call.respond(HttpStatusCode.NotFound)
                    } else call.respond(HttpStatusCode.OK)
                    preparedStatement.close()
                    connection.close()
                } catch (e: Exception) {
                    println(e.message)
                    call.respond(HttpStatusCode(400, "Error"))
                }
            }
            get("/my") {
                val user = call.sessions.get<UserSession>()
                val page = call.request.queryParameters["page"]?.toInt()
                if (page == null || page < 1 || user == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                try {
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    val preparedStatement = connection!!.prepareStatement(
                        "select s.id, title, content, c.name," +
                                " postDate, isAnon, status, (select Count(*) from likes where suggestionId = s.id)" +
                                " as likes from suggestions s join categories c on s.categoryId = c.id " +
                                "where userId = ? limit ? offset ?"
                    )
                    preparedStatement.setInt(1, user.userId)
                    preparedStatement.setInt(2, page * 10)
                    preparedStatement.setInt(3, page * 10 - 10)
                    val resultSet = preparedStatement.executeQuery()

                    val result = mutableListOf<SendOwnSuggestionDTO>()
                    while (resultSet.next()) {
                        result.add(
                            SendOwnSuggestionDTO(
                                resultSet.getInt(1),
                                resultSet.getString(2),
                                resultSet.getString(3),
                                resultSet.getString(4),
                                resultSet.getString(5),
                                resultSet.getInt(6),
                                resultSet.getString(7),
                                resultSet.getInt(8)
                            )
                        )
                    }
                    resultSet.close()
                    preparedStatement.close()
                    connection.close()

                    call.respond(HttpStatusCode.OK, result)
                } catch (e: Exception) {
                    println(e.message)
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            post("/like/{suggestionId}") {
                val suggestionId = call.parameters["suggestionId"]?.toInt()
                val user = call.sessions.get<UserSession>()
                if (suggestionId == null || suggestionId < 0 || user == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                try {
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    var preparedStatement = connection!!.prepareStatement(
                        "select Count(*) from likes where suggestionId = ? AND likedById = ?"
                    )
                    preparedStatement.setInt(1, suggestionId)
                    preparedStatement.setInt(2, user.userId)
                    val resultSet = preparedStatement.executeQuery()
                    if (resultSet.next()) {
                        if (resultSet.getInt(1) > 0) {
                            call.respond(HttpStatusCode(406, "Suggestion is already liked"))
                            return@post
                        }
                    }
                    preparedStatement = connection.prepareStatement(
                        "insert into likes " +
                                "(suggestionId, likedById, likeDate) values(?,?,?)"
                    )
                    preparedStatement.setInt(1, suggestionId)
                    preparedStatement.setInt(2, user.userId)
                    preparedStatement.setString(3, LocalDate.now().toString())

                    preparedStatement.executeUpdate()
                    preparedStatement.close()
                    connection.close()

                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    println(e.message)
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            delete("/unlike/{suggestionId}") {
                val suggestionId = call.parameters["suggestionId"]?.toInt()
                val user = call.sessions.get<UserSession>()
                if (suggestionId == null || suggestionId < 0 || user == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }

                try {
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    val preparedStatement = connection!!.prepareStatement(
                        "delete from likes where " +
                                "suggestionId = ? AND likedById = ?"
                    )
                    preparedStatement.setInt(1, suggestionId)
                    preparedStatement.setInt(2, user.userId)

                    preparedStatement.executeUpdate()
                    preparedStatement.close()
                    connection.close()

                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    println(e.message)
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
        get("/all") {
            val reqUser = call.sessions.get<UserSession>()
            val page = call.request.queryParameters["page"]?.toInt()
            if (page == null || page < 1) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            try {
                val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                val sql = "select s.id, title, content, postDate, isAnon, postDate, " +
                        "categories.name as categoryName, u.name as userName, role, schools.name as schoolName, " +
                        "(select Count(*) from likes where suggestionId = s.id) as likes, " +
                        "(select Count(*) from likes where suggestionId = s.id AND likedById  = ?) as likedByUser " +
                        "from suggestions as s left join users as u on s.userId = u.id join categories on " +
                        "s.categoryId = categories.id left join schools on u.schoolId = schools.id " +
                        "where status='accepted' limit ? offset ?"
                val preparedStatement = connection!!.prepareStatement(sql)

                if (reqUser != null) {
                    preparedStatement.setInt(1, reqUser.userId)
                } else preparedStatement.setInt(1, -1)
                preparedStatement.setInt(2, page * 10)
                preparedStatement.setInt(3, page * 10 - 10)
                val resultSet = preparedStatement.executeQuery()

                val res = mutableListOf<SendSuggestionDTO>()
                while (resultSet.next()) {
                    val user: PublicUserDTO?
                    var isAnon: Boolean
                    if (resultSet.getString("userName").isNullOrEmpty()) {
                        user = null
                        isAnon = false
                    } else {
                        if (resultSet.getInt("isAnon") == 0) {
                            user = PublicUserDTO(
                                resultSet.getString("userName"),
                                resultSet.getString("role"),
                                resultSet.getString("schoolName")
                            )
                            isAnon = false
                        } else {
                            user = null
                            isAnon = true
                        }
                    }
                    res.add(
                        SendSuggestionDTO(
                            resultSet.getInt("id"),
                            user,
                            resultSet.getString("title"),
                            resultSet.getString("content"),
                            resultSet.getString("categoryName"),
                            isAnon,
                            resultSet.getInt("likes"),
                            resultSet.getString("postDate"),
                            if (reqUser == null) null else resultSet.getInt("likedByUser") > 0
                        )
                    )
                }
                resultSet.close()
                preparedStatement.close()
                connection.close()


                call.respond(HttpStatusCode.OK, res)
            } catch (e: Exception) {
                println(e.message)
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        get("/category") {
            try {
                val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                val preparedStatement = connection!!.prepareStatement("select * from categories order by id")
                val resultSet = preparedStatement.executeQuery()

                val result = mutableListOf<CategoryDTO>()
                while (resultSet.next()) {
                    result.add(
                        CategoryDTO(resultSet.getInt(1), resultSet.getString(2))
                    )
                }
                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                println(e.localizedMessage)
                call.respond(HttpStatusCode(400, "Error"))
            }
        }
    }
}
