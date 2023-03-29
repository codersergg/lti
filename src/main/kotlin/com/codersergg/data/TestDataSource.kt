package com.codersergg.data

import io.ktor.server.request.*

interface TestDataSource {

    suspend fun addTestDataSource(request: ApplicationRequest): Boolean?
    suspend fun getAllTestDataSource(): MutableList<ApplicationRequest>?
}