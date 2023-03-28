package com.codersergg.data

import com.codersergg.data.models.InitLogin

interface InitLoginDataSource {

    suspend fun getByLoginHint(loginHint: String): InitLogin?

    suspend fun putByLoginHint(initLogin: InitLogin): Boolean?
}