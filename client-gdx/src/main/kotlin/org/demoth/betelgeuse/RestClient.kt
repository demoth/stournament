package org.demoth.betelgeuse

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.davnokodery.rigel.model.LoginRequest
import org.davnokodery.rigel.model.LoginResponse

private const val SERVER_URL = "http://localhost:8080/login"

class RestClient {
    private val jsonType = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val client = OkHttpClient()

    fun login(loginRequest: LoginRequest, postLogin: (LoginResponse) -> Unit) {
        try {
            val request = Request.Builder()
                .post(mapper.writeValueAsString(loginRequest).toRequestBody(jsonType))
                .url(SERVER_URL)
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            println("Response: $responseBody")

            if (response.isSuccessful) {
                postLogin.invoke(mapper.readValue(responseBody!!))
            } else {
                println("Could not login")
            }
        } catch (e: Exception) {
            println("Could not login due to an exception: ${e.message}")
        }
    }

}
