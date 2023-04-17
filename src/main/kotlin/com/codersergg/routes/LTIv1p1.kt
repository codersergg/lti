package com.codersergg.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


suspend fun requestInitLoginV1p0(
    parameters: Parameters,
    call: ApplicationCall
) {

    val isFieldsBlank = parameters["lti_message_type"].toString().isBlank() ||
            parameters["lti_version"].toString().isBlank() ||
            parameters["resource_link_id"].toString().isBlank()

    if (isFieldsBlank) {
        call.respond(HttpStatusCode.Conflict, "Fields must not be empty")
        return
    }
    if (!parameters["lti_message_type"].equals("basic-lti-launch-request")) {
        call.respond(HttpStatusCode.Conflict, "This is not a basic launch message")
        return
    }

    println(parameters.toString())
    val sig = parameters["oauth_signature"]
    val publicKey = parameters["oauth_consumer_key"]
    val signatureMethod = parameters["oauth_signature_method"]
    println("sig: $sig")
    println("publicKey: $publicKey")
    println("signatureMethod: $signatureMethod")

    // verify
    val encodingAlgorithm = "HmacSHA1"
    val secretKey = "privateKey"

    val sha1Hmac = Mac.getInstance(encodingAlgorithm)
    val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), encodingAlgorithm)
    sha1Hmac.init(secretKeySpec)

    val body = call.receiveText()

    val hash = sha1Hmac.doFinal(body.encodeToByteArray())
    val message = Base64.getEncoder().encodeToString(hash)

    println("hash: $hash")
    println("message: $message")

    call.respondRedirect("https://infinite-lowlands-71677.herokuapp.com/redirect", false)
}