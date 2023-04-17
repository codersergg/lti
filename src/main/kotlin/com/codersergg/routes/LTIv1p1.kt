package com.codersergg.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import java.security.KeyFactory
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


suspend fun PipelineContext<Unit, ApplicationCall>.requestInitLoginV1p0(
    parameters: Parameters
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
    val  secretKey = "privateKey"

    val sha1Hmac = Mac.getInstance(encodingAlgorithm)
    val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), encodingAlgorithm)
    sha1Hmac.init(secretKeySpec)

    val hash = sha1Hmac.doFinal(sig!!.toByteArray())
    val message = Base64.getEncoder().encodeToString(hash)

    println("hash: $hash")
    println("message: $message")

    call.respondRedirect("https://infinite-lowlands-71677.herokuapp.com/redirect", false)
}

fun pubKeyFromString(key: String?): PublicKey {
    val keySpecPublic = X509EncodedKeySpec(Base64.getDecoder().decode(key))
    println("keySpecPublic: $keySpecPublic")
    return KeyFactory.getInstance("RSA").generatePublic(keySpecPublic) as RSAPublicKey
}