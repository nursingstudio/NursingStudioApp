package com.example.nursingstudio.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    // 2026 Standard: Provide Firebase instances here so Repository can use them
    @Provides
    @Singleton
    internal fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
   internal fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()


}