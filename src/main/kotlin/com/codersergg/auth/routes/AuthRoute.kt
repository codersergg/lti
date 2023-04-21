package com.codersergg.auth.routes

import com.codersergg.auth.data.AuthDataSource
import com.codersergg.auth.data.models.UserInfo
import com.codersergg.auth.data.models.UserSession
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.p

fun Route.auth(authDataSource: AuthDataSource, httpClient: HttpClient) {
    authenticate("auth-oauth-google") {
        get("/login") {
            // Redirects to 'authorizeUrl' automatically
        }

        get("/callback") {
            val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
            call.sessions.set(UserSession(principal!!.state!!, principal.accessToken))
            val redirect = authDataSource.getState(principal.state!!)
            call.respondRedirect(redirect)
        }
    }
    get("/") {
        call.respondHtml {
            body {
                p {
                    a("/login") { +"Login with Google" }
                }
            }
        }
    }
    get("/{path}") {
        val userSession: UserSession? = call.sessions.get()
        if (userSession != null) {
            val userInfo: UserInfo = httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${userSession.token}")
                }
            }.body()
            call.respondText("Hello, ${userInfo.name}!")
        } else {
            val redirectUrl = URLBuilder("http://0.0.0.0:8080/login").run {
                parameters.append("redirectUrl", call.request.uri)
                build()
            }
            call.respondRedirect(redirectUrl)
        }
    }
}