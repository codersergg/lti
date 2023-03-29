package com.codersergg.plugins

import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.TestDataSource
import com.codersergg.routes.getAllTest
import com.codersergg.routes.getSavedInitiateLogin
import com.codersergg.routes.initiateLogin
import com.codersergg.routes.test
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    initLoginDataSource: InitLoginDataSource,
    testDataSource: TestDataSource
) {

    routing {
        initiateLogin(initLoginDataSource)
        getSavedInitiateLogin(initLoginDataSource)
        test(testDataSource)
        getAllTest(testDataSource)
    }

}
