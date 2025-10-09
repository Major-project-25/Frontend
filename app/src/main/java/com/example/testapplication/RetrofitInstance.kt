package com.example.testapplication

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // IMPORTANT: Use 10.0.2.2 for the Android Emulator.
    // If using a real phone, replace this with your computer's local IP address.
    //private const val BASE_URL = "http://10.100.211.210:8000/"

    //IMPORTANT:
    // Where i am supposed to change
    //1)RetrofitInstance
    //2)Ktorwebsocketservice
    //3)Generalinterfacescreen

    //private const val BASE_URL = "http://172.17.6.125:8000/"
    private const val BASE_URL = "http://172.17.2.88:8000/"
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