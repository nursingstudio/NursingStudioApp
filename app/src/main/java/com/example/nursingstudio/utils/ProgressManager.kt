package com.example.nursingstudio.utils

import android.content.Context

object ProgressManager {

    private const val PREF = "progress"

    fun increment(context: Context, key: String) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val current = sp.getInt(key, 0)
        sp.edit().putInt(key, current + 1).apply()
    }

    fun get(context: Context, key: String): Int {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt(key, 0)
    }
}