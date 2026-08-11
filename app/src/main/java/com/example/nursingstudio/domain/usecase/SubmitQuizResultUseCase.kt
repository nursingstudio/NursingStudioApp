package com.example.nursingstudio.domain.usecase

import com.example.nursingstudio.data.model.QuizResultData
import com.example.nursingstudio.data.repository.LeaderboardRepository
import javax.inject.Inject

class SubmitQuizResultUseCase @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository
) {
    suspend operator fun invoke(resultData: QuizResultData): Result<Unit> {
        return leaderboardRepository.submitQuizScore(resultData)
    }
}