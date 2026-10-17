package com.studyspark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.studyspark.app.ui.navigation.StudySparkNavGraph
import com.studyspark.app.ui.theme.StudySparkTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader // NEW IMPORT

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // NEW: Initialize PDFBox resources
        PDFBoxResourceLoader.init(applicationContext)

        setContent {
            StudySparkTheme {
                StudySparkNavGraph()
            }
        }
    }
}
