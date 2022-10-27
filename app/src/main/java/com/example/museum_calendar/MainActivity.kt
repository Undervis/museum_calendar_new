package com.example.museum_calendar

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private var dates: ArrayList<DateClass> = ArrayList()
    private var searchData: ArrayList<DateClass> = ArrayList()
    private var url: String = ""
    private var secretKey: String? = null

    private var rvDates: RecyclerView? = null
    private var pbLoading: ProgressBar? = null
    private var tvConnectionError: TextView? = null

    val NO_URL_DIALOG: Int = 0
    val CONNECTION_ERROR_DIALOG: Int = 1
    val SECRET_KEY_DIALOG: Int = 2
    val SET_SECRET_KEY_DIALOG: Int = 3
    val NEW_URL_DIALOG: Int = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = resources.getColor(R.color.base_dark)

        rvDates = findViewById(R.id.rvDates)
        pbLoading = findViewById(R.id.pbLoading)
        tvConnectionError = findViewById(R.id.tvConnectionError)

        val prefs = getSharedPreferences("url", Context.MODE_PRIVATE)
        secretKey = prefs.getString("secret_key", "museum_1901")
        // prefs.edit().remove("server_url").apply()
        if (prefs.contains("server_url")) {
            loadData(prefs)
        } else {
            callingDialog(this, NO_URL_DIALOG, prefs)
        }

        val cal = Calendar.getInstance()
        val spMonth: Spinner = findViewById(R.id.spMonth)
        val edDay: EditText = findViewById(R.id.edDay)
        val edYear: EditText = findViewById(R.id.edYear)
        val btnSearch: ImageButton = findViewById(R.id.btnSearch)

        val months = MonthAdapter.getMonthArray()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, months)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spMonth.adapter = adapter

        btnSearch.setOnClickListener {
            searchData.clear()
            var mode: Int = 0
            var year: Int = 0
            var month: Int = 0
            var day: Int = 0
            if (edYear.text.isNotEmpty()) {
                mode = 1
                year = edYear.text.toString().toInt()
            }
            if (edYear.text.isNotEmpty() && spMonth.selectedItemPosition > 0) {
                mode = 2
                year = edYear.text.toString().toInt()
                month = spMonth.selectedItemPosition
            }
            if (edYear.text.isNotEmpty() && spMonth.selectedItemPosition > 0 && edDay.text.isNotEmpty()) {
                mode = 3
                year = edYear.text.toString().toInt()
                month = spMonth.selectedItemPosition
                day = edDay.text.toString().toInt()
            }
            if (mode > 0) {
                for (i in dates) {
                    when (mode) {
                        1 ->
                            if (year == i.year) {
                                searchData.add(i)
                            }
                        2 ->
                            if (year == i.year && month == i.month) {
                                searchData.add(i)
                            }
                        3 ->
                            if (year == i.year && month == i.month && day == i.day) {
                                searchData.add(i)
                            }
                    }

                }
                if (searchData.isEmpty()) {
                    var firstDate = ""
                    var secondDate = ""
                    for (i in dates) {
                        if (year > i.year) {
                            if (i.month > 0) {
                                firstDate =
                                    "${MonthAdapter.getMonthByInt(i.month)} ${i.year}"
                            }
                            if (i.month > 0 && i.day > 0) {
                                firstDate =
                                    "${i.day} ${MonthAdapter.getMonthByInt(i.month)} ${i.year}"
                            }
                            break
                        }
                        if (year < i.year) {
                            if (i.month > 0) {
                                secondDate =
                                    "${MonthAdapter.getMonthByInt(i.month)} ${i.year}"
                            }
                            if (i.month > 0 && i.day > 0) {
                                secondDate =
                                    "${i.day} ${MonthAdapter.getMonthByInt(i.month)} ${i.year}"
                            }
                        }
                    }
                    Toast.makeText(
                        this,
                        "Ближайшие даты $firstDate и $secondDate",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    updateRecyclerView(searchData)
                }
            } else {
                updateRecyclerView(dates)
            }
        }
    }

    fun updateRecyclerView(dates: ArrayList<DateClass>) {
        rvDates?.layoutManager = LinearLayoutManager(this)
        val recyclerAdapter = RecyclerAdapter(dates)
        rvDates?.adapter = recyclerAdapter
    }

    fun loadData(prefs: SharedPreferences) {
        url = prefs.getString("server_url", "None").toString()
        val baseUrl: String = "$url/"
        val retrofitServices: RetrofitServices =
            RetrofitClient.getClient(baseUrl).create(RetrofitServices::class.java)

        pbLoading?.visibility = View.VISIBLE

        retrofitServices.getDatesList()
            .enqueue(object : retrofit2.Callback<ArrayList<DateClass>> {
                override fun onResponse(
                    call: Call<ArrayList<DateClass>>,
                    response: Response<ArrayList<DateClass>>
                ) {
                    val items = response.body()
                    if (items != null) {
                        dates = items
                        updateRecyclerView(dates)
                        pbLoading?.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<ArrayList<DateClass>>, t: Throwable) {
                    Log.e("retrofit", t.message.toString())
                    callingDialog(this@MainActivity, CONNECTION_ERROR_DIALOG, prefs)
                }
            })
    }

    private fun testConnection(url: String): Boolean {
        val baseUrl: String = "$url/"
        val retrofitServices: RetrofitServices =
            RetrofitClient.getClient(baseUrl).create(RetrofitServices::class.java)

        return try {
            val request = retrofitServices.getDatesList().execute()
            Log.i("retrofit", request.body().toString())
            request.body()?.isNotEmpty() == true
        } catch (E: IOException) {
            Log.e("retrofit", E.message.toString())
            false
        }
    }

    @SuppressLint("SetTextI18n")
    fun callingDialog(context: Context, mode: Int, prefs: SharedPreferences): Boolean {

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_layout)

        var result: Boolean = true

        val btnAccept: TextView = dialog.findViewById(R.id.btnAccept)
        val btnCancel: TextView = dialog.findViewById(R.id.btnCancel)
        val tvGeneralMessage: TextView = dialog.findViewById(R.id.tvMessageDialog)
        val tvSecondMessage: TextView = dialog.findViewById(R.id.tvMessageScnd)
        val edTextField: EditText = dialog.findViewById(R.id.edTextField)
        val edConfirmSK: EditText = dialog.findViewById(R.id.edConfirmSK)
        val imDialogIcon: ImageView = dialog.findViewById(R.id.imDialogIcon)
        val tvErrorMsg: TextView = dialog.findViewById(R.id.tvErrorMsg)
        val pbConnection: ProgressBar = dialog.findViewById(R.id.pbConnection)

        if (mode == NO_URL_DIALOG || mode == NEW_URL_DIALOG) {
            if (mode == NO_URL_DIALOG) {
                tvGeneralMessage.text = "Не задан URL для подключения к серверу"
                tvSecondMessage.text = "Укажите новый адресс для подключения"
            }
            if (mode == NEW_URL_DIALOG) {
                tvGeneralMessage.text = "Не удалось подключиться к серверу"
                tvSecondMessage.text = "Укажите новый адресс для подключения"
            }

            btnAccept.setOnClickListener {
                if (edTextField.text.isNotEmpty()) {
                    var newUrl = edTextField.text.toString()
                    if (!newUrl.contains("http://") || !newUrl.contains("https://")) {
                        newUrl = "http://$newUrl"
                    }

                    pbConnection.visibility = View.VISIBLE
                    tvErrorMsg.visibility = View.INVISIBLE

                    val test = kotlinx.coroutines.MainScope()
                    test.launch {
                        var response = false
                        withContext(IO) { response = testConnection(newUrl) }
                        if (response) {
                            pbConnection.visibility = View.INVISIBLE
                            result = if (mode == NO_URL_DIALOG) {
                                if (callingDialog(context, SET_SECRET_KEY_DIALOG, prefs)) {
                                    prefs.edit().putString("server_url", newUrl).apply()
                                    loadData(prefs)
                                    true
                                } else {
                                    false
                                }
                            } else {
                                prefs.edit().putString("server_url", newUrl).apply()
                                loadData(prefs)
                                true
                            }
                            dialog.dismiss()
                        } else {
                            pbConnection.visibility = View.INVISIBLE
                            tvErrorMsg.visibility = View.VISIBLE
                            tvErrorMsg.text = "Не удалось подключиться"
                        }
                    }
                    return@setOnClickListener
                } else {
                    tvErrorMsg.visibility = View.VISIBLE
                    tvErrorMsg.text = "Поле пустое"
                }

            }
        }

        if (mode == CONNECTION_ERROR_DIALOG) {
            tvGeneralMessage.text = "Не удалось подключиться к серверу"
            tvSecondMessage.text = "Хотите изменить адресс подключения?"
            edTextField.visibility = View.GONE

            btnAccept.setOnClickListener {
                result = callingDialog(context, SECRET_KEY_DIALOG, prefs)
                dialog.dismiss()
                return@setOnClickListener
            }
        }

        if (mode == SECRET_KEY_DIALOG) {
            tvGeneralMessage.text = "Введите секретный ключ"
            tvSecondMessage.visibility = View.GONE
            imDialogIcon.setImageDrawable(resources.getDrawable(R.drawable.key_icon))

            edTextField.hint = "Секретный ключ"
            edTextField.transformationMethod = PasswordTransformationMethod.getInstance()

            btnAccept.text = "Подтвердить"
            btnAccept.setOnClickListener {
                if (edTextField.text.isNotEmpty()) {
                    if (edTextField.text.toString() == secretKey) {
                        result = callingDialog(context, NEW_URL_DIALOG, prefs)
                        dialog.dismiss()
                        return@setOnClickListener
                    } else {
                        edTextField.text = edConfirmSK.text
                        tvErrorMsg.visibility = View.VISIBLE
                        tvErrorMsg.text = "Неверный секретный ключ"
                    }
                } else {
                    tvErrorMsg.visibility = View.VISIBLE
                    tvErrorMsg.text = "Поле пустое"
                }
            }
        }

        if (mode == SET_SECRET_KEY_DIALOG) {
            tvGeneralMessage.text =
                "В случае сбоя подключения, понадобится секретный ключ для изменения адреса подключения." +
                        "\nЗапомните его, изменить или восстановить секретный ключ невозможно"
            tvSecondMessage.text = "Введите новый секретный ключ"
            imDialogIcon.setImageDrawable(resources.getDrawable(R.drawable.key_icon))

            edTextField.hint = "Секретный ключ"
            edConfirmSK.hint = "Подтвердите секретный ключ"
            edConfirmSK.visibility = View.VISIBLE
            btnCancel.visibility = View.GONE
            edTextField.transformationMethod = PasswordTransformationMethod.getInstance()
            edConfirmSK.transformationMethod = PasswordTransformationMethod.getInstance()

            btnAccept.text = "Подтвердить"
            btnAccept.setOnClickListener {
                if (edTextField.text.isNotEmpty() && edConfirmSK.text.isNotEmpty()) {
                    if (edConfirmSK.text.toString() == edTextField.text.toString()) {
                        prefs.edit().putString("secret_key", edTextField.text.toString()).apply()
                        result = true
                        dialog.dismiss()
                        loadData(prefs)
                        return@setOnClickListener
                    } else {
                        tvErrorMsg.visibility = View.VISIBLE
                        tvErrorMsg.text = "Ключи не совпадают"
                    }
                } else {
                    tvErrorMsg.visibility = View.VISIBLE
                    tvErrorMsg.text = "Одно из полей пустое"
                }
            }
        }

        btnCancel.setOnClickListener {
            result = false
            dialog.dismiss()
            return@setOnClickListener
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        return result
    }
}