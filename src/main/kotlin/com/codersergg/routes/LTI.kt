package com.codersergg.routes

import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.models.InitLogin
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Location
import io.ktor.http.HttpHeaders.SetCookie
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.util.*
import org.apache.commons.codec.digest.DigestUtils
import java.net.URLDecoder
import java.util.*
import kotlin.collections.set


fun Route.initiateLogin(
    initLoginDataSource: InitLoginDataSource
) {
    post("initiate-login") {
        val SECRET = "qwasrffvw4531r"
        val request = call.receiveText()
        val isFieldsBlank = !request.contains("iss") ||
                !request.contains("login_hint") ||
                !request.contains("target_link_uri")

        if (isFieldsBlank) {
            call.respond(HttpStatusCode.Conflict, "Fields must not be empty")
            return@post
        }

        val session = call.sessions
        //session.set("session_id", UUID.randomUUID().toString())
        val initLogin = InitLogin(
            iss = findParameterValue(request, "iss")!!,
            login_hint = findParameterValue(request, "login_hint")!!,
            target_link_uri = findParameterValue(request, "target_link_uri")!!,
            lti_message_hint = findParameterValue(request, "lti_message_hint"),
            lti_deployment_id = findParameterValue(request, "lti_deployment_id"),
            client_id = findParameterValue(request, "client_id"),
            session = session
        )

        initLoginDataSource.putByLoginHint(initLogin)

        // проверка уникальности
        /*val wasAcknowledged = initLoginDataSource.putByLoginHint(initLogin)
        if (!wasAcknowledged!!) {
            call.respond(HttpStatusCode.Conflict)
            return@post
        } else {
            call.respond(HttpStatusCode.OK)
        }*/


        val url = url {
            protocol = URLProtocol.HTTPS
            host = initLogin.iss.replace("https://", "")

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

        val state = UUID.randomUUID().toString()

        val client = HttpClient()
        client.request(url) {
            method = HttpMethod.Post
            headers {
                append(SetCookie, DigestUtils.sha256Hex(state))
            }
        }
    }
}

fun Route.authenticationResponse() {
    get("authentication-response") {
        call.respondText("GOOD authentication-response!!!")
    }
}

fun Route.redirectGet() {
    post("redirect") {
        call.respondText("GOOD redirect GET!!!")
    }
}

fun Route.redirectPost() {
    post("redirect") {
        call.respondText("GOOD redirect POST!!!")
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