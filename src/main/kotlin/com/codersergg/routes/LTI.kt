package com.codersergg.routes

import com.codersergg.data.AuthenticationData
import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.models.InitLogin
import com.codersergg.data.models.State
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.net.URLDecoder
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.*

fun Route.initiateLogin(
    initLoginDataSource: InitLoginDataSource,
    authenticationData: AuthenticationData
) {
    post("initiate-login") {

        // Индивидуальные параметры площадки

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

        val clientId = findParameterValue(request, "client_id")
        val endUserIdentifier = findParameterValue(request, "login_hint")
        val initLogin = InitLogin(
            iss = findParameterValue(request, "iss")!!,
            login_hint = endUserIdentifier!!,
            target_link_uri = findParameterValue(request, "target_link_uri")!!,
            lti_message_hint = findParameterValue(request, "lti_message_hint"),
            lti_deployment_id = findParameterValue(request, "lti_deployment_id"),
            client_id = clientId
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
        authenticationData.putState(
            State(
                state = state,
                nonce = nonce,
                clientId = clientId.toString(),
                endUserIdentifier = endUserIdentifier
            )
        )
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
    }
}

fun Route.authenticationResponsePost(authenticationData: AuthenticationData) {
    post("authentication-response") {
        val receiveText = call.receiveText()

        val token = findParameterValue(receiveText, "id_token")
        val stateAuthResponse = findParameterValue(receiveText, "state")

        // Check State
        if (!authenticationData.isCorrectState(stateAuthResponse.toString())) {
            call.respond(HttpStatusCode.Conflict, "Wrong state")
            return@post
        }

        val chunks = token!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val header = String(Base64.getUrlDecoder().decode(chunks[0]))
        val payload = String(Base64.getUrlDecoder().decode(chunks[1]))

        val state = authenticationData.getState(stateAuthResponse.toString())
        val nonce = state.nonce
        val clientId = state.clientId
        val endUserIdentifier = state.endUserIdentifier

        val jsonPayload: Map<String, JsonElement> = Json.parseToJsonElement(payload).jsonObject

        // Check nonce
        if (!payload.contains("\"nonce\":\"$nonce\"")) {
            call.respond(HttpStatusCode.Conflict, "Wrong nonce")
            return@post
        }
        // Check Client ID
        val jsonClientId = jsonPayload["aud"].toString()
        if (!payload.contains("\"aud\":\"$clientId\"")) {
            call.respond(HttpStatusCode.Conflict, "Wrong Client ID 1")
            return@post
        }
        if (jsonClientId != clientId) {
            call.respond(HttpStatusCode.Conflict, "Wrong Client ID 2")
            return@post
        }
        // Check end User identifier
        if (!payload.contains("\"sub\":\"$endUserIdentifier\"")) {
            call.respond(HttpStatusCode.Conflict, "Wrong Users Identifier")
            return@post
        }

        println("header: $header")
        println("payload: $payload")

        call.respondRedirect("redirect")
    }
}

fun Route.redirectGet() {
    get("redirect") {
        // get the interactive from the repository
        val interactive = "src/main/resources/static/my_interactive_picture.html"
        call.respondFile(File(interactive))
    }
}

fun Route.getPublicKey() {
    get("public-key") {
        val data: ByteArray = Base64.getDecoder()
            .decode("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDR3WR47goUtiHKHPnlLTzS5YY2V4Kj4stKO+OLBv5QVnWMhWplXBAbdvBMTMTLbM51ay0Pb8+ldXkzXMLLsIyHx3j6eFZ3nYp3O1OAo6poQmPrWWm/eIroCQy34eiW3+2g0XMLs21CuNVUMA1kHMOc3ICBNJ43091Cx/4anEq2vQIDAQAB".toByteArray())
        val spec = X509EncodedKeySpec(data)
        val fact = KeyFactory.getInstance("RSA")

        val publicKey = fact.generatePublic(spec)
        println("publicKey:$publicKey")

        call.respond(publicKey.toString())
    }
}

fun Route.getPublicKeyPost() {
    post("public-key") {
        val data: ByteArray = Base64.getDecoder()
            .decode("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDR3WR47goUtiHKHPnlLTzS5YY2V4Kj4stKO+OLBv5QVnWMhWplXBAbdvBMTMTLbM51ay0Pb8+ldXkzXMLLsIyHx3j6eFZ3nYp3O1OAo6poQmPrWWm/eIroCQy34eiW3+2g0XMLs21CuNVUMA1kHMOc3ICBNJ43091Cx/4anEq2vQIDAQAB".toByteArray())
        val spec = X509EncodedKeySpec(data)
        val fact = KeyFactory.getInstance("RSA")

        val publicKey = fact.generatePublic(spec)
        println("publicKey:$publicKey")

        call.respond(publicKey.toString())
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


private fun findParameterValue(text: String, parameterName: String): String? {
    val second = text.split('&').map {
        val parts = it.split('=')
        val name = parts.firstOrNull() ?: ""
        val value = parts.drop(1).firstOrNull() ?: ""
        Pair(name, value)
    }.firstOrNull { it.first == parameterName }?.second
    return URLDecoder.decode(second, "UTF-8")
}