package com.codersergg.routes

import com.codersergg.data.models.InitLogin
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


suspend fun PipelineContext<Unit, ApplicationCall>.requestInitLoginV1p0(
    parameters: Parameters
) {

    val isFieldsBlank = parameters["lti_message_type"].toString().isBlank() ||
            parameters["lti_version"].toString().isBlank() ||
            parameters["resource_link_id"].toString().isBlank()

    if (isFieldsBlank) {
        call.respond(HttpStatusCode.Conflict, "Fields must not be empty")
        return
    }
    if (!parameters["lti_message_type"].equals("basic-lti-launch-request")) {
        call.respond(HttpStatusCode.Conflict, "This is not a basic launch message")
        return
    }
    println(parameters.toString())

    // to do verify
    if (!verifyRequest(parameters)) {
        call.respond(HttpStatusCode.Conflict, "Unauthorized request")
        return
    }

    // to do grade
    garde(parameters)

    call.respondRedirect("https://infinite-lowlands-71677.herokuapp.com/redirect", false)
}

suspend fun garde(parameters: Parameters): HttpResponse {

    val lisResultSourcedid = parameters["lis_result_sourcedid"]
    val grade = "1.0"
    val xmlStr = """
    <?xml version = "1.0" encoding = "UTF-8"?>
    <imsx_POXEnvelopeRequest xmlns = "http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0">
      <imsx_POXHeader>
        <imsx_POXRequestHeaderInfo>
          <imsx_version>V1.0</imsx_version>
          <imsx_messageIdentifier>999999123</imsx_messageIdentifier>
        </imsx_POXRequestHeaderInfo>
      </imsx_POXHeader>
      <imsx_POXBody>
        <replaceResultRequest>
          <resultRecord>
            <sourcedGUID>
              <sourcedId>$lisResultSourcedid</sourcedId>
            </sourcedGUID>
            <result>
              <resultScore>
                <language>en</language>
                <textString>$grade</textString>
              </resultScore>
            </result>
          </resultRecord>
        </replaceResultRequest>
      </imsx_POXBody>
    </imsx_POXEnvelopeRequest>
""".trimIndent()

    val status = HttpClient() {
        /*install(ContentNegotiation) {
            xml(format = XML {
                xmlDeclMode = XmlDeclMode.Charset
            })
        }*/
    }.use { client ->
        client.post(
            url {
                host = parameters["lis_outcome_service_url"].toString()
            }
        ) {
            headers {
                append(HttpHeaders.ContentType, "application/xml")
                append(HttpHeaders.Accept, "application/xml")
            }
            setBody {
                InitLogin("1", "2", "3")
            }
        }
    }
    println("status: $status")
    return status
}

private fun verifyRequest(parameters: Parameters): Boolean {
    val sig = parameters["oauth_signature"]
    val publicKey = parameters["oauth_consumer_key"]
    val signatureMethod = parameters["oauth_signature_method"]
    println("sig: $sig")
    println("publicKey: $publicKey")
    println("signatureMethod: $signatureMethod")

    // verify
    val encodingAlgorithm = "HmacSHA1"
    val secretKey = "privateKey"

    val sha1Hmac = Mac.getInstance(encodingAlgorithm)
    val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), encodingAlgorithm)
    sha1Hmac.init(secretKeySpec)

    val hash = sha1Hmac.doFinal(sig!!.encodeToByteArray()) // params used to sing
    val message = Base64.getEncoder().encodeToString(hash)

    println("hash: $hash")
    println("message: $message")

    return true
}