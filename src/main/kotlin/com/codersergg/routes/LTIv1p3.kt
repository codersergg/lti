package com.codersergg.routes

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.codersergg.data.AuthenticationData
import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.models.InitLogin
import com.codersergg.data.models.State
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

suspend fun PipelineContext<Unit, ApplicationCall>.requestInitLoginV1p3(
    parameters: Parameters,
    initLoginDataSource: InitLoginDataSource,
    authenticationData: AuthenticationData,
    authUrl: String
) {

    val isFieldsBlank = parameters["iss"].toString().isBlank() ||
            parameters["login_hint"].toString().isBlank() ||
            parameters["target_link_uri"].toString().isBlank()

    if (isFieldsBlank) {
        call.respond(HttpStatusCode.Conflict, "Fields must not be empty")
        return
    }

    parameters["lti_deployment_id"].toString()

    val clientId = parameters["client_id"].toString()
    val endUserIdentifier = parameters["login_hint"].toString()
    val iss = parameters["iss"].toString()
    val initLogin = InitLogin(
        iss = iss,
        login_hint = endUserIdentifier,
        target_link_uri = parameters["target_link_uri"].toString(),
        lti_message_hint = parameters["lti_message_hint"].toString(),
        lti_deployment_id = parameters["lti_deployment_id"].toString(),
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
            clientId = clientId,
            endUserIdentifier = endUserIdentifier,
            iss = iss
        )
    )
    val url = url {
        protocol = URLProtocol.HTTPS
        host = authUrl

        this.parameters.append("scope", "openid")
        this.parameters.append("response_type", "id_token")
        this.parameters.append("client_id", initLogin.client_id.toString())
        this.parameters.append("redirect_uri", initLogin.target_link_uri)
        this.parameters.append("login_hint", initLogin.login_hint)
            .also {
                if (!initLogin.lti_message_hint.isNullOrBlank()) this.parameters.append(
                    "lti_message_hint",
                    initLogin.lti_message_hint
                )
            }
        this.parameters.append("state", state)
        this.parameters.append("response_mode", "form_post")
        this.parameters.append("nonce", nonce)
        this.parameters.append("prompt", "none")
    }

    call.respondRedirect(url, false)
}

fun Route.authenticationResponsePost(
    authenticationData: AuthenticationData,
    jwkProvider: JwkProvider,
    privateKeyString: String
) {
    post("authentication-response") {
        val formParameters = call.receiveParameters()

        // Check State
        val stateAuthResponse = formParameters["state"].toString()
        val state = authenticationData.getState(stateAuthResponse)
        if (stateAuthResponse != state.state) {
            call.respond(HttpStatusCode.Conflict, "Wrong state")
            return@post
        }

        val token = formParameters["id_token"].toString()
        val chunks = token.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val header = String(Base64.getUrlDecoder().decode(chunks[0]))
        val payload = String(Base64.getUrlDecoder().decode(chunks[1]))
        val jsonPayload: Map<String, JsonElement> = Json.parseToJsonElement(payload).jsonObject

        // Check nonce
        val nonce = state.nonce
        println(nonce)
        val jsonNonce = jsonPayload["nonce"].toString()
        println(jsonNonce)
        if (!jsonNonce.contains(nonce)) {
            call.respond(HttpStatusCode.Conflict, "Wrong nonce")
            return@post
        }
        // Check Client ID
        val clientId = state.clientId
        val jsonClientId = jsonPayload["aud"].toString()
        if (!jsonClientId.contains(clientId)) {
            call.respond(HttpStatusCode.Conflict, "Wrong Client ID")
            return@post
        }
        // Check end User identifier
        val endUserIdentifier = state.endUserIdentifier
        val jsonEndUserIdentifier = jsonPayload["sub"].toString()
        if (!jsonEndUserIdentifier.contains(endUserIdentifier)) {
            call.respond(HttpStatusCode.Conflict, "Wrong Users Identifier")
            return@post
        }

        println("header: $header")
        println("payload: $payload")

        getGrade(
            authenticationData,
            stateAuthResponse,
            jsonPayload,
            jwkProvider,
            jsonNonce,
            privateKeyString
        )
        call.respondRedirect("redirect")

        // TO DO
        // Check exp
        // The current time MUST be before the time represented by the exp Claim
        // https://www.imsglobal.org/spec/security/v1p0/#authentication-response-validation

        // Check iat
        // The Tool MAY use the iat Claim to reject tokens that were issued too far away from the
        // current time, limiting the amount of time that it needs to store nonces used to prevent
        // attacks. The Tool MAY define its own acceptable time range
        // https://www.imsglobal.org/spec/security/v1p0/#authentication-response-validation

        // Check token
        // The Tool MUST Validate the signature of the ID Token according to JSON Web
        // Signature [RFC7515], Section 5.2 using the Public Key from the Platform
        // https://www.imsglobal.org/spec/security/v1p0/#authentication-response-validation
    }
    static(".well-known") {
        staticRootFolder = File("certs")
        file("jwks.json")
    }
}

private suspend fun getGrade(
    authenticationData: AuthenticationData,
    stateAuthResponse: String?,
    jsonPayload: Map<String, JsonElement>,
    jwkProvider: JwkProvider,
    jsonNonce: String,
    privateKeyString: String
) {
    val updatedState = authenticationData.getState(stateAuthResponse.toString())
    println("updatedState: $updatedState")
    val lineitem = jsonPayload["https://purl.imsglobal.org/spec/lti-ags/claim/endpoint"]
        ?.jsonObject?.get("lineitem").toString()
        .replace("\"", "")
        .replace("https://", "")
    println("lineitem: $lineitem")
    authenticationData.putState(updatedState)
    println(updatedState)

    val publicKey = jwkProvider.get("6f8856ed-9189-488f-9011-0ff4b6c08edc").publicKey
    // https://stackoverflow.com/questions/6559272/algid-parse-error-not-a-sequence
    val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))
    val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)

    val respondToken = JWT.create()
        .withClaim("iss", "914tL6Nm7c7Kba7")
        .withClaim("aud", "https://lti-test-connect.moodlecloud.com")
        .withClaim("nonce", jsonNonce.replace("\"", ""))
        .withClaim("exp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + 60 * 10)
        .withClaim("iat", DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + 1)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))
    println("respondToken: $respondToken")

    val status = HttpClient().use { client ->
        client.get(
            url {
                protocol = URLProtocol.HTTPS
                host = lineitem
            }
        ) {
            headers {
                append(HttpHeaders.Accept, "application/vnd.ims.lis.v2.lineitem+json")
                append(HttpHeaders.Authorization, "Bearer $respondToken")
            }
        }
    }
    println("status: $status")
}