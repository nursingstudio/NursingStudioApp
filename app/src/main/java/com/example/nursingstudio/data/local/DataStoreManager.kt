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
        val SUBSCRIPTION_TYPE = stringPreferencesKey("subscription_type")
        val IS_MPIN_SET = booleanPreferencesKey("is_mpin_set")
        // 🚀 2026 GOLD STANDARD: Centralized Key Token Mapping
        val UNIQUE_NS_ID = stringPreferencesKey("unique_ns_id")
    }

    suspend fun saveUser(name: String, mobile: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            prefs[USER_MOBILE] = mobile
        }
    }

    // 🚀 NEW: Type-Safe Identity Resolution Persistent Logic
    suspend fun saveUniqueNsId(nsId: String) {
        context.dataStore.edit { prefs ->
            prefs[UNIQUE_NS_ID] = nsId
        }
    }

    suspend fun saveSubscription(type: String) {
        context.dataStore.edit { prefs ->
            prefs[SUBSCRIPTION_TYPE] = type
        }
    }

    suspend fun saveMpinStatus(isSet: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_MPIN_SET] = isSet
        }
    }

    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userMobile: Flow<String?> = context.dataStore.data.map { it[USER_MOBILE] }
    val subscriptionType: Flow<String> = context.dataStore.data.map { it[SUBSCRIPTION_TYPE] ?: "Free" }
    val isMpinSet: Flow<Boolean> = context.dataStore.data.map { it[IS_MPIN_SET] ?: false }

    // 🚀 FIXED: Unified Dynamic Identity Resolution Stream Flow Token
    val uniqueNsId: Flow<String?> = context.dataStore.data.map { it[UNIQUE_NS_ID] }
}