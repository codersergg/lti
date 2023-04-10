package com.codersergg.data

import com.codersergg.data.models.InitLogin

interface AuthenticationData {
    suspend fun getByLoginHint(loginHint: String): InitLogin?
    suspend fun getAll(): MutableMap<String, InitLogin>?
    suspend fun putByLoginHint(initLogin: InitLogin): Boolean?
}