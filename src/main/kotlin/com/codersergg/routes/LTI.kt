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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.net.URLDecoder
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
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
                endUserIdentifier = endUserIdentifier,
                iss = findParameterValue(request, "iss")!!
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

fun Route.authenticationResponsePost(
    authenticationData: AuthenticationData,
    jwkProvider: JwkProvider,
    privateKeyString: String
) {
    post("authentication-response") {
        val receiveText = call.receiveText()

        // Check State
        val stateAuthResponse = findParameterValue(receiveText, "state")
        val state = authenticationData.getState(stateAuthResponse.toString())
        if (!stateAuthResponse.equals(state.state)) {
            call.respond(HttpStatusCode.Conflict, "Wrong state")
            return@post
        }

        val token = findParameterValue(receiveText, "id_token")
        val chunks = token!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
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

        val updatedState = authenticationData.getState(stateAuthResponse.toString())
        println("updatedState: $updatedState")
        val lineitem = jsonPayload["https://purl.imsglobal.org/spec/lti-ags/claim/endpoint"]
            ?.jsonObject?.get("lineitem").toString()
            .replace("\"", "")
            .replace("https://", "")
        println("lineitem: $lineitem")
        authenticationData.putState(updatedState)
        println(updatedState)

        println("header: $header")
        println("payload: $payload")

        val publicKey = jwkProvider.get("6f8856ed-9189-488f-9011-0ff4b6c08edc").publicKey
        // https://stackoverflow.com/questions/6559272/algid-parse-error-not-a-sequence
        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode("MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAtfJaLrzXILUg1U3N1KV8yJr92GHn5OtYZR7qWk1Mc4cy4JGjklYup7weMjBD9f3bBVoIsiUVX6xNcYIr0Ie0AQIDAQABAkEAg+FBquToDeYcAWBe1EaLVyC45HG60zwfG1S4S3IB+y4INz1FHuZppDjBh09jptQNd+kSMlG1LkAc/3znKTPJ7QIhANpyB0OfTK44lpH4ScJmCxjZV52mIrQcmnS3QzkxWQCDAiEA1Tn7qyoh+0rOO/9vJHP8U/beo51SiQMw0880a1UaiisCIQDNwY46EbhGeiLJR1cidr+JHl86rRwPDsolmeEF5AdzRQIgK3KXL3d0WSoS//K6iOkBX3KMRzaFXNnDl0U/XyeGMuUCIHaXv+n+Brz5BDnRbWS+2vkgIe9bUNlkiArpjWvX+2we"))
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)

        val respondToken = JWT.create()
            .withClaim("iss", jsonPayload["iss"].toString().replace("\"", ""))
            .withClaim("aud", jsonClientId.replace("\"", ""))
            .withClaim("nonce", jsonNonce.replace("\"", ""))
            .withClaim("exp", jsonPayload["exp"].toString().replace("\"", ""))
            .withClaim("iat", jsonPayload["iat"].toString().replace("\"", ""))
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))
        println("respondToken: $respondToken")

        val status = HttpClient().use { client ->
            client.post(
                url {
                    protocol = URLProtocol.HTTPS
                    host = lineitem
                    parameters.append("JWT", respondToken)
                }
            ) {
                headers {
                    append(HttpHeaders.Accept, "application/vnd.ims.lis.v2.lineitem+json")
                    //append(HttpHeaders.Authorization, "abc123")
                }
            }
        }
        println("status: $status")
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

fun Route.redirect() {
    get("redirect") {
        // get the interactive from the repository
        val interactive = "src/main/resources/static/my_interactive_picture.html"
        call.respondFile(File(interactive))
    }
}

/*fun Route.grade(authenticationData: AuthenticationData) {
    get("grade") {
        val status = HttpClient().use { client ->
            client.get(
                authenticationData.getState()
            )
        }
    }
}*/

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