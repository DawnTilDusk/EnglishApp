package com.example.testenglishlearningmachine.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testenglishlearningmachine.ui.components.AppTopBar
import com.example.testenglishlearningmachine.ui.components.Flashcard
import com.example.testenglishlearningmachine.viewmodel.LearningViewModel

@Composable
fun LearningScreen(
    navController: NavController,
    viewModel: LearningViewModel = viewModel()
) {
    val words by viewModel.words.collectAsState()
    val currentIndex by viewModel.currentWordIndex.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Flashcards",
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
            if (words.isEmpty()) {
                CircularProgressIndicator()
            } else {
                val currentWord = words.getOrNull(currentIndex)
                
                if (currentWord != null) {
                    Flashcard(
                        word = currentWord,
                        onPlayAudio = { viewModel.playAudio(currentWord.text) },
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.previousWord() },
                            enabled = currentIndex > 0
                        ) {
                            Text("Previous")
                        }

                        Button(
                            onClick = { viewModel.nextWord() }
                        ) {
                            Text(if (currentIndex < words.size - 1) "Next" else "Restart")
                        }
                    }
                    
                    Text(
                        text = "${currentIndex + 1} / ${words.size}",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
