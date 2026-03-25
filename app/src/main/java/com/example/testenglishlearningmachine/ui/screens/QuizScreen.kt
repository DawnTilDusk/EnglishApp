package com.example.testenglishlearningmachine.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        },
        bottomBar = {
            // 固定在底部的操作栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (isCorrect == null) {
                    // 未答题状态显示“不认识”按钮
                    Button(
                        onClick = { viewModel.markAsUnknown() },
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("不认识", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    // 已答题状态显示“下一题”按钮
                    Button(
                        onClick = { viewModel.generateNewQuestion() },
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Next Question", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp), // 增大外边距
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score Header (不限制宽度，依然在顶部两端)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // 使用 weight 替代固定高度，实现自适应
            Spacer(modifier = Modifier.weight(1f))

            // 核心答题区域，限制最大宽度并居中
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentQuestion != null) {
                    Text(
                        text = currentQuestion!!.text,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    options.forEach { option ->
                        val isThisCorrectAnswer = option.id == currentQuestion!!.id
                        val showAsCorrect = isCorrect != null && isThisCorrectAnswer
                        
                        val borderColor = if (showAsCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        }
                        
                        val containerColor = if (showAsCorrect) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }

                        OutlinedCard(
                            onClick = { viewModel.checkAnswer(option) },
                            enabled = isCorrect == null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .height(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = containerColor,
                                disabledContainerColor = containerColor
                            ),
                            border = BorderStroke(if (showAsCorrect) 2.dp else 1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option.meaning,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = if (showAsCorrect) FontWeight.Bold else FontWeight.Normal,
                                    color = if (showAsCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                
                                if (showAsCorrect) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correct",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isCorrect != null) {
                        val feedbackText = if (isCorrect == true) "Awesome! That's correct." else "Not quite right."
                        val feedbackColor = if (isCorrect == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        val feedbackIcon = if (isCorrect == true) Icons.Default.CheckCircle else Icons.Default.Cancel

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Icon(
                                imageVector = feedbackIcon,
                                contentDescription = null,
                                tint = feedbackColor,
                                modifier = Modifier.size(28.dp).padding(end = 8.dp)
                            )
                            Text(
                                text = feedbackText,
                                style = MaterialTheme.typography.titleLarge,
                                color = feedbackColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text("Loading questions...")
                }
            }
            
            // 底部也用 weight 撑开，让内容保持在视觉中央
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
