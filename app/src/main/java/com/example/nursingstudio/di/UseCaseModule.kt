package com.example.nursingstudio.di

import com.example.nursingstudio.data.repository.LeaderboardRepository
import com.example.nursingstudio.data.repository.QuizRepository
import com.example.nursingstudio.domain.usecase.GetQuizUseCase
import com.example.nursingstudio.domain.usecase.SubmitQuizResultUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetQuizUseCase(
        quizRepository: QuizRepository
    ): GetQuizUseCase {
        return GetQuizUseCase(quizRepository)
    }

    @Provides
    @Singleton
    fun provideSubmitQuizResultUseCase(
        leaderboardRepository: LeaderboardRepository
    ): SubmitQuizResultUseCase {
        return SubmitQuizResultUseCase(leaderboardRepository)
    }
}