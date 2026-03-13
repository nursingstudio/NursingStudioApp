package com.example.nursingstudio.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.local.DataStoreManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: FirebaseFirestore,
    private val dataStore: DataStoreManager
) : ViewModel() {

    fun syncUserData(uid: String) {
        repository.collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("fullName") ?: ""
                    val mobile = doc.getString("mobile") ?: ""
                    viewModelScope.launch {
                        dataStore.saveUser(name, mobile)
                    }
                }
            }
    }
}