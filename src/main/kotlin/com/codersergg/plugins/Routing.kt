package com.codersergg.plugins

import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.TestDataSource
import com.codersergg.routes.*
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
        getAllHeaderValue(testDataSource)
        getAllEncoding(testDataSource)
        getSizeTestRequest(testDataSource)
        getAllReceiveText(testDataSource)
        authenticationResponsePost()
        redirectGet()
        getPublicKey()
        getPublicKeyPost()
    }

}
