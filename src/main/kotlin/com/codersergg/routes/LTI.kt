package com.codersergg.routes

import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.models.InitLogin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.util.*
import java.net.URLDecoder
import java.util.*


fun Route.initiateLogin(
    initLoginDataSource: InitLoginDataSource
) {
    post("initiate-login") {
        val request = call.receiveText()
        val isFieldsBlank = !request.contains("iss") ||
                !request.contains("login_hint") ||
                !request.contains("target_link_uri")

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
            client_id = findParameterValue(request, "client_id"),
            session = call.sessions
        )

        // проверка уникальности
        /*val wasAcknowledged = initLoginDataSource.putByLoginHint(initLogin)
        if (!wasAcknowledged!!) {
            call.respond(HttpStatusCode.Conflict)
            return@post
        } else {
            call.respond(HttpStatusCode.OK)
        }*/

        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        httpClient.post(initLogin.iss) {
            generateSessionId()
            url {
                parameters.append("scope", "openid")
                parameters.append("response_type", "id_token")
                parameters.append("client_id", initLogin.client_id.toString())
                parameters.append(
                    "redirect_uri",
                    "https://infinite-lowlands-71677.herokuapp.com/redirect"
                )
                parameters.append("login_hint", initLogin.login_hint)
                    .also {
                        if (!initLogin.lti_message_hint.isNullOrBlank()) parameters.append(
                            "lti_message_hint",
                            initLogin.lti_message_hint
                        )
                    }
                parameters.append("state", UUID.randomUUID().toString())
                parameters.append("response_mode", "form_post")
                parameters.append("nonce", UUID.randomUUID().toString())
                parameters.append("prompt", "none")
            }

        }

        initLoginDataSource.putByLoginHint(initLogin)

        call.respond(HttpStatusCode.OK)

    }
}

fun Route.initiateLogin() {
    post("authentication-response") {

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

private fun findParameterValue(text: String, parameterName: String): String? {
    val second = text.split('&').map {
        val parts = it.split('=')
        val name = parts.firstOrNull() ?: ""
        val value = parts.drop(1).firstOrNull() ?: ""
        Pair(name, value)
    }.firstOrNull { it.first == parameterName }?.second
    return URLDecoder.decode(second, "UTF-8")
}