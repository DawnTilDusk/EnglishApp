package com.example.seedie.ui.screens.learning.assignments

import com.example.seedie.domain.model.PracticeAssignmentListItem
import com.example.seedie.domain.model.PracticeAssignmentMode

data class PracticeAssignmentArgs(
    val moduleId: String,
    val submissionId: String,
    val mode: PracticeAssignmentMode
)

enum class AssignmentListTab {
    Incomplete,
    Grading,
    Completed
}

data class AssignmentListUiState(
    val moduleId: String = "reading",
    val moduleTitle: String = "阅读训练",
    val showGradingTab: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedTab: AssignmentListTab = AssignmentListTab.Incomplete,
    val incomplete: List<PracticeAssignmentListItem> = emptyList(),
    val grading: List<PracticeAssignmentListItem> = emptyList(),
    val completed: List<PracticeAssignmentListItem> = emptyList()
)
