package com.luoyunxi.applistviewer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private val items = mutableListOf<AppInfo>()

    fun submitList(list: List<AppInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.appName)
        val packageName: TextView = view.findViewById(R.id.packageName)
        val moreInfo: TextView = view.findViewById(R.id.moreInfo)
        val appVersion: TextView = view.findViewById(R.id.appVersion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = items[position]
        holder.appName.text = app.appName
        holder.packageName.text = app.packageName
        holder.moreInfo.text = app.moreInfo
        holder.appVersion.text = app.appVersion
    }
}

