package com.codersergg.auth.data

class MemoryAuthDataSource(private val redirects: MutableMap<String, String> = mutableMapOf()) :
    AuthDataSource {
    override suspend fun getState(state: String?): String {
        return redirects[state].toString()
    }

    override suspend fun putState(state: String, redirectUrl: String) {
        redirects[state] = redirectUrl
    }
}