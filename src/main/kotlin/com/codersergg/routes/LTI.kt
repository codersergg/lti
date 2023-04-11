package com.codersergg.routes

import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.models.InitLogin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.net.URLDecoder
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

fun Route.initiateLogin(
    initLoginDataSource: InitLoginDataSource
) {
    post("initiate-login") {
        // SECRET key Moodle: save in DB or EV
        //val SECRET = "qwasrffvw4531r"

        // url запроса аутентификации в LMS, смотреть в настройках LMS
        val authUrl = "lti-test-connect.moodlecloud.com/mod/lti/auth.php"

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
            client_id = findParameterValue(request, "client_id")
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

        val state = UUID.randomUUID().toString()
        val nonce = UUID.randomUUID().toString()
        val url = url {
            protocol = URLProtocol.HTTPS
            host = authUrl

            parameters.append("scope", "openid")
            parameters.append("response_type", "id_token")
            parameters.append("client_id", initLogin.client_id.toString())
            parameters.append("redirect_uri", initLogin.target_link_uri)
            parameters.append("login_hint", initLogin.login_hint)
                .also {
                    if (!initLogin.lti_message_hint.isNullOrBlank()) parameters.append(
                        "lti_message_hint",
                        initLogin.lti_message_hint
                    )
                }
            parameters.append("state", state)
            parameters.append("response_mode", "form_post")
            parameters.append("nonce", nonce)
            parameters.append("prompt", "none")
        }

        call.respondRedirect(url, false)
        TODO("Save state for checking")
    }
}

fun Route.authenticationResponsePost() {
    post("authentication-response") {
        val receiveText = call.receiveText()
        println("receiveText:$receiveText")
        val sha256Digest = DigestUtils.getSha256Digest()
        println("sha256Digest:$sha256Digest")
        call.respondRedirect("redirect")
        TODO("Check token")
    }
}

fun Route.redirectGet() {
    get("redirect") {
        // get the interactive from the repository
        val interactive = "src/main/resources/static/my_interactive_picture.html"
        call.respondFile(File(interactive))
    }
}

// Роут для проверки наличия запроса на аутентификацию
fun Route.getSavedInitiateLogin(
    initLoginDataSource: InitLoginDataSource
) {
    get("saved-initiate-login") {
        initLoginDataSource.getAll()
        call.respond(HttpStatusCode.OK, initLoginDataSource.getAll().toString())
    }
}

fun Route.getPublicKey() {
    get("public-key") {
        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode("SECRET"))
        println(keySpecPKCS8)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpecPKCS8)
        println(publicKey)
        call.respond(publicKey)
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