package com.studyspark.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studyspark.app.ui.HomeScreen
import com.studyspark.app.ui.editor.ManualEditorScreen
import com.studyspark.app.ui.setup.QuizSetupScreen
import com.studyspark.app.ui.screens.PlaceholderScreen

@Composable
fun StudySparkNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onCreateFromPdf = { navController.navigate(Routes.IMPORT_PDF) },
                onCreateManually = { navController.navigate(Routes.MANUAL_EDITOR) },
                onOpenQuiz = { quizId ->
                    navController.navigate("${Routes.QUIZ_SETUP}/$quizId")
                }
            )
        }

        composable(Routes.IMPORT_PDF) {
            PlaceholderScreen(
                title = "Import PDF",
                description = "Upload a handout, extract text, and generate a quiz. Coming in Phase 4.",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MANUAL_EDITOR) {
            ManualEditorScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable("${Routes.QUIZ_SETUP}/{quizId}") { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            QuizSetupScreen(
                quizId = quizId,
                onBack = { navController.popBackStack() },
                onStartQuiz = { questionCount, timeLimitSeconds, randomize ->
                    // Navigate to Quiz Taking (Placeholder for now, built in next step)
                    navController.navigate(
                        "${Routes.QUIZ_TAKING}/$quizId?count=$questionCount&time=$timeLimitSeconds&randomize=$randomize"
                    )
                }
            )
        }

        composable("${Routes.QUIZ_TAKING}/{quizId}?count={count}&time={time}&randomize={randomize}") { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            PlaceholderScreen(
                title = "Quiz Taking",
                description = "Quiz $quizId is ready to start! Coming in the next step.",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RESULTS) {
            PlaceholderScreen(
                title = "Results",
                description = "Your score and answer review. Coming soon.",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            PlaceholderScreen(
                title = "Settings",
                description = "App settings and About. Coming soon.",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
