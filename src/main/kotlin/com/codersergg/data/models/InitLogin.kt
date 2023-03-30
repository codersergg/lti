package com.codersergg.data.models

import io.ktor.server.sessions.*

data class InitLogin(
    val iss: String,
    val login_hint: String,
    val target_link_uri: String,
    val lti_message_hint: String? = null,
    val lti_deployment_id: String? = null,
    val client_id: String? = null,
    val session: CurrentSession
)