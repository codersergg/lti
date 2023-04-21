package com.codersergg.auth.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class UserSession(val state: String, val token: String)
@Serializable
data class UserInfo(
    val id: String,
    val name: String,
    @SerialName("given_name") val givenName: String,
    @SerialName("family_name") val familyName: String,
    val picture: String,
    val locale: String
)
