package com.codersergg.data

import io.ktor.server.application.*

interface TestDataSource {

    suspend fun addTestDataSource(request: ApplicationCall): Boolean?
    suspend fun getAllTestDataSource(): ArrayList<ApplicationCall>?
    suspend fun getAllReceiveText(): ArrayList<String>
}