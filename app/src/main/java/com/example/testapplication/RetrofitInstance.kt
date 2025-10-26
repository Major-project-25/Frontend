package com.example.testapplication

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    //val BASE_URL = "http://172.17.2.88:8000/"
    val BASE_URL = "http://172.17.0.176:8000/"

    // Custom Gson instance to handle UUIDs from the API
    private val gson = GsonBuilder()
        .registerTypeAdapter(java.util.UUID::class.java, UuidTypeAdapter())
        .create()

    // Custom OkHttpClient with longer timeouts
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.SECONDS) // Time to establish connection (10 min)
        .writeTimeout(600, TimeUnit.SECONDS)   // Time to write data (upload) (10 min)
        .readTimeout(600, TimeUnit.SECONDS)    // Time to read data (response) (10 min)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient) // Use the custom client
            .build()
            .create(ApiService::class.java)
    }
}

// Helper class to convert UUIDs correctly between JSON (String) and Kotlin (UUID)
class UuidTypeAdapter : com.google.gson.TypeAdapter<java.util.UUID>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: java.util.UUID?) {
        out.value(value?.toString())
    }
    override fun read(inReader: com.google.gson.stream.JsonReader): java.util.UUID? {
        if (inReader.peek() == com.google.gson.stream.JsonToken.NULL) {
            inReader.nextNull()
            return null
        }
        return java.util.UUID.fromString(inReader.nextString())
    }
}