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
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.io.File
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

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.extractTextFromPdf(context, uri, "Handout.pdf")
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            viewModel.addPendingImage(photoUri!!)
        }
    }

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
                    OutlinedTextField(value = state.schoolName, onValueChange = { viewModel.updateSchoolName(it) }, label = { Text("School/Institution") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = state.department, onValueChange = { viewModel.updateDepartment(it) }, label = { Text("Department") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = state.courseTitle, onValueChange = { viewModel.updateCourseTitle(it) }, label = { Text("Course Title") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = state.examTitle, onValueChange = { viewModel.updateExamTitle(it) }, label = { Text("Exam Title") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = state.timeAllowed, onValueChange = { viewModel.updateTimeAllowed(it) }, label = { Text("Time Allowed") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = state.examDate, onValueChange = { viewModel.updateExamDate(it) }, label = { Text("Date") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = state.instructions, onValueChange = { viewModel.updateInstructions(it) }, label = { Text("Instructions") }, minLines = 2)
                }
            },
            confirmButton = { Button(onClick = { viewModel.generatePdf(context) }) { Text("Generate PDF") } },
            dismissButton = { TextButton(onClick = { viewModel.showHeaderDialog(false) }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Handout") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.isLoadingPdf) {
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Processing...")
                Spacer(modifier = Modifier.weight(1f))
                return@Scaffold
            }

            if (state.generatedQuestions.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "${state.generatedQuestions.size} Questions Generated!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.generatedQuestions.forEachIndexed { index, q ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = q.text, fontWeight = FontWeight.SemiBold)
                                if (q.options.isNotEmpty() && q.options[0] != "") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    q.options.forEachIndexed { i, opt ->
                                        Text(text = "${listOf("A", "B", "C", "D")[i]}. $opt", color = if (i == q.correctOptionIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.questionType == "Objectives") {
                    Button(onClick = { viewModel.saveAndContinueToEditor(onNavigateToEditor) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("Continue to Editor & Take Quiz") }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(text = "Essay quizzes are for PDF export only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                }

                Button(
                    onClick = { viewModel.showHeaderDialog(true) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
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
                
                OutlinedButton(onClick = { viewModel.generateQuestionsWithAi() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Regenerate Questions")
                }

            } else if (state.extractedText.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "Text Extracted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Card(modifier = Modifier.weight(1f).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Text(text = state.extractedText, modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Question Type", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = state.questionType == "Objectives", onClick = { viewModel.updateQuestionType("Objectives") }, label = { Text("Objectives") })
                            FilterChip(selected = state.questionType == "Essay", onClick = { viewModel.updateQuestionType("Essay") }, label = { Text("Essay (PDF)") })
                        }

                        if (state.questionType == "Essay") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Main Questions (a, b, c...): ${state.numberOfQuestions}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Slider(value = state.numberOfQuestions.toFloat(), onValueChange = { viewModel.updateNumberOfQuestions(it.roundToInt()) }, valueRange = 1f..10f, steps = 8)

                            Text("Max Parts per Question (i, ii...): ${state.numberOfParts}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Slider(value = state.numberOfParts.toFloat(), onValueChange = { viewModel.updateNumberOfParts(it.roundToInt()) }, valueRange = 1f..5f, steps = 3)
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Number of Questions: ${state.numberOfQuestions}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Slider(value = state.numberOfQuestions.toFloat(), onValueChange = { viewModel.updateNumberOfQuestions(it.roundToInt()) }, valueRange = 1f..100f, steps = 98)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.isGeneratingAi) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gemini is generating your quiz...")
                } else {
                    Button(onClick = { viewModel.generateQuestionsWithAi() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
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
                Spacer(modifier = Modifier.weight(1f))
                
                if (state.pendingImages.isEmpty()) {
                    Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.height(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Import Handout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Choose a PDF or take photos of your handout.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { pdfLauncher.launch("application/pdf") },
                            modifier = Modifier.weight(1f).height(60.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("PDF File")
                        }
                        
                        Button(
                            onClick = { 
                                val tempFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
                                photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                cameraLauncher.launch(photoUri) 
                            },
                            modifier = Modifier.weight(1f).height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Take Photo")
                        }
                    }
                } else {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.height(60.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "${state.pendingImages.size} Photos Captured", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "You can take more photos or process them all together.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { viewModel.processPendingImages(context) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Extract Text from All Photos")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                val tempFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
                                photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                cameraLauncher.launch(photoUri) 
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Add Another")
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearPendingImages() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear All")
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
