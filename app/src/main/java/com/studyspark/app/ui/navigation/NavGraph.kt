package com.studyspark.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                description = "Upload a handout, extract text, and generate a quiz. Coming soon.",
                onBack = { navController.popBackStack() }
            )
        }

        // Route accepts optional quizId (null = create new)
        composable(
            route = "${Routes.MANUAL_EDITOR}?quizId={quizId}",
            arguments = listOf(navArgument("quizId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId")
            ManualEditorScreen(
                quizId = quizId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable("${Routes.QUIZ_SETUP}/{quizId}") { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            QuizSetupScreen(
                quizId = quizId,
                onBack = { navController.popBackStack() },
                onEditQuiz = {
                    navController.navigate("${Routes.MANUAL_EDITOR}?quizId=$quizId")
                },
                onStartQuiz = { questionCount, timeLimitSeconds, randomize ->
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
