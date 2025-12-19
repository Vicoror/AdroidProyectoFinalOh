package com.vicoror.appandroidfinal.data.model

import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("login")
    suspend fun getAllUsers(): Response<List<User>>
}

data class User(
    val id: Int,
    val correo: String,
    val password: String
)
