package com.codersergg.data

import com.codersergg.data.models.InitLogin

class MemoryInitLoginDataSource(private val map: MutableMap<String, InitLogin> = HashMap()) :
    InitLoginDataSource {

    override suspend fun getByLoginHint(loginHint: String): InitLogin? {
        return map[loginHint]
    }

    override suspend fun putByLoginHint(initLogin: InitLogin): Boolean {
        val key = initLogin.login_hint
        map[key] = initLogin
        return map.containsKey(key)
    }

}