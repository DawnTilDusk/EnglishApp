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
    val DASHBOARD = ActivityModule.Dashboard.id
    val LEARNING_HUB = ActivityModule.LearningHub.id
    val DATA_GARDEN = ActivityModule.DataGarden.id
    val PROFILE = ActivityModule.Profile.id
    val VOCABULARY_STUDY = ActivityModule.VocabularyStudy.id
    val VOCABULARY_REVIEW = ActivityModule.VocabularyReview.id
    val LISTENING = ActivityModule.ListeningPractice.id
    val READING = ActivityModule.ReadingPractice.id
    val QUIZ = ActivityModule.VocabularyQuiz.id
    val SHOP = ActivityModule.Shop.id
}

val ActivityTrackingModules = ActivityModule.values().map { module ->
    ActivityModuleMeta(id = module.id, label = module.displayName)
}

private val ActivityModuleLabelMap = ActivityTrackingModules.associate { it.id to it.label }

fun activityModuleLabel(moduleId: String): String {
    return ActivityModuleLabelMap[moduleId] ?: moduleId
}
