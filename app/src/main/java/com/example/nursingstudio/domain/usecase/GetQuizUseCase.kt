package com.example.nursingstudio.domain.usecase

import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuizMetadata
import com.example.nursingstudio.data.repository.QuizRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

data class QuizDataBundle(
    val metadata: QuizMetadata,
    val questions: List<QuestionItem>
)

/**
 * 🚀 2026 Gold Standard Domain UseCase
 * Encapsulates quiz fetching with parallel execution for speed and battery optimization.
 */
class GetQuizUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    suspend operator fun invoke(quizId: String): Result<QuizDataBundle> = coroutineScope {
        // Execute calls concurrently to reduce total waiting time
        val metaDeferred = async { quizRepository.getQuizMetadata(quizId) }
        val questionsDeferred = async { quizRepository.getQuizQuestions(quizId) }

        val metaResult = metaDeferred.await()
        val questionsResult = questionsDeferred.await()

        if (metaResult.isSuccess && questionsResult.isSuccess) {
            Result.success(
                QuizDataBundle(
                    metadata = metaResult.getOrThrow(),
                    questions = questionsResult.getOrThrow()
                )
            )
        } else {
            Result.failure(
                metaResult.exceptionOrNull()
                    ?: questionsResult.exceptionOrNull()
                    ?: Exception("Failed to fetch quiz data.")
            )
        }
    }
}