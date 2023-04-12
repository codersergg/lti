package com.codersergg

import com.auth0.jwk.JwkProviderBuilder
import com.codersergg.data.MemoryAuthenticationData
import com.codersergg.data.MemoryInitLoginDataSource
import com.codersergg.data.MemoryTestDataSource
import com.codersergg.plugins.*
import io.ktor.server.application.*
import java.util.concurrent.TimeUnit

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    val initLoginDataSource = MemoryInitLoginDataSource()
    val testDataSource = MemoryTestDataSource()
    val authenticationData = MemoryAuthenticationData()
    val issuer = environment.config.property("jwt.issuer").getString()
    val privateKeyString = environment.config.property("jwt.privateKey").getString()
    val jwkProvider = JwkProviderBuilder(issuer)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    configureSerialization()
    configureMonitoring()
    configureRouting(initLoginDataSource, testDataSource, authenticationData, jwkProvider, privateKeyString)
}
