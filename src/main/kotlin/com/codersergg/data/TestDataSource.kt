package com.codersergg.data

import io.ktor.server.application.*
import io.ktor.server.request.*

interface TestDataSource {

    suspend fun addTestDataSource(request: ApplicationCall): Boolean?
    suspend fun getAllTestDataSource(): MutableList<ApplicationCall>?
}