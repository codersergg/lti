package com.codersergg.data.models

data class State(
    val state: String,
    val nonce: String,
    val clientId: String,
    val endUserIdentifier: String,
    var lineitems: String? = null
)
