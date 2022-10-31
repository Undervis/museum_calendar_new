package com.example.museum_calendar

object MonthAdapter {
    private val monthArray = arrayOf("Месяца", "Января", "Февраля", "Марта",
        "Апреля", "Мая", "Июня", "Июля", "Августа",
        "Сентября", "Октября", "Ноября", "Декабря")

    fun getMonthByInt(month: Int) : String {
        return monthArray[month]
    }
}