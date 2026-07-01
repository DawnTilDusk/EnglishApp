package com.example.seedie.domain.model

data class ActivityModuleSummary(
    val moduleId: String,
    val durationSec: Int
)

data class DailyActivityTotal(
    val date: String,
    val durationSec: Int
)

data class ActivityModuleMeta(
    val id: String,
    val label: String
)

object ActivityModuleIds {
    const val DASHBOARD = ActivityModule.Dashboard.id
    const val LEARNING_HUB = ActivityModule.LearningHub.id
    const val DATA_GARDEN = ActivityModule.DataGarden.id
    const val PROFILE = ActivityModule.Profile.id
    const val VOCABULARY_STUDY = ActivityModule.VocabularyStudy.id
    const val VOCABULARY_REVIEW = ActivityModule.VocabularyReview.id
    const val LISTENING = ActivityModule.ListeningPractice.id
    const val QUIZ = ActivityModule.VocabularyQuiz.id
    const val SHOP = ActivityModule.Shop.id
}

val ActivityTrackingModules = ActivityModule.values().map { module ->
    ActivityModuleMeta(id = module.id, label = module.displayName)
}

private val ActivityModuleLabelMap = ActivityTrackingModules.associate { it.id to it.label }

fun activityModuleLabel(moduleId: String): String {
    return ActivityModuleLabelMap[moduleId] ?: moduleId
}
