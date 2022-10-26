package com.example.museum_calendar

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import okio.Utf8

class RecyclerAdapter (private val data: List<DateClass>):RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val imPhoto: ImageView = itemView.findViewById(R.id.imPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.date_item, parent, false)
        return ViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = data[position]
        holder.tvDate.tag = date.id
        if (date.year > 0) {
            holder.tvDate.text = "${date.year}"
        }
        if (date.year > 0 && date.month > 0) {
            holder.tvDate.text = "${MonthAdapter.getMonthByInt(date.month)} ${date.year}"
        }
        if (date.year > 0 && date.month > 0 && date.day > 0) {
            holder.tvDate.text = "${date.day} ${MonthAdapter.getMonthByInt(date.month)} ${date.year}"
        }

        holder.tvTitle.text = date.title
        holder.tvDescription.text = date.description
        holder.imPhoto.clipToOutline = true
        var photoUrl = "${RetrofitClient.baseUrl}${date.photo}"
        if (photoUrl.contains("\uFEFF")) {
            photoUrl = photoUrl.replace("\uFEFF", "")
        }
        try {
            Picasso.get().load(photoUrl).into(holder.imPhoto)
        } catch (E: IllegalArgumentException) {
            Log.i("Picasso", E.message.toString())
        }
    }

    override fun getItemCount() = data.size
}