package com.codersergg.data

import com.codersergg.data.models.State

interface AuthenticationData {
    suspend fun isCorrectState(state: String): Boolean
    suspend fun putState(state: State): Boolean?
}