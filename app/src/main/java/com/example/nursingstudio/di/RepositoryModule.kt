package com.example.nursingstudio.di

import android.content.Context
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.data.repository.AuthRepository
import com.example.nursingstudio.data.repository.LeaderboardRepository
import com.example.nursingstudio.data.repository.ProfileRepository
import com.example.nursingstudio.data.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        dataStoreManager: DataStoreManager
    ): AuthRepository {
        return AuthRepository(context, firebaseAuth, firestore, dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideQuizRepository(
        firestore: FirebaseFirestore
    ): QuizRepository {
        return QuizRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideLeaderboardRepository(
        firestore: FirebaseFirestore
    ): LeaderboardRepository {
        return LeaderboardRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        dataStoreManager: DataStoreManager
    ): ProfileRepository {
        return ProfileRepository(firebaseAuth, firestore, dataStoreManager)
    }
}