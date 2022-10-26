package com.example.museum_calendar

import retrofit2.Call
import retrofit2.http.GET

interface RetrofitServices {
    @GET("get_dates")
    fun getDatesList(): Call<ArrayList<DateClass>>
}