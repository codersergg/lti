package com.codersergg.auth.plugins

import com.codersergg.auth.data.AuthDataSource
import com.codersergg.auth.data.models.UserSession
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*

fun Application.authOauthGoogle(
    authDataSource: AuthDataSource,
    httpClient: HttpClient
) {
    install(Sessions) {
        cookie<UserSession>("user_session")
    }
    install(Authentication) {
        oauth("auth-oauth-google") {
            urlProvider = { "https://infinite-lowlands-71677.herokuapp.com/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("GOOGLE_CLIENT_ID"),
                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
                    extraAuthParameters = listOf("access_type" to "offline"),
                    onStateCreated = { call, state ->
                        val queryParameters = call.request.queryParameters
                        println("queryParameters: ${queryParameters["redirectUrl"]}")
                        authDataSource.putState(
                            state,
                            queryParameters["redirectUrl"]!!
                        )
                    }
                )
            }
            client = httpClient
        }
    }
}