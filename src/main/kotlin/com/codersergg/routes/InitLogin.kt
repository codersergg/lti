package com.codersergg.routes

import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.TestDataSource
import com.codersergg.data.models.InitLogin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.initiateLogin(
    initLoginDataSource: InitLoginDataSource
) {
    post("initiate-login") {
        val request = call.receiveText()

        val isFieldsBlank = !request.contains("iss") || !request.contains("login_hint")
                || !request.contains("target_link_uri")

        if (isFieldsBlank) {
            call.respond(HttpStatusCode.Conflict, "Fields must not be empty")
            return@post
        }

        val initLogin = InitLogin(
            iss = findParameterValue(request, "iss")!!,
            login_hint = findParameterValue(request, "login_hint")!!,
            target_link_uri = findParameterValue(request, "target_link_uri")!!,
            lti_message_hint = findParameterValue(request, "lti_message_hint"),
            lti_deployment_id = findParameterValue(request, "lti_deployment_id"),
            client_id = findParameterValue(request, "client_id")
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

private fun findParameterValue(text: String, parameterName: String): String? {
    return text.split('&').map {
        val parts = it.split('=')
        val name = parts.firstOrNull() ?: ""
        val value = parts.drop(1).firstOrNull() ?: ""
        Pair(name, value)
    }.firstOrNull { it.first == parameterName }?.second
}