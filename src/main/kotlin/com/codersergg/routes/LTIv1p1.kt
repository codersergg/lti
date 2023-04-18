package com.codersergg.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import org.imsglobal.pox.IMSPOXRequest


suspend fun PipelineContext<Unit, ApplicationCall>.requestInitLoginV1p0(request: ApplicationRequest) {

    val isFieldsBlank = request.queryParameters["lti_message_type"].toString().isBlank() ||
            request.queryParameters["lti_version"].toString().isBlank() ||
            request.queryParameters["resource_link_id"].toString().isBlank()

    if (isFieldsBlank) {
        call.respond(HttpStatusCode.Conflict, "Fields must not be empty")
        return
    }
    if (!request.queryParameters["lti_message_type"].equals("basic-lti-launch-request")) {
        call.respond(HttpStatusCode.Conflict, "This is not a basic launch message")
        return
    }

    // verify OAuth1
    if (!verifyRequest(request)) {
        call.respond(HttpStatusCode.Conflict, "Unauthorized request")
        return
    }

    // grade
    garde(request)

    call.respondRedirect("https://infinite-lowlands-71677.herokuapp.com/redirect", false)
}

private fun verifyRequest(request: ApplicationRequest): Boolean {
    // to do verify
    return true
}

fun garde(request: ApplicationRequest) {
    val urlString = request.queryParameters["lis_outcome_service_url"]
    val publicKey = request.queryParameters["oauth_consumer_key"]
    val secretKey = "privateKey"
    val lisResultSourcedid = request.queryParameters["lis_result_sourcedid"]
    IMSPOXRequest.sendReplaceResult(urlString, publicKey, secretKey, lisResultSourcedid, "1.0")
}