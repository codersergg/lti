package com.codersergg.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

suspend fun PipelineContext<Unit, ApplicationCall>.requestInitLoginV1p0(
    reqparameters: Parameters
) {

    val isFieldsBlank = reqparameters["lti_message_type"].toString().isBlank() ||
            reqparameters["lti_version"].toString().isBlank() ||
            reqparameters["resource_link_id"].toString().isBlank()

    if (isFieldsBlank) {
        call.respond(HttpStatusCode.Conflict, "Fields must not be empty")
        return
    }

    println(reqparameters.toString())

    call.respondRedirect("https://infinite-lowlands-71677.herokuapp.com/redirect", false)
}