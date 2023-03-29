package com.codersergg.data

import com.codersergg.data.models.InitLogin

class MemoryInitLoginDataSource(private val map: MutableMap<String, InitLogin> = HashMap()) :
    InitLoginDataSource {

    override suspend fun getByLoginHint(loginHint: String): InitLogin? {
        return map[loginHint]
    }

    override suspend fun getAll(): MutableMap<String, InitLogin> {
        return map
    }

    override suspend fun putByLoginHint(initLogin: InitLogin): Boolean {
        val key = initLogin.login_hint
        map.put(key, initLogin) ?: return true
        return false
    }

}