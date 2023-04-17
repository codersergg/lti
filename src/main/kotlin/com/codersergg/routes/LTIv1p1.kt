package com.codersergg.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import org.imsglobal.pox.IMSPOXRequest
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

    // to do verify
    if (!verifyRequest(parameters)) {
        call.respond(HttpStatusCode.Conflict, "Unauthorized request")
        return
    }

    // to do grade
    garde(parameters)

    call.respondRedirect("https://infinite-lowlands-71677.herokuapp.com/redirect", false)
}

fun garde(parameters: Parameters) {

    val urlString = parameters["lis_outcome_service_url"]
    val publicKey = parameters["oauth_consumer_key"]
    val secretKey = "privateKey"
    val lisResultSourcedid = parameters["lis_result_sourcedid"]
    IMSPOXRequest.sendReplaceResult(urlString, publicKey, secretKey, lisResultSourcedid, "0.5")
}

private fun verifyRequest(parameters: Parameters): Boolean {
    val sig = parameters["oauth_signature"]
    val publicKey = parameters["oauth_consumer_key"]
    val signatureMethod = parameters["oauth_signature_method"]
    println("sig: $sig")
    println("publicKey: $publicKey")
    println("signatureMethod: $signatureMethod")

    // verify
    val (hash, message) = getSing(sig)

    println("hash: $hash")
    println("message: $message")

    return true
}

private fun getSing(sig: String?): Pair<ByteArray, String> {
    val encodingAlgorithm = "HmacSHA1"
    val secretKey = "privateKey"

    val sha1Hmac = Mac.getInstance(encodingAlgorithm)
    val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), encodingAlgorithm)
    sha1Hmac.init(secretKeySpec)

    val hash = sha1Hmac.doFinal(sig!!.encodeToByteArray()) // params used to sing
    val message = Base64.getEncoder().encodeToString(hash)
    return Pair(hash, message)
}