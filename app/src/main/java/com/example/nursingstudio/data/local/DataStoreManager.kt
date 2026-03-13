package com.example.nursingstudio.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_MOBILE = stringPreferencesKey("user_mobile")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
    }

    suspend fun saveUser(name: String, mobile: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            prefs[USER_MOBILE] = mobile
        }
    }

    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userMobile: Flow<String?> = context.dataStore.data.map { it[USER_MOBILE] }
}