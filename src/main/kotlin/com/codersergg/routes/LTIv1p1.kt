package com.codersergg.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import org.imsglobal.pox.IMSPOXRequest
import kotlin.random.Random


suspend fun PipelineContext<Unit, ApplicationCall>.requestInitLoginV1p0(parameters: Parameters) {

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

    // verify OAuth1
    if (!verifyParam(parameters)) {
        call.respond(HttpStatusCode.Conflict, "Unauthorized request")
        return
    }

    // grade
    garde(parameters)

    call.respondRedirect("https://infinite-lowlands-71677.herokuapp.com/redirect", false)
}

private fun verifyParam(parameters: Parameters): Boolean {
    // to do verify
    // https://api.ktor.io/ktor-server/ktor-server-plugins/ktor-server-auth/io.ktor.server.auth/-o-auth-server-settings/-o-auth1a-server-settings/index.html
    if (parameters["oauth_consumer_key"] == null) return false
    return true
}

fun garde(request: Parameters) {
    val urlString = request["lis_outcome_service_url"]
    val publicKey = request["oauth_consumer_key"]
    val secretKey = "privateKey"
    val lisResultSourcedid = request["lis_result_sourcedid"]
    // random for testing
    val grade = "%.1f".format(Random.nextDouble(from = 0.0, until = 1.01))
    IMSPOXRequest.sendReplaceResult(urlString, publicKey, secretKey, lisResultSourcedid, grade)
}