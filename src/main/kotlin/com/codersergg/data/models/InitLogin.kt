package com.codersergg.data.models

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class InitLogin(
    @BsonId
    val id: ObjectId = ObjectId(),
    val iss: String,
    val login_hint: String,
    val target_link_uri: String
)