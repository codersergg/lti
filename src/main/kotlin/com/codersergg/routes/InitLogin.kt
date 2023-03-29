package com.codersergg.routes

import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.TestDataSource
import com.codersergg.data.models.InitLogin
import com.codersergg.data.models.requests.InitLoginRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.initiateLogin(
    initLoginDataSource: InitLoginDataSource
) {
    post("initiate-login") {
        val request =
            kotlin.runCatching { call.receiveNullable<InitLoginRequest>() }.getOrNull()
                ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
        val isFieldsBlank = request.iss.isBlank() || request.login_hint.isBlank()
                || request.target_link_uri.isBlank()

        if (isFieldsBlank) {
            call.respond(HttpStatusCode.Conflict, "Fields must not be empty")
            return@post
        }

        if (initLoginDataSource.getByLoginHint(request.login_hint) != null) {
            call.respond(HttpStatusCode.Conflict, "Login hint is exist")
            return@post
        }

        val initLogin = InitLogin(
            iss = request.iss,
            login_hint = request.login_hint,
            target_link_uri = request.target_link_uri
        )

        val wasAcknowledged = initLoginDataSource.putByLoginHint(initLogin)
        if (!wasAcknowledged!!) {
            call.respond(HttpStatusCode.Conflict)
            return@post
        } else {
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun Route.getSavedInitiateLogin(
    initLoginDataSource: InitLoginDataSource
) {
    get("saved-initiate-login") {
        initLoginDataSource.getAll()
        call.respond(HttpStatusCode.OK, initLoginDataSource.getAll().toString())
    }

}

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
            allTestDataSource?.get(allTestDataSource.size - 1)?.request?.acceptItems().toString())
    }
}

fun Route.getAllEncoding(
    testDataSource: TestDataSource
) {
    get("get-all-test-encoding") {
        val allTestDataSource = testDataSource.getAllTestDataSource()
        call.respond(
            HttpStatusCode.OK,
            allTestDataSource?.get(allTestDataSource.size - 1)?.request?.acceptEncodingItems().toString())
    }
}

fun Route.getAllReceiveText(
    testDataSource: TestDataSource
) {
    get("get-all-test-receive-text") {
        val allTestDataSource = testDataSource.getAllTestDataSource()
        call.respond(
            HttpStatusCode.OK,
            allTestDataSource?.get(allTestDataSource.size - 1)?.receiveText().toString())
    }
}

fun Route.getSizeTestRequest(
    testDataSource: TestDataSource
) {
    get("get-all-test-size") {
        val allTestDataSource = testDataSource.getAllTestDataSource()
        if (allTestDataSource != null) {
            call.respond(
                HttpStatusCode.OK,
                allTestDataSource.size)
        }
    }
}