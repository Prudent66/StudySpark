package com.studyspark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.studyspark.app.ui.navigation.StudySparkNavGraph
import com.studyspark.app.ui.theme.StudySparkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // FLAG_SECURE has been removed from here. 
        // It is now handled dynamically inside QuizTakingScreen based on user preference.

        setContent {
            StudySparkTheme {
                StudySparkNavGraph()
            }
        }
    }
}
