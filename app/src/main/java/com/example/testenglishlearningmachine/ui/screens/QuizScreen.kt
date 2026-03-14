package com.example.testenglishlearningmachine.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testenglishlearningmachine.ui.components.AppTopBar
import com.example.testenglishlearningmachine.viewmodel.QuizViewModel

@Composable
fun QuizScreen(
    navController: NavController,
    viewModel: QuizViewModel = viewModel()
) {
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val options by viewModel.options.collectAsState()
    val score by viewModel.score.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Quiz Challenge",
                canNavigateBack = true,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (currentQuestion != null) {
                Text(
                    text = "What is the meaning of:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = currentQuestion!!.text,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                options.forEach { option ->
                    val buttonColor = if (isCorrect != null) {
                        if (option.id == currentQuestion!!.id) {
                            Color.Green // Correct answer
                        } else if (isCorrect == false && option.meaning == currentQuestion!!.meaning) {
                             Color.Green // Should not happen if checking IDs, but just in case
                        } else {
                            if (isCorrect == false) Color.Red else MaterialTheme.colorScheme.primary
                        }
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    
                    // Actually, logic for color:
                    // If not answered: Primary
                    // If answered:
                    //   - If this button is the correct answer: Green
                    //   - If this button was clicked AND it is wrong: Red
                    //   - Otherwise: Gray/Disabled look?
                    
                    // Let's simplify:
                    // We need to know WHICH button was clicked to show Red on THAT one.
                    // But ViewModel only exposes isCorrect.
                    // Let's just highlight the Correct Answer in Green always after answer.
                    // And if wrong, we don't know which one was clicked unless we track it.
                    // For MVP, just show Correct Answer in Green.
                    
                    val backgroundColor = if (isCorrect != null) {
                        if (option.id == currentQuestion!!.id) {
                             Color(0xFF4CAF50) // Green
                        } else {
                             MaterialTheme.colorScheme.surfaceVariant
                        }
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    Button(
                        onClick = { viewModel.checkAnswer(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        enabled = isCorrect == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = backgroundColor,
                            disabledContainerColor = backgroundColor,
                            contentColor = if (isCorrect == null) Color.White else Color.Black,
                            disabledContentColor = Color.Black
                        )
                    ) {
                        Text(text = option.meaning)
                    }
                }

                if (isCorrect != null) {
                    Text(
                        text = if (isCorrect == true) "Correct!" else "Wrong!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isCorrect == true) Color(0xFF4CAF50) else Color.Red,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.generateNewQuestion() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Next Question")
                    }
                }
            } else {
                Text("Loading questions...")
            }
        }
    }
}
