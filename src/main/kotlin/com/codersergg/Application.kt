package com.codersergg

import com.codersergg.auth.data.MemoryAuthDataSource
import com.codersergg.auth.plugins.authOauthGoogle
import com.codersergg.data.MemoryAuthenticationData
import com.codersergg.data.MemoryInitLoginDataSource
import com.codersergg.data.MemoryTestDataSource
import com.codersergg.plugins.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    val initLoginDataSource = MemoryInitLoginDataSource()
    val testDataSource = MemoryTestDataSource()
    val authenticationData = MemoryAuthenticationData()
    /*val issuer = environment.config.property("jwt.issuer").getString()
    val privateKeyString = environment.config.property("jwt.privateKey").getString()*/
    val authDataSource = MemoryAuthDataSource()

    /*val jwkProvider = JwkProviderBuilder(issuer)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()*/

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    authOauthGoogle(
        authDataSource,
        httpClient
    )

    configureSerialization()
    configureMonitoring()
    /*   configureRouting(
           initLoginDataSource,
           testDataSource,
           authenticationData,
           jwkProvider,
           privateKeyString,
           authDataSource,
           httpClient
       )*/
    configureRouting(
        initLoginDataSource,
        testDataSource,
        authenticationData,
        authDataSource,
        httpClient
    )
}
