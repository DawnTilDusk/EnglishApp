package com.example.seedie.ui.screens.main
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.seedie.ui.components.BottomNavigationBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.seedie.ui.screens.learning.LearningHubScreen
import com.example.seedie.ui.screens.learning.ModuleConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import com.example.seedie.ui.screens.dashboard.DashboardScreen
import com.example.seedie.ui.screens.profile.ProfileScreen
import com.example.seedie.ui.screens.garden.DataGardenScreen
import com.example.seedie.ui.components.CustomIndicatorPanel
import com.example.seedie.domain.model.StudyResult
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeMode
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onOpenVocabularyStudy: (VocabularyPracticeArgs) -> Unit,
    onOpenVocabularyReview: (VocabularyPracticeArgs) -> Unit,
    onOpenListeningPractice: () -> Unit,
    onOpenVocabularyQuiz: () -> Unit,
    onOpenShop: () -> Unit,
    pendingStudyResult: StudyResult?,
    onStudyResultConsumed: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val vocabularyEntryState by viewModel.vocabularyEntryState.collectAsState()
    var showReviewChoiceDialog by remember { mutableStateOf(false) }
    var dataGardenEnterKey by remember { mutableStateOf(0) }

    LaunchedEffect(pendingStudyResult) {
        pendingStudyResult?.let { result ->
            viewModel.handleStudyResult(result)
            snackbarHostState.showSnackbar(
                when (result.moduleId) {
                    "vocabulary_review" -> "单词复习完成：+${result.earnedTokens} 代币"
                    "listening" -> "听力训练完成：+${result.earnedTokens} 代币"
                    "quiz" -> {
                        val estimateText = result.estimatedVocabulary?.let { "估算词汇量约 $it，" } ?: ""
                        "词汇测验完成：${estimateText}+${result.earnedTokens} 代币"
                    }
                    else -> "背单词完成：+${result.earnedTokens} 代币，掌握 ${result.vocabularyDelta} 个单词"
                }
            )
            onStudyResultConsumed()
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page == 2) {
                dataGardenEnterKey += 1
            }
        }
    }

    val learningModules = listOf(
        ModuleConfig(
            "vocabulary",
            "背单词",
            vocabularyEntryState.defaultSubtitle,
            Icons.AutoMirrored.Filled.MenuBook,
            isAvailable = true,
            hasNewContent = false
        ),
        ModuleConfig(
            "vocabulary_review",
            "单词复习",
            "待复习 ${vocabularyEntryState.pendingReviewCount} 个词",
            Icons.Default.Quiz,
            isAvailable = true,
            hasNewContent = vocabularyEntryState.shouldShowReviewBadge
        ),
        ModuleConfig("grammar", "语法", "句型结构突破", Icons.Default.School),
        ModuleConfig("speaking", "口语跟读", "AI 智能跟读练习", Icons.Default.Mic),
        ModuleConfig(
            "quiz",
            "词汇测验",
            "检验学习成果",
            Icons.Default.Quiz,
            isAvailable = true
        ),
        ModuleConfig("textbook", "教材训练", "同步课堂进度", Icons.Default.Book),
        ModuleConfig("listening", "听力训练", "磨耳朵", Icons.Default.Headphones, isAvailable = true),
        ModuleConfig("writing", "写作/专项", "句型实战", Icons.Default.Create)
    )

    if (showReviewChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showReviewChoiceDialog = false },
            title = { Text("还有待复习单词") },
            text = { Text("你还有 ${vocabularyEntryState.pendingReviewCount} 个单词要复习") },
            confirmButton = {
                Button(
                    onClick = {
                        showReviewChoiceDialog = false
                        val pendingRoundId = vocabularyEntryState.pendingRoundId ?: return@Button
                        onOpenVocabularyReview(
                            VocabularyPracticeArgs(
                                sourceModuleId = "vocabulary_review",
                                entryMode = VocabularyPracticeMode.Review,
                                targetRoundId = pendingRoundId
                            )
                        )
                    }
                ) {
                    Text("复习")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showReviewChoiceDialog = false
                        onOpenVocabularyStudy(
                            VocabularyPracticeArgs(
                                sourceModuleId = "vocabulary",
                                entryMode = VocabularyPracticeMode.Study
                            )
                        )
                    }
                ) {
                    Text("仍要学习")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                // Suspended indicator above bottom bar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CustomIndicatorPanel(pagerState = pagerState)
                }
                BottomNavigationBar(pagerState = pagerState)
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) { page ->
            when (page) {
                0 -> DashboardScreen() // Tab 1: Dashboard
                1 -> LearningHubScreen(
                    modules = learningModules,
                    onModuleClick = { module ->
                        when (module.id) {
                            "vocabulary" -> {
                                if (vocabularyEntryState.pendingReviewCount > 0) {
                                    showReviewChoiceDialog = true
                                } else {
                                    onOpenVocabularyStudy(
                                        VocabularyPracticeArgs(
                                            sourceModuleId = "vocabulary",
                                            entryMode = VocabularyPracticeMode.Study
                                        )
                                    )
                                }
                            }

                            "vocabulary_review" -> {
                                val pendingRoundId = vocabularyEntryState.pendingRoundId
                                if (pendingRoundId != null && vocabularyEntryState.pendingReviewCount > 0) {
                                    onOpenVocabularyReview(
                                        VocabularyPracticeArgs(
                                            sourceModuleId = "vocabulary_review",
                                            entryMode = VocabularyPracticeMode.Review,
                                            targetRoundId = pendingRoundId
                                        )
                                    )
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("暂无待复习单词")
                                    }
                                }
                            }

                            "listening" -> onOpenListeningPractice()

                            "quiz" -> onOpenVocabularyQuiz()
                        }
                    },
                    snackbarHostState = snackbarHostState
                ) // Tab 2: Learning Hub
                2 -> DataGardenScreen(trendReplayKey = dataGardenEnterKey) // Tab 3: Data & Garden
                3 -> ProfileScreen(onOpenShop = onOpenShop)
                else -> {
                    // Placeholder for other Tabs
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tab ${page + 1}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
