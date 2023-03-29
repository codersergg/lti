package com.codersergg.routes

import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.models.InitLogin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.initiateLogin(
    initLoginDataSource: InitLoginDataSource
) {
    post("initiate-login/") {
        val receiveParameters = call.request.queryParameters

        val isFieldsBlank = !receiveParameters.contains("iss") ||
                !receiveParameters.contains("login_hint") ||
                !receiveParameters.contains("target_link_uri")

        if (isFieldsBlank) {
            call.respond(HttpStatusCode.Conflict, "Fields must not be empty")
            return@post
        }

        val initLogin = InitLogin(
            iss = receiveParameters.get("iss")!!,
            login_hint = receiveParameters.get("login_hint")!!,
            target_link_uri = receiveParameters.get("target_link_uri")!!,
            lti_message_hint = receiveParameters.get("lti_message_hint"),
            lti_deployment_id = receiveParameters.get("lti_deployment_id"),
            client_id = receiveParameters.get("client_id")
        )

        // проверка уникальности
        /*val wasAcknowledged = initLoginDataSource.putByLoginHint(initLogin)
        if (!wasAcknowledged!!) {
            call.respond(HttpStatusCode.Conflict)
            return@post
        } else {
            call.respond(HttpStatusCode.OK)
        }*/

        initLoginDataSource.putByLoginHint(initLogin)
        call.respond(HttpStatusCode.OK)

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