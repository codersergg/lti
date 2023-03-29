package com.codersergg.data

import io.ktor.server.request.*

class MemoryTestDataSource(private val list: ArrayList<ApplicationRequest> = ArrayList()) :
    TestDataSource {
    override suspend fun addTestDataSource(request: ApplicationRequest): Boolean {
        return list.add(request)
    }

    override suspend fun getAllTestDataSource(): MutableList<ApplicationRequest> {
        return list
    }
}