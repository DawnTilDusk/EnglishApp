package com.example.seedie.ui.screens.teacher.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TeacherDashboardScreen(
    onStudentClick: (String) -> Unit,
    viewModel: TeacherDashboardViewModel = hiltViewModel()
) {
    val students by viewModel.students.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && students.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { viewModel.refresh() }) {
                        Text("重试")
                    }
                }
            }
            students.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "暂无绑定学生\n请在 Supabase 后台设置 students.teacher_id",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = { viewModel.refresh() }) {
                        Text("刷新")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "我的学生",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(students, key = { it.id }) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStudentClick(student.id) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                student.studentNo?.let {
                                    Text(text = "学号：$it", style = MaterialTheme.typography.bodyMedium)
                                }
                                student.classId?.let {
                                    Text(text = "班级：$it", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentDetailScreen(
    studentId: String,
    onNavigateBack: () -> Unit,
    viewModel: StudentDetailViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    androidx.compose.runtime.LaunchedEffect(studentId) {
        viewModel.load(studentId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "← 返回",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onNavigateBack() }
        )

        when {
            isLoading -> CircularProgressIndicator()
            errorMessage != null -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.load(studentId) }) { Text("重试") }
                }
            }
            stats != null -> {
                val s = stats!!
                Text(text = s.name, style = MaterialTheme.typography.headlineMedium)
                StatRow("学号", s.studentNo ?: "-")
                StatRow("班级", s.classId ?: "-")
                StatRow("累计签到", "${s.totalCheckIns} 次")
                StatRow("累计学习", "${s.totalStudyMinutes} 分钟")
                StatRow("已学词汇", "${s.learnedWordCount} 词")
                StatRow("代币余额", "${s.tokenBalance}")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
