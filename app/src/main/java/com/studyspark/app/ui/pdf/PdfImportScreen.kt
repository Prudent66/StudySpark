package com.studyspark.app.ui.pdf

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studyspark.app.data.local.StudySparkDatabase
import com.studyspark.app.data.repository.QuizRepository
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfImportScreen(
    onBack: () -> Unit,
    onNavigateToEditor: (quizId: String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: PdfImportViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val db = StudySparkDatabase.getDatabase(context)
                val repo = QuizRepository(db.quizDao())
                PdfImportViewModel(repo)
            }
        }
    )
    val state by viewModel.uiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.extractTextFromPdf(context, uri, "Handout.pdf")
    }

    // Automatically share the PDF when it's generated
    LaunchedEffect(state.generatedPdfFile) {
        state.generatedPdfFile?.let { file ->
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Exam PDF"))
        }
    }

    if (state.showHeaderDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showHeaderDialog(false) },
            title = { Text("Customize PDF Header") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = state.schoolName, onValueChange = { viewModel.updateSchoolName(it) },
                        label = { Text("School/Institution Name") }, singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.examTitle, onValueChange = { viewModel.updateExamTitle(it) },
                        label = { Text("Exam Title") }, singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.courseCode, onValueChange = { viewModel.updateCourseCode(it) },
                        label = { Text("Course / Subject") }, singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.instructions, onValueChange = { viewModel.updateInstructions(it) },
                        label = { Text("Instructions") }, minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.generatePdf(context) }) {
                    Text("Generate PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showHeaderDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoadingPdf) {
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Extracting text from PDF...")
                Spacer(modifier = Modifier.weight(1f))
                return@Scaffold
            }

            if (state.generatedQuestions.isNotEmpty()) {
                // --- Generated Questions Preview ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "${state.generatedQuestions.size} Questions Generated!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.generatedQuestions.forEachIndexed { index, q ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "${index + 1}. ${q.text}", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                q.options.forEachIndexed { i, opt ->
                                    Text(
                                        text = "${listOf("A", "B", "C", "D")[i]}. $opt",
                                        color = if (i == q.correctOptionIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Button(
                    onClick = { viewModel.saveAndContinueToEditor(onNavigateToEditor) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Continue to Editor")
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.showHeaderDialog(true) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (state.isGeneratingPdf) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Download Exam PDF")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { viewModel.generateQuestionsWithAi() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Regenerate Questions")
                }

            } else if (state.extractedText.isNotEmpty()) {
                // --- Extracted Text & AI Configuration ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "PDF Text Extracted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = state.extractedText,
                        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Configuration UI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Question Type", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.questionType == "Objectives",
                                onClick = { viewModel.updateQuestionType("Objectives") },
                                label = { Text("Objectives (Max 100)") }
                            )
                            FilterChip(
                                selected = state.questionType == "Essay",
                                onClick = { viewModel.updateQuestionType("Essay") },
                                label = { Text("Essay (Max 10)") }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Number of Questions: ${state.numberOfQuestions}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Slider(
                            value = state.numberOfQuestions.toFloat(),
                            onValueChange = { viewModel.updateNumberOfQuestions(it.roundToInt()) },
                            valueRange = 1f..state.maxQuestionsAllowed.toFloat(),
                            steps = (state.maxQuestionsAllowed - 2).coerceAtLeast(0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.isGeneratingAi) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gemini is generating your quiz...")
                } else {
                    Button(
                        onClick = { viewModel.generateQuestionsWithAi() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Generate Questions with AI")
                    }
                }
                
                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = state.errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }

            } else {
                // --- Initial File Picker ---
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.height(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select a PDF Handout",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We will extract the text and generate a quiz for you.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { launcher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Choose PDF File")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
