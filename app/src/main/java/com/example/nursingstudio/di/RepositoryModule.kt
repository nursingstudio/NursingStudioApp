package com.example.nursingstudio.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 🚀 2026 GOLD STANDARD DEPENDENCY INJECTION
 * All repositories (AuthRepository, QuizRepository, LeaderboardRepository, ProfileRepository)
 * use @Inject constructor with @Singleton annotations directly on their class declarations.
 * Hilt injects them automatically without requiring redundant @Provides functions.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Retain module for future interface-to-implementation @Binds declarations if needed.
}