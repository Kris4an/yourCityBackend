package com.example.security

import io.ktor.server.sessions.*
import java.sql.Connection
import java.sql.DriverManager

class SessionStorageDatabase : SessionStorage{
    private val url = System.getenv("DATABASE_URL")
    override suspend fun invalidate(id: String) {
        try{
            val connection: Connection? = DriverManager.getConnection(url, "root", "")
            val sql = "delete from sessions where id=?"
            val preparedStatement = connection!!.prepareStatement(sql)
            preparedStatement.setString(1, id)
            preparedStatement.executeUpdate()
            connection.close()

        }catch (e:Exception){
            throw e
        }
    }

    override suspend fun read(id: String): String {
        try{
            val connection: Connection? = DriverManager.getConnection(url, "root", "")
            val sql = "select * from sessions where id=?"
            val preparedStatement = connection!!.prepareStatement(sql)
            preparedStatement.setString(1,id)

            val result = preparedStatement.executeQuery()
            connection.close()
            if(!result.next()) throw NoSuchElementException("Session $id not found")
            return result.getString("value") ?:throw NoSuchElementException("Session $id not found")
        }catch (e:Exception){
            throw e
        }
    }
    override suspend fun write(id: String, value: String) {
        try{
            val connection: Connection? = DriverManager.getConnection(url, "root", "")
            val sql = "insert into sessions values(?,?)"
            val preparedStatement = connection!!.prepareStatement(sql)
            preparedStatement.setString(1,id)
            preparedStatement.setString(2,value)
            preparedStatement.execute()
            connection.close()

        }catch (e: Exception){
            println("id=${id}, value=${value}")
            throw e
        }
    }
}