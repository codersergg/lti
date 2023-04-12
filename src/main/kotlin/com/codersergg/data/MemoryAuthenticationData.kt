package com.codersergg.data

import com.codersergg.data.models.State

class MemoryAuthenticationData(private val map: MutableMap<String, State> = HashMap()) :
    AuthenticationData {
    override suspend fun isCorrectState(state: String): Boolean {
        return map[state]?.state.equals(state)
    }

    override suspend fun getState(state: String?): State {
        return map[state]!!
    }

    override suspend fun putState(state: State): Boolean {
        return map.put(state.state, state) != null
    }

}