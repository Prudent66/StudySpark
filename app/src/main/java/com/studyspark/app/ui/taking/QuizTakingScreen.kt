package com.studyspark.app.ui.taking

import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studyspark.app.data.local.StudySparkDatabase
import com.studyspark.app.data.repository.QuizRepository
import com.studyspark.app.domain.model.QuizResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTakingScreen(
    quizId: String,
    questionCount: Int,
    timeLimitSeconds: Int?,
    randomize: Boolean,
    blockScreenshots: Boolean,
    onNavigateToResults: (QuizResult) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    DisposableEffect(blockScreenshots) {
        val window = (view.context as? android.app.Activity)?.window
        if (blockScreenshots) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    val viewModel: QuizTakingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val db = StudySparkDatabase.getDatabase(context)
                val repo = QuizRepository(db.quizDao())
                val prefs = context.getSharedPreferences("studyspark_anticheat", Context.MODE_PRIVATE)
                QuizTakingViewModel(repo, quizId, questionCount, timeLimitSeconds, randomize, prefs)
            }
        }
    )
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !state.isSubmitted) viewModel.onAppBackgrounded()
            if (event == Lifecycle.Event.ON_START && state.isSubmitted) state.result?.let { onNavigateToResults(it) }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted && state.result != null) onNavigateToResults(state.result!!)
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(state.currentIndex) { scrollState.scrollTo(0) }

    if (state.showWarningDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Exam Mode Enabled") },
            text = { Text("Leaving this app will automatically submit your quiz.", textAlign = TextAlign.Center) },
            confirmButton = { Button(onClick = viewModel::acknowledgeWarning) { Text("I Understand") } }
        )
    }

    if (state.showFinishDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFinishDialog,
            title = { Text("Finish Quiz?") },
            text = {
                val unanswered = state.activeQuestions.size - state.answeredCount
                if (unanswered > 0) Text("You have $unanswered unanswered questions. Submit?")
                else Text("You have answered all questions. Ready to submit?")
            },
            confirmButton = { Button(onClick = viewModel::submitQuiz) { Text("Submit") } },
            dismissButton = { TextButton(onClick = viewModel::dismissFinishDialog) { Text("Review Answers") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.quiz?.title ?: "Quiz") },
                actions = {
                    state.timeLeftSeconds?.let { seconds ->
                        val minutes = seconds / 60
                        val secs = seconds % 60
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                            Icon(Icons.Filled.Timer, contentDescription = "Timer", tint = if (seconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format("%02d:%02d", minutes, secs), fontWeight = FontWeight.Bold, color = if (seconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    TextButton(onClick = viewModel::requestFinish) { Text("Finish", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("${state.answeredCount} of ${state.activeQuestions.size} answered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(state.activeQuestions) { index, question ->
                    val isAnswered = if (question.questionType == "Essay") !state.essayAnswers[question.id].isNullOrBlank() else state.answers[question.id] != null
                    val isCurrent = state.currentIndex == index
                    val bgColor = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isAnswered -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(bgColor).selectable(selected = isCurrent, onClick = { viewModel.goToQuestion(index) }),
                        contentAlignment = Alignment.Center
                    ) { Text(text = "${index + 1}", color = if (isCurrent || isAnswered) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                state.currentQuestion?.let { question ->
                    Text("Question ${state.currentIndex + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = question.text, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (question.questionType == "Essay") {
                        OutlinedTextField(
                            value = state.essayAnswers[question.id] ?: "",
                            onValueChange = { viewModel.selectEssayAnswer(question.id, it) },
                            label = { Text("Your Answer") },
                            placeholder = { Text("Type your answer here...") },
                            modifier = Modifier.fillMaxWidth().height(250.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        val labels = listOf("A", "B", "C", "D")
                        question.options.forEachIndexed { index, option ->
                            val isSelected = state.answers[question.id] == index
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).selectable(selected = isSelected, onClick = { viewModel.selectAnswer(question.id, index) }),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = isSelected, onClick = { viewModel.selectAnswer(question.id, index) })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "${labels[index]}. $option", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = viewModel::previousQuestion, enabled = state.currentIndex > 0) { Text("Previous") }
                Button(onClick = viewModel::nextQuestion, enabled = state.currentIndex < state.activeQuestions.size - 1) { Text("Next") }
            }
        }
    }
}
