package com.codersergg.data

import io.ktor.server.application.*

class MemoryTestDataSource(private val list: ArrayList<ApplicationCall> = ArrayList()) :
    TestDataSource {
    override suspend fun addTestDataSource(request: ApplicationCall): Boolean {
        return list.add(request)
    }

    override suspend fun getAllTestDataSource(): ArrayList<ApplicationCall> {
        return list
    }
}