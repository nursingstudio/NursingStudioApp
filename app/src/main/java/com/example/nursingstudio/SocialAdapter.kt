package com.example.nursingstudio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class SocialAdapter(
    context: Context,
    private val items: List<SocialItem>
) : ArrayAdapter<SocialItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.social_item, parent, false)

        val item = items[position]

        // 👉 Yahi line hai:
        val icon = view.findViewById<ImageView>(R.id.icon)
        val title = view.findViewById<TextView>(R.id.title)

        icon.setImageResource(item.iconRes)
        title.text = item.title

        return view
    }
}
