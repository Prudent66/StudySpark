package com.studyspark.app.ui.editor

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyspark.app.domain.model.Question

/**
 * A card for editing a single multiple-choice question.
 */
@Composable
fun QuestionEditorCard(
    questionNumber: Int,
    question: Question,
    onQuestionChange: (Question) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: "Question N" + Delete button
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

            Spacer(modifier = Modifier.height(8.dp))

            // Question text
            OutlinedTextField(
                value = question.text,
                onValueChange = { onQuestionChange(question.copy(text = it)) },
                label = { Text("Question") },
                placeholder = { Text("What do you want to ask?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Options (tap the circle to mark the correct one)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Four options
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

            Spacer(modifier = Modifier.height(8.dp))

            // Optional explanation
            OutlinedTextField(
                value = question.explanation ?: "",
                onValueChange = { onQuestionChange(question.copy(explanation = it.ifBlank { null })) },
                label = { Text("Explanation (optional)") },
                placeholder = { Text("Why is this the correct answer?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
