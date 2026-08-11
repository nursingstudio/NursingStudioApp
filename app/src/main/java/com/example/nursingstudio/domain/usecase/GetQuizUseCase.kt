package com.example.nursingstudio.domain.usecase

import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuizMetadata
import com.example.nursingstudio.data.repository.QuizRepository
import javax.inject.Inject

data class QuizDataBundle(
    val metadata: QuizMetadata,
    val questions: List<QuestionItem>
)

class GetQuizUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    suspend operator fun invoke(quizId: String): Result<QuizDataBundle> {
        val metaResult = quizRepository.getQuizMetadata(quizId)
        val questionsResult = quizRepository.getQuizQuestions(quizId)

        return if (metaResult.isSuccess && questionsResult.isSuccess) {
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