package com.example.seedie.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.seedie.domain.model.ActivityModule
import com.example.seedie.domain.usecase.GlobalActivityTracker
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.screens.learning.assignments.AssignmentListRoute
import com.example.seedie.ui.screens.learning.assignments.PracticeAssignmentArgs
import com.example.seedie.ui.screens.learning.catalog.FreePracticeArgs
import com.example.seedie.ui.screens.learning.catalog.PracticeCatalogRoute
import com.example.seedie.ui.screens.learning.catalog.PracticeModeChooserRoute
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeRoute
import com.example.seedie.ui.screens.learning.listening.ListeningPracticeRoute
import com.example.seedie.ui.screens.learning.listening.immersion.ListeningImmersionRoute
import com.example.seedie.ui.screens.learning.listening.textbook.ListeningTextbookBooksRoute
import com.example.seedie.ui.screens.learning.listening.textbook.ListeningTextbookUnitsRoute
import com.example.seedie.ui.screens.learning.quiz.VocabularyQuizRoute
import com.example.seedie.ui.screens.learning.reading.ReadingPracticeRoute
import com.example.seedie.ui.screens.learning.writing.WritingConnectorDrillRoute
import com.example.seedie.ui.screens.learning.writing.WritingFeatureDestination
import com.example.seedie.ui.screens.learning.writing.WritingHubScreen
import com.example.seedie.ui.screens.learning.writing.WritingParaphraseRoute
import com.example.seedie.ui.screens.learning.writing.WritingPracticeRoute
import com.example.seedie.ui.screens.learning.writing.WritingSentenceTranslationRoute
import com.example.seedie.ui.screens.main.MainScreen
import com.example.seedie.ui.screens.shop.MyOrdersScreen
import com.example.seedie.ui.screens.shop.StudentShopScreen
import com.example.seedie.ui.screens.splash.SplashScreen

@Composable
fun SeedieNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route,
    activityTracker: GlobalActivityTracker
) {
    var pendingStudyResult by remember { mutableStateOf<StudyResult?>(null) }
    var currentVocabularyArgs by remember { mutableStateOf(VocabularyPracticeArgs(sourceModuleId = "vocabulary")) }
    var currentReadingAssignmentArgs by remember { mutableStateOf<PracticeAssignmentArgs?>(null) }
    var currentListeningAssignmentArgs by remember { mutableStateOf<PracticeAssignmentArgs?>(null) }
    var currentReadingFreeArgs by remember { mutableStateOf<FreePracticeArgs?>(null) }
    var currentListeningFreeArgs by remember { mutableStateOf<FreePracticeArgs?>(null) }
    var currentWritingArgs by remember { mutableStateOf<PracticeAssignmentArgs?>(null) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute, currentVocabularyArgs) {
        val module = when (currentRoute) {
            Screen.VocabularyPractice.route -> ActivityModule.fromPracticeSource(currentVocabularyArgs.sourceModuleId)
            Screen.ListeningPractice.route,
            Screen.ListeningAssignments.route,
            Screen.ListeningMode.route,
            Screen.ListeningTextbookBooks.route,
            Screen.ListeningTextbookUnits.route,
            Screen.ListeningImmersion.route,
            Screen.ListeningCatalog.route -> ActivityModule.ListeningPractice
            Screen.ReadingPractice.route,
            Screen.ReadingAssignments.route,
            Screen.ReadingMode.route,
            Screen.ReadingCatalog.route -> ActivityModule.ReadingPractice
            Screen.WritingHub.route,
            Screen.WritingPractice.route,
            Screen.WritingAssignments.route,
            Screen.WritingSentenceTranslation.route,
            Screen.WritingConnectorDrill.route,
            Screen.WritingParaphrase.route -> ActivityModule.WritingPractice
            Screen.VocabularyQuiz.route -> ActivityModule.VocabularyQuiz
            Screen.StudentShop.route,
            Screen.MyOrders.route -> ActivityModule.Shop
            Screen.Main.route -> null
            else -> null
        }
        if (currentRoute != Screen.Main.route) {
            activityTracker.trackModule(module)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Main.route) {
            MainScreen(
                onOpenVocabularyStudy = { args ->
                    currentVocabularyArgs = args
                    navController.navigate(Screen.VocabularyPractice.route)
                },
                onOpenVocabularyReview = { args ->
                    currentVocabularyArgs = args
                    navController.navigate(Screen.VocabularyPractice.route)
                },
                onOpenListeningMode = {
                    navController.navigate(Screen.ListeningMode.route)
                },
                onOpenReadingMode = {
                    navController.navigate(Screen.ReadingMode.route)
                },
                onOpenWritingHub = {
                    navController.navigate(Screen.WritingHub.route)
                },
                onOpenVocabularyQuiz = {
                    navController.navigate(Screen.VocabularyQuiz.route)
                },
                onOpenShop = {
                    navController.navigate(Screen.StudentShop.route)
                },
                onVisibleModuleChanged = activityTracker::trackModule,
                pendingStudyResult = pendingStudyResult,
                onStudyResultConsumed = {
                    pendingStudyResult = null
                }
            )
        }
        composable(route = Screen.VocabularyPractice.route) {
            VocabularyPracticeRoute(
                args = currentVocabularyArgs,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFinishSession = { result ->
                    pendingStudyResult = result
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.ReadingMode.route) {
            PracticeModeChooserRoute(
                moduleId = "reading",
                onNavigateBack = { navController.popBackStack() },
                onSelectFree = { navController.navigate(Screen.ReadingCatalog.route) },
                onSelectHomework = { navController.navigate(Screen.ReadingAssignments.route) }
            )
        }
        composable(route = Screen.ListeningMode.route) {
            PracticeModeChooserRoute(
                moduleId = "listening",
                onNavigateBack = { navController.popBackStack() },
                onSelectFree = { navController.navigate(Screen.ListeningCatalog.route) },
                onSelectHomework = { navController.navigate(Screen.ListeningAssignments.route) },
                onSelectTextbook = { navController.navigate(Screen.ListeningTextbookBooks.route) },
                onSelectImmersion = { navController.navigate(Screen.ListeningImmersion.route) }
            )
        }
        composable(route = Screen.ListeningTextbookBooks.route) {
            ListeningTextbookBooksRoute(
                onNavigateBack = { navController.popBackStack() },
                onOpenBook = { bookId ->
                    navController.navigate(Screen.ListeningTextbookUnits.buildRoute(bookId))
                }
            )
        }
        composable(route = Screen.ListeningTextbookUnits.route) {
            ListeningTextbookUnitsRoute(
                onNavigateBack = { navController.popBackStack() },
                onOpenSection = { materialId ->
                    currentListeningFreeArgs =
                        FreePracticeArgs(moduleId = "listening", itemRef = materialId)
                    currentListeningAssignmentArgs = null
                    navController.navigate(Screen.ListeningPractice.route)
                }
            )
        }
        composable(route = Screen.ListeningImmersion.route) {
            ListeningImmersionRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.ReadingCatalog.route) {
            PracticeCatalogRoute(
                moduleId = "reading",
                onNavigateBack = { navController.popBackStack() },
                onOpenItem = { args ->
                    currentReadingFreeArgs = args
                    currentReadingAssignmentArgs = null
                    navController.navigate(Screen.ReadingPractice.route)
                }
            )
        }
        composable(route = Screen.ListeningCatalog.route) {
            PracticeCatalogRoute(
                moduleId = "listening",
                onNavigateBack = { navController.popBackStack() },
                onOpenItem = { args ->
                    currentListeningFreeArgs = args
                    currentListeningAssignmentArgs = null
                    navController.navigate(Screen.ListeningPractice.route)
                }
            )
        }
        composable(route = Screen.ReadingAssignments.route) {
            AssignmentListRoute(
                moduleId = "reading",
                onNavigateBack = { navController.popBackStack() },
                onOpenAssignment = { args ->
                    currentReadingAssignmentArgs = args
                    currentReadingFreeArgs = null
                    navController.navigate(Screen.ReadingPractice.route)
                }
            )
        }
        composable(route = Screen.ListeningAssignments.route) {
            AssignmentListRoute(
                moduleId = "listening",
                onNavigateBack = { navController.popBackStack() },
                onOpenAssignment = { args ->
                    currentListeningAssignmentArgs = args
                    currentListeningFreeArgs = null
                    navController.navigate(Screen.ListeningPractice.route)
                }
            )
        }
        composable(route = Screen.WritingAssignments.route) {
            AssignmentListRoute(
                moduleId = "writing",
                onNavigateBack = { navController.popBackStack() },
                onOpenAssignment = { args ->
                    currentWritingArgs = args
                    navController.navigate(Screen.WritingPractice.route)
                }
            )
        }
        composable(route = Screen.WritingHub.route) {
            WritingHubScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenFeature = { destination ->
                    when (destination) {
                        WritingFeatureDestination.PracticalWriting ->
                            navController.navigate(Screen.WritingAssignments.route)
                        WritingFeatureDestination.SentenceTranslation ->
                            navController.navigate(Screen.WritingSentenceTranslation.route)
                        WritingFeatureDestination.ConnectorDrill ->
                            navController.navigate(Screen.WritingConnectorDrill.route)
                        WritingFeatureDestination.Paraphrase ->
                            navController.navigate(Screen.WritingParaphrase.route)
                    }
                }
            )
        }
        composable(route = Screen.ListeningPractice.route) {
            val assignmentArgs = currentListeningAssignmentArgs
            val freeArgs = currentListeningFreeArgs
            when {
                assignmentArgs != null -> {
                    ListeningPracticeRoute(
                        assignmentArgs = assignmentArgs,
                        freeArgs = null,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onFinishSession = { result ->
                            pendingStudyResult = result
                            navController.popBackStack()
                        }
                    )
                }
                freeArgs != null -> {
                    ListeningPracticeRoute(
                        assignmentArgs = null,
                        freeArgs = freeArgs,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onFinishSession = { result ->
                            pendingStudyResult = result
                            navController.popBackStack()
                        }
                    )
                }
                else -> LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
        composable(route = Screen.ReadingPractice.route) {
            val assignmentArgs = currentReadingAssignmentArgs
            val freeArgs = currentReadingFreeArgs
            when {
                assignmentArgs != null -> {
                    ReadingPracticeRoute(
                        assignmentArgs = assignmentArgs,
                        freeArgs = null,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onFinishSession = { result ->
                            pendingStudyResult = result
                            navController.popBackStack()
                        }
                    )
                }
                freeArgs != null -> {
                    ReadingPracticeRoute(
                        assignmentArgs = null,
                        freeArgs = freeArgs,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onFinishSession = { result ->
                            pendingStudyResult = result
                            navController.popBackStack()
                        }
                    )
                }
                else -> LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
        composable(route = Screen.WritingPractice.route) {
            val args = currentWritingArgs
            if (args == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                WritingPracticeRoute(
                    assignmentArgs = args,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAwardResult = { result ->
                        pendingStudyResult = result
                    }
                )
            }
        }
        composable(route = Screen.WritingSentenceTranslation.route) {
            WritingSentenceTranslationRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.WritingConnectorDrill.route) {
            WritingConnectorDrillRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.WritingParaphrase.route) {
            WritingParaphraseRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.VocabularyQuiz.route) {
            VocabularyQuizRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFinishSession = { result ->
                    pendingStudyResult = result
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.StudentShop.route) {
            StudentShopScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenOrders = { navController.navigate(Screen.MyOrders.route) }
            )
        }
        composable(route = Screen.MyOrders.route) {
            MyOrdersScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
