package com.codersergg.routes

import com.codersergg.data.TestDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.test(
    testDataSource: TestDataSource
) {
    post("test") {
        val request = call
        testDataSource.addTestDataSource(request)
        call.respond(HttpStatusCode.OK)
    }
}

fun Route.getAllHeaderValue(
    testDataSource: TestDataSource
) {
    get("get-all-test-header") {
        val allTestDataSource = testDataSource.getAllTestDataSource()
        call.respond(
            HttpStatusCode.OK,
            allTestDataSource?.get(allTestDataSource.size - 1)?.request?.acceptItems().toString()
        )
    }
}

fun Route.getAllEncoding(
    testDataSource: TestDataSource
) {
    get("get-all-test-encoding") {
        val allTestDataSource = testDataSource.getAllTestDataSource()
        call.respond(
            HttpStatusCode.OK,
            allTestDataSource?.get(allTestDataSource.size - 1)?.request?.acceptEncodingItems()
                .toString()
        )
    }
}

fun Route.getAllReceiveText(
    testDataSource: TestDataSource
) {
    get("get-all-test-receive-text") {
        val allTestDataSource = testDataSource.getAllReceiveText()
        val text = allTestDataSource[allTestDataSource.lastIndex]
        call.respondText(text)
    }
}

fun Route.getSizeTestRequest(
    testDataSource: TestDataSource
) {
    get("get-all-test-size") {
        val allTestDataSource = testDataSource.getAllTestDataSource()
        call.respondText(allTestDataSource?.size.toString())
    }
}