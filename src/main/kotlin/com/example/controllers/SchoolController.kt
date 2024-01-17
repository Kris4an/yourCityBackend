package com.example.controllers

import com.example.dto.IdSchoolDTO
import com.example.dto.SchoolDTO
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection
import java.sql.DriverManager

fun getSchoolById(id: Int?, databaseUrl: String): SchoolDTO? {
    if(id == null) return null
    try{
        val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
        val sql = "select name from schools where id=?"
        val preparedStatement = connection!!.prepareStatement(sql)
        preparedStatement.setInt(1,id.toInt())
        val result = preparedStatement.executeQuery()
        if(!result.next()){
            return null
        }
        val school = SchoolDTO(result.getString("name"))
        result.close()
        connection.close()
        if(school.name.isEmpty()) return null
        return school
    }catch (e:Exception){
        throw e
    }
}
fun Route.schoolRoutes() {
    route("/school"){
        val databaseUrl = environment?.config?.propertyOrNull("ktor.database.url")?.getString() ?:""
        get("/{id}"){
            val schoolId = call.parameters["id"]
            if(schoolId.isNullOrEmpty() || schoolId.toIntOrNull() == null){
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            val result = getSchoolById(schoolId.toInt(), databaseUrl)
            if(result == null){
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respond(HttpStatusCode.OK, result)
        }
        get("/all"){
            val connection: Connection? = DriverManager.getConnection(databaseUrl, "root", "")
            val statement = connection!!.createStatement()
            val sql = "select * from schools"
            val result = statement.executeQuery(sql)
            val finalRes = mutableListOf<IdSchoolDTO>()
            while(result.next()){
                val school = IdSchoolDTO(result.getInt("id"), result.getString("name"))
                finalRes.add(school)
            }
            result.close()
            statement.close()
            connection.close()
            call.respond(HttpStatusCode.OK, finalRes)
        }
    }
}