package com.codersergg.routes

import com.codersergg.data.AuthenticationData
import com.codersergg.data.InitLoginDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.initiateLogin(
    initLoginDataSource: InitLoginDataSource,
    authenticationData: AuthenticationData
) {
    post("initiate-login") {

        // Индивидуальные параметры площадки

        // url запроса аутентификации в LMS, смотреть в настройках LMS
        val authUrl = "lti-test-connect.moodlecloud.com/mod/lti/auth.php"

        val request = call.request
        val formParameters = request.queryParameters
        if (formParameters["lti_version"] != null &&
            formParameters["lti_version"].equals("LTI-1p0")
        ) {
            requestInitLoginV1p0(request)
        } else if (formParameters["login_hint"] != null) {
            requestInitLoginV1p3(formParameters, initLoginDataSource, authenticationData, authUrl)
        } else {
            println("Format not supported")
            call.respond(HttpStatusCode.Conflict, "Format not supported")
            return@post
        }
    }
}

fun Route.redirect() {
    get("redirect") {
        // get the interactive from the repository
        val interactive = "src/main/resources/static/my_interactive_picture.html"
        call.respondFile(File(interactive))
    }
}

// Роут для проверки наличия запроса на аутентификацию
fun Route.getSavedInitiateLogin(
    initLoginDataSource: InitLoginDataSource
) {
    get("saved-initiate-login") {
        initLoginDataSource.getAll()
        call.respond(HttpStatusCode.OK, initLoginDataSource.getAll().toString())
    }
}