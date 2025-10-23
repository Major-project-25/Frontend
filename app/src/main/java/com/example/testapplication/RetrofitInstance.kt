package com.example.testapplication

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // FIX: Define BASE_URL as a standard public property of the object.
    //val BASE_URL = "http://172.17.2.88:8000/"
    val BASE_URL = "http://10.216.235.210:8000/"

    // Custom Gson instance to handle UUIDs from the API
    private val gson = GsonBuilder()
        .registerTypeAdapter(java.util.UUID::class.java, UuidTypeAdapter())
        .create()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
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
