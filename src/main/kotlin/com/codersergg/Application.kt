package com.codersergg

import com.codersergg.data.MemoryInitLoginDataSource
import com.codersergg.data.MemoryTestDataSource
import com.codersergg.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    val initLoginDataSource = MemoryInitLoginDataSource()
    val testDataSource = MemoryTestDataSource()

    configureSerialization()
    configureMonitoring()
    configureRouting(initLoginDataSource = initLoginDataSource, testDataSource = testDataSource)
    configureSecurity()
}
