package com.codersergg.data.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class InitLoginRequest(
    val iss: String,
    val login_hint: String,
    val target_link_uri: String,
    val lti_message_hint: String? = null,
    val lti_deployment_id: String? = null,
    val client_id: String? = null,
)
