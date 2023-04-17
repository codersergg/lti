package com.codersergg.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.*


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
    val signature = parameters["oauth_signature"]
    val publicKey = parameters["oauth_consumer_key"]
    val signatureMethod = parameters["oauth_signature_method"]
    println(signature)

    val publicBytes: ByteArray = Base64.getDecoder().decode(publicKey)
    val keySpec = X509EncodedKeySpec(publicBytes)
    val keyFactory = KeyFactory.getInstance(signatureMethod)
    val pubKey = keyFactory.generatePublic(keySpec)

    val sig = Signature.getInstance(parameters["oauth_signature"])
    sig.initVerify(pubKey)
    val verify = sig.verify(sig.sign())

    println("verify: $verify")

    call.respondRedirect("https://infinite-lowlands-71677.herokuapp.com/redirect", false)
}