package com.codersergg.plugins

import com.codersergg.data.InitLoginDataSource
import com.codersergg.routes.initiateLogin
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    initLoginDataSource: InitLoginDataSource
) {

    routing {
        initiateLogin(initLoginDataSource)
    }

}
