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
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate

private fun getPagedData(sql: String, databaseUrl: String, page: Int):ResultSet {
    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
    val preparedStatement = connection!!.prepareStatement(sql)
    preparedStatement.setInt(1,page*10)
    preparedStatement.setInt(2,page*10-10)
    connection.close()
    return preparedStatement.executeQuery()
}
fun Route.adminRoutes() {
    authenticate("auth-admin") {
        val databaseUrl = environment?.config?.propertyOrNull("ktor.database.url")?.getString() ?:""
        route("/admin"){
            route("/school"){
                post("/new"){
                    val body = call.receive<SchoolDTO>()
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val sql = "insert into schools (name) values (?)"
                        val preparedStatement = connection!!.prepareStatement(sql)
                        preparedStatement.setString(1,body.name)
                        preparedStatement.executeUpdate()
                        call.respond(HttpStatusCode.OK)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                put{
                    //change the school's name
                    val body = call.receive<IdSchoolDTO>()
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val preparedStatement = connection!!.prepareStatement("update schools set name = ? " +
                                "where id = ?")
                        preparedStatement.setString(1,body.name)
                        preparedStatement.setInt(2,body.id)
                        preparedStatement.executeUpdate()
                        call.respond(HttpStatusCode.OK)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                delete("/{id}"){
                    val schoolId = call.parameters["id"]
                    if(schoolId.isNullOrEmpty() || schoolId.toIntOrNull() == null || schoolId.toInt() < 0){
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val sql = "update users set schoolId = null where schoolId = ?"
                        var preparedStatement = connection!!.prepareStatement(sql)
                        preparedStatement.setInt(1,schoolId.toInt())
                        preparedStatement.executeUpdate()

                        preparedStatement = connection.prepareStatement("delete from schools where id = ?")
                        preparedStatement.setInt(1,schoolId.toInt())
                        preparedStatement.executeUpdate()

                        call.respond(HttpStatusCode.OK)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
            route("/category"){
                post("/new"){
                    val body = call.receive<AdminCategoryDTO>()
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val sql = "insert into categories (name) values(?)"
                        val preparedStatement = connection!!.prepareStatement(sql)
                        preparedStatement.setString(1,body.name)
                        preparedStatement.executeUpdate()
                        preparedStatement.close()
                        connection.close()

                        call.respond(HttpStatusCode.OK)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                put("/{id}"){
                    val body = call.receive<AdminCategoryDTO>()
                    val categoryId = call.parameters["id"]
                    if(categoryId.isNullOrEmpty() || categoryId.toIntOrNull() == null || categoryId.toInt() < 0){
                        call.respond(HttpStatusCode.BadRequest)
                        return@put
                    }
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val preparedStatement = connection!!.prepareStatement("update categories set name  = ? " +
                                "where id = ?")
                        preparedStatement.setString(1,body.name)
                        preparedStatement.setInt(2,categoryId.toInt())
                        preparedStatement.executeUpdate()
                        preparedStatement.close()
                        connection.close()

                        call.respond(HttpStatusCode.OK)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                delete{
                    val body = call.receive<AdminDeleteCategoryDTO>()
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        var prepStatement = connection!!.prepareStatement("update suggestions set categoryId = ? " +
                                "where categoryId = ?")
                        prepStatement.setInt(1,body.newCategoryId)
                        prepStatement.setInt(2,body.id)
                        prepStatement.executeUpdate()

                        prepStatement = connection.prepareStatement("delete from categories where id = ?")
                        prepStatement.setInt(1,body.id)
                        prepStatement.executeUpdate()

                        prepStatement.close()
                        connection.close()

                        call.respond(HttpStatusCode.OK)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
            route("/suggestion"){
                get("/all"){
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
                        val resultSet = getPagedData(sql, databaseUrl, page)
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
                        resultSet.close()
                        call.respond(HttpStatusCode.OK, result)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode(400, "Error"))
                    }
                }
                post("/approve"){
                    val body = call.receive<AdminApproveSuggestionDTO>()
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val sql = "insert into approvedsuggestions (suggestionId, moreInfo, date) values (?,?,?)"
                        val preparedStatement = connection!!.prepareStatement(sql)
                        preparedStatement.setInt(1,body.suggestionId)
                        preparedStatement.setString(2,body.moreInfo)
                        preparedStatement.setString(3, LocalDate.now().toString())
                        preparedStatement.executeUpdate()
                        preparedStatement.close()
                        connection.close()

                        call.respond(HttpStatusCode.OK)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                put("/approved/update"){
                    val body = call.receive<AdminApprovedSuggestionUpdateDTO>()
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val sql = "update approvedsuggestions set moreInfo = ?, date = ? where id = ?"
                        val preparedStatement = connection!!.prepareStatement(sql)
                        preparedStatement.setString(1,body.moreInfo)
                        preparedStatement.setString(2,LocalDate.now().toString())
                        preparedStatement.setInt(3, body.id)
                        preparedStatement.executeUpdate()
                        preparedStatement.close()
                        connection.close()

                        call.respond(HttpStatusCode.OK)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                get("/approved/all"){
                    val page = call.request.queryParameters["page"]?.toInt()
                    val sort = call.request.queryParameters["sort"]
                    if(page == null || page < 1) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    val sortSql = when(sort) {
                        "dateDesc" -> "order by date desc"
                        "dateAsc" -> "order by date asc"
                        "idDesc" -> "order by id desc"
                        else -> ""
                    }
                    val sql =
                        "select a.id, suggestionId, moreInfo, date, title, content, postDate, isAnon, status, " +
                                "u.id as userId, u.name as userName, role, email, schools.name as school, " +
                                "categories.name as category, (select Count(*) from likes where suggestionId = s.id)" +
                                " as likes  from approvedSuggestions a join suggestions s on a.suggestionId = s.id " +
                                "join categories on s.categoryId = categories.id left join users u on s.userId = u.id" +
                                " left join schools on u.schoolId = schools.id $sortSql limit ? offset ?"

                    try {
                        val resultSet = getPagedData(sql, databaseUrl, page)
                        val result = mutableListOf<SendApprovedSuggestionDTO>()

                        while(resultSet.next()){
                            result.add(
                                SendApprovedSuggestionDTO(
                                    resultSet.getInt("id"),
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
                                    ),
                                    resultSet.getString("moreInfo"),
                                    resultSet.getString("date")
                                )
                            )
                        }
                        resultSet.close()

                        call.respond(HttpStatusCode.OK, result)
                    }catch (e: Exception){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                put("/status"){
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
                delete("/{id}"){
                    val id = call.parameters["id"]?.toInt()
                    if(id == null){
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        var preparedStatement = connection!!.prepareStatement("select count(*) as cnt from " +
                                "approvedsuggestions where suggestionId = ?")
                        val resultSet = preparedStatement.executeQuery()
                        if(resultSet.getInt("cnt") > 0){
                            call.respond(HttpStatusCode(400, "Could not delete suggestion which is " +
                                    "approved. Delete the approved suggestion first"))
                        }
                        preparedStatement = connection.prepareStatement("delete from likes where suggestionId = ?")
                        preparedStatement.setInt(1,id)
                        preparedStatement.executeUpdate()

                        preparedStatement = connection.prepareStatement("delete from suggestions where id = ?")
                        preparedStatement.setInt(1,id)
                        preparedStatement.executeUpdate()

                        preparedStatement.close()
                        connection.close()
                        call.respond(HttpStatusCode.OK)
                    }catch (e: SQLException){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode(400, "Error"))
                    }
                }
                delete("/approved/{id}"){
                    val id = call.parameters["id"]?.toInt()
                    if(id == null){
                        call.respond(HttpStatusCode.BadRequest)
                        return@delete
                    }
                    try {
                        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                        val preparedStatement = connection!!.prepareStatement("delete from approvedsuggestions " +
                                "where suggestionId = ?")
                        preparedStatement.setInt(1,id)
                        preparedStatement.executeUpdate()

                        preparedStatement.close()
                        connection.close()
                        call.respond(HttpStatusCode.OK)
                    }catch (e: SQLException){
                        println(e.localizedMessage)
                        call.respond(HttpStatusCode(400, "Error"))
                    }
                }
            }
            post("/admin/ban"){
                val admin = call.sessions.get<UserSession>()
                val body = call.receive<AdminBanDTO>()
                if(admin == null || body.banLength < 1){
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                val date = LocalDate.now()
                try {
                    val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
                    val preparedStatement = connection!!.prepareStatement("insert into bans (userId, startDate," +
                            " reason, endDate, adminId) values (?,?,?,?,?) ")
                    preparedStatement.setInt(1,body.userId)
                    preparedStatement.setString(2,date.toString())
                    preparedStatement.setString(3,body.reason)
                    preparedStatement.setString(4,date.plusDays(body.banLength.toLong()).toString())
                    preparedStatement.setInt(5, admin.userId)
                    preparedStatement.executeUpdate()

                    preparedStatement.close()
                    connection.close()
                    call.respond(HttpStatusCode.OK)
                }catch (e: SQLException){
                    println(e.localizedMessage)
                    call.respond(HttpStatusCode(400, "Error"))
                }
            }
            delete("/admin/unban/{id}") {
                val userId = call.parameters["id"]?.toInt()
                if(userId == null || userId < 0){
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
            }
            get("/admin/bans/{id}"){

            }
        }
    }
}