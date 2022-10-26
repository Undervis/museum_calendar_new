package com.example.museum_calendar

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient {
    private var retrofit: Retrofit? = null
    var baseUrl = ""

    fun getClient(baseUrl: String): Retrofit {
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        this.baseUrl = baseUrl
        return retrofit!!
    }
}