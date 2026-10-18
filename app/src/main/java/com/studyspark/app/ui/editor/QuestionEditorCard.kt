package com.studyspark.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyspark.app.domain.model.DraftQuestion

@Composable
fun QuestionEditorCard(
    questionNumber: Int,
    question: DraftQuestion,
    onQuestionChange: (DraftQuestion) -> Unit,
    onDelete: () -> Unit
) {
    var showExplanation by remember { mutableStateOf(question.explanation.isNotBlank()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question $questionNumber",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete question",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Question Type Selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = question.questionType == "Objectives",
                    onClick = { onQuestionChange(question.copy(questionType = "Objectives")) },
                    label = { Text("Objectives") }
                )
                FilterChip(
                    selected = question.questionType == "Essay",
                    onClick = { onQuestionChange(question.copy(questionType = "Essay")) },
                    label = { Text("Essay") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = question.text,
                onValueChange = { onQuestionChange(question.copy(text = it)) },
                label = { Text("Question") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            // Only show options if it's an Objective question
            if (question.questionType == "Objectives") {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Options (tap the circle to mark the correct one)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val optionLabels = listOf("A", "B", "C", "D")
                question.options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = question.correctOptionIndex == index,
                                onClick = { onQuestionChange(question.copy(correctOptionIndex = index)) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = question.correctOptionIndex == index,
                            onClick = { onQuestionChange(question.copy(correctOptionIndex = index)) }
                        )
                        Text(
                            text = "${optionLabels[index]}.",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.width(24.dp)
                        )
                        OutlinedTextField(
                            value = option,
                            onValueChange = { newText ->
                                val updated = question.options.toMutableList()
                                updated[index] = newText
                                onQuestionChange(question.copy(options = updated))
                            },
                            placeholder = { Text("Option ${optionLabels[index]}") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Explanation Toggle
            if (!showExplanation) {
                TextButton(onClick = { showExplanation = true }) {
                    Text("+ Add / Show Explanation")
                }
            } else {
                OutlinedTextField(
                    value = question.explanation,
                    onValueChange = { onQuestionChange(question.copy(explanation = it)) },
                    label = { Text(if (question.questionType == "Essay") "Model Answer (Hidden until results)" else "Explanation (Hidden until results)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
                TextButton(onClick = { showExplanation = false }) {
                    Text("Hide Explanation")
                }
            }
        }
    }
}
