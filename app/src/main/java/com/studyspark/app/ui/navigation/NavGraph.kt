package com.studyspark.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studyspark.app.ui.HomeScreen
import com.studyspark.app.ui.editor.ManualEditorScreen
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
                onCreateManually = { navController.navigate(Routes.MANUAL_EDITOR) }
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

        composable(Routes.QUIZ_SETUP) {
            PlaceholderScreen(
                title = "Quiz Setup",
                description = "Choose the number of questions and set a time limit.",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QUIZ_TAKING) {
            PlaceholderScreen(
                title = "Quiz Taking",
                description = "Answer questions with a timer and navigator.",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RESULTS) {
            PlaceholderScreen(
                title = "Results",
                description = "Your score and answer review. Coming in Phase 3.",
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
