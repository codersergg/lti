package com.codersergg.auth.data

interface AuthDataSource {
    suspend fun getState(state: String?): String
    suspend fun putState(state: String, redirectUrl: String)
}