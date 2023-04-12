package com.codersergg.data

import com.codersergg.data.models.State

interface AuthenticationData {
    suspend fun isCorrectState(state: String): Boolean
    suspend fun getState(state: String?): State
    suspend fun putState(state: State): Boolean?
}