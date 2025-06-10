package com.meat.meatdash.sharedpref

import android.content.Context

object PrefsHelper {
    private const val PREFS_NAME = "meatdash_user_prefs"

    fun saveString(context: Context, key: String, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, null)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
