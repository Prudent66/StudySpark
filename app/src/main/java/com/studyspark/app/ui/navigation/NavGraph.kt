package com.studyspark.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studyspark.app.ui.HomeScreen
import com.studyspark.app.ui.editor.ManualEditorScreen
import com.studyspark.app.ui.pdf.PdfImportScreen
import com.studyspark.app.ui.setup.QuizSetupScreen
import com.studyspark.app.ui.screens.PlaceholderScreen
import com.studyspark.app.ui.taking.QuizTakingScreen
import com.studyspark.app.ui.taking.ResultsScreen
import com.studyspark.app.domain.model.QuizResult

var lastQuizResult: QuizResult? = null

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
            PdfImportScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEditor = { quizId ->
                    // We navigate to the editor using the ID of the newly saved AI quiz
                    navController.navigate("${Routes.MANUAL_EDITOR}?quizId=$quizId") {
                        popUpTo(Routes.IMPORT_PDF) { inclusive = true }
                    }
                }
            )
        }

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
                onStartQuiz = { questionCount, timeLimitSeconds, randomize, blockScreenshots ->
                    navController.navigate(
                        "${Routes.QUIZ_TAKING}/$quizId?count=$questionCount&time=$timeLimitSeconds&randomize=$randomize&blockScreenshots=$blockScreenshots"
                    )
                }
            )
        }

        composable(
            route = "${Routes.QUIZ_TAKING}/{quizId}?count={count}&time={time}&randomize={randomize}&blockScreenshots={blockScreenshots}",
            arguments = listOf(
                navArgument("quizId") { type = NavType.StringType },
                navArgument("count") { type = NavType.IntType },
                navArgument("time") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("randomize") { type = NavType.BoolType },
                navArgument("blockScreenshots") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            val count = backStackEntry.arguments?.getInt("count") ?: 1
            val timeStr = backStackEntry.arguments?.getString("time")
            val time = if (timeStr == "null" || timeStr == null) null else timeStr.toIntOrNull()
            val randomize = backStackEntry.arguments?.getBoolean("randomize") ?: false
            val blockScreenshots = backStackEntry.arguments?.getBoolean("blockScreenshots") ?: true

            QuizTakingScreen(
                quizId = quizId,
                questionCount = count,
                timeLimitSeconds = time,
                randomize = randomize,
                blockScreenshots = blockScreenshots,
                onNavigateToResults = { resultState ->
                    lastQuizResult = resultState
                    navController.navigate(Routes.RESULTS) {
                        popUpTo(Routes.HOME)
                    }
                },
                onExit = { navController.popBackStack() }
            )
        }

        composable(Routes.RESULTS) {
            val result = lastQuizResult
            if (result != null) {
                ResultsScreen(
                    result = result,
                    onDone = {
                        lastQuizResult = null
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                )
            } else {
                PlaceholderScreen(
                    title = "Error",
                    description = "No result found.",
                    onBack = { navController.popBackStack() }
                )
            }
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
