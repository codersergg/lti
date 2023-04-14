package com.codersergg.plugins

import com.auth0.jwk.JwkProvider
import com.codersergg.data.AuthenticationData
import com.codersergg.data.InitLoginDataSource
import com.codersergg.data.TestDataSource
import com.codersergg.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    initLoginDataSource: InitLoginDataSource,
    testDataSource: TestDataSource,
    authenticationData: AuthenticationData,
    jwkProvider: JwkProvider,
    privateKeyString: String
) {

    routing {
        initiateLogin(initLoginDataSource, authenticationData)
        initiateLoginV1p1(initLoginDataSource, authenticationData)
        getSavedInitiateLogin(initLoginDataSource)
        test(testDataSource)
        getAllHeaderValue(testDataSource)
        getAllEncoding(testDataSource)
        getSizeTestRequest(testDataSource)
        getAllReceiveText(testDataSource)
        authenticationResponsePost(authenticationData, jwkProvider, privateKeyString)
        redirect()
    }

}
