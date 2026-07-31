package com.example.seedie.domain.model

enum class ActivityModule(
    val id: String,
    val displayName: String
) {
    Dashboard("dashboard", "首页"),
    LearningHub("learning_hub", "学习中心"),
    DataGarden("data_garden", "数据花园"),
    Profile("profile", "个人页"),
    VocabularyStudy("vocabulary", "背单词"),
    VocabularyReview("vocabulary_review", "单词复习"),
    ListeningPractice("listening", "听力训练"),
    ReadingPractice("reading", "阅读训练"),
    WritingPractice("writing", "写作训练"),
    VocabularyQuiz("vocabulary_quiz", "词汇测验"),
    Shop("shop", "商店");

    companion object {
        fun fromPracticeSource(sourceModuleId: String): ActivityModule {
            return when (sourceModuleId) {
                "vocabulary_review" -> VocabularyReview
                else -> VocabularyStudy
            }
        }
    }
}
