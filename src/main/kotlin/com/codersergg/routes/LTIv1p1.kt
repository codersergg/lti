package com.codersergg.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import org.apache.commons.codec.binary.Hex
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.EncodedKeySpec
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
    val sig = parameters["oauth_signature"]
    val publicKey = parameters["oauth_consumer_key"]
    val signatureMethod = parameters["oauth_signature_method"]
    println(sig)

    // verify
    val mySig = Signature.getInstance("NONEwithRSA")
    mySig.initVerify(pubKeyFromString(publicKey))
    val isSigValid = mySig.verify(sig!!.toByteArray())

    println("verify: $isSigValid")

    call.respondRedirect("https://infinite-lowlands-71677.herokuapp.com/redirect", false)
}

fun pubKeyFromString(key: String?): PublicKey {
    val keyFactory = KeyFactory.getInstance("RSA")
    val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(Hex.decodeHex(key))
    return keyFactory.generatePublic(publicKeySpec)
}