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
        setContent {
            StudySparkTheme {
                StudySparkNavGraph()
            }
        }
    }
}
