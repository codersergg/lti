package com.codersergg.data

import io.ktor.server.application.*
import io.ktor.server.request.*

class MemoryTestDataSource(
    private val calls: ArrayList<ApplicationCall> = ArrayList(),
    private val texts: ArrayList<String> = ArrayList()
) :
    TestDataSource {
    override suspend fun addTestDataSource(request: ApplicationCall): Boolean {
        val add = calls.add(request)
        texts.add(request.receiveText())
        return add
    }

    override suspend fun getAllTestDataSource(): ArrayList<ApplicationCall> {
        return calls
    }

    override suspend fun getAllReceiveText(): ArrayList<String> {
        return texts
    }
}