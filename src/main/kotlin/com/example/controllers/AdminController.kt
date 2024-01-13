package com.example.controllers

import com.example.dto.AdminSuggestionDTO
import com.example.dto.AdminSuggestionStatusDTO
import com.example.dto.AdminUserDTO
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection
import java.sql.DriverManager

fun Route.adminRoutes() {
    authenticate("auth-admin") {
        val databaseUrl = environment?.config?.propertyOrNull("ktor.database.url")?.getString() ?:""
        route("/admin"){
            get("/suggestions"){
                val filterParams = call.parameters.getAll("filter")
                val sort = call.request.queryParameters["sort"]
                val page = call.request.queryParameters["page"]?.toInt()
                if(page == null || page < 1) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                var sql = "select s.id as suggestionId, title, content, postDate, isAnon, status, u.id as userId, " +
                        "u.name as userName, role, email, schools.name as school, categories.name as category," +
                        " (select Count(*) from likes where suggestionId = s.id) as likes from suggestions s join" +
                        " categories on s.categoryId = categories.id left join users u on s.userId = u.id left join " +
                        "schools on u.schoolId = schools.id"

                if(filterParams != null) {
                    val filters = filterParams.filter { f -> Validator.getSuggestionStatusList().contains(f) }
                    if(filters.size in 1..3){
                        var where= ""
                        for(filter in filters){
                            where += "OR status = '$filter' "
                        }
                        sql += " where" + where.substring(2)
                    }
                }
                sql += when(sort){
                    "dateDesc" -> " order by postDate desc, s.id"
                    "dateAsc" -> " order by postDate asc, s.id"
                    "likesDesc" -> " order by likes desc, s.id"
                    "likesAsc" -> " order by likes asc, s.id"
                    else -> " order by postDate desc, s.id"
                }
                sql += " limit ? offset ?"
                try {
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    val preparedStatement = connection!!.prepareStatement(sql)
                    preparedStatement.setInt(1,page*10)
                    preparedStatement.setInt(2,page*10-10)
                    val resultSet = preparedStatement.executeQuery()
                    val result = mutableListOf<AdminSuggestionDTO>()
                    while(resultSet.next()){
                        result.add(
                            AdminSuggestionDTO(
                                resultSet.getInt("suggestionId"),
                                resultSet.getString("title"),
                                resultSet.getString("content"),
                                resultSet.getString("postDate"),
                                resultSet.getInt("isAnon"),
                                resultSet.getString("status"),
                                resultSet.getString("category"),
                                if(!resultSet.getString("userId").isNullOrEmpty()) AdminUserDTO(
                                    resultSet.getInt("userId"),
                                    resultSet.getString("userName"),
                                    resultSet.getString("role"),
                                    resultSet.getString("email"),
                                    resultSet.getString("school"),
                                ) else null,
                                resultSet.getInt("likes")
                            ))
                    }
                    connection.close()
                    preparedStatement.close()
                    resultSet.close()
                    call.respond(HttpStatusCode.OK, result)
                }catch (e: Exception){
                    println(e.localizedMessage)
                    call.respond(HttpStatusCode(400, "Error"))
                }
            }
            put("/suggestion/status"){
                val body = call.receive<AdminSuggestionStatusDTO>()
                if(!Validator.validateSuggestionStatus(body.status)){
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }
                try {
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    val sql = "update suggestions set status = ? where id = ?"
                    val preparedStatement = connection!!.prepareStatement(sql)
                    preparedStatement.setString(1, body.status)
                    preparedStatement.setInt(2, body.suggestionId)
                    preparedStatement.executeUpdate()
                    preparedStatement.close()
                    connection.close()
                    call.respond(HttpStatusCode.OK)
                }catch (e: Exception){
                    println(e.localizedMessage)
                    call.respond(HttpStatusCode(400, "Error"))
                }
            }
        }
    }
}