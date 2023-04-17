package com.codersergg.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureSerialization2() {
    install(ContentNegotiation) {
        xml()
    }
}
