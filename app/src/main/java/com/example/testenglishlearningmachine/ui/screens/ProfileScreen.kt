package com.example.testenglishlearningmachine.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testenglishlearningmachine.ui.components.AppTopBar
import com.example.testenglishlearningmachine.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val wordsLearned by viewModel.wordsLearned.collectAsState()
    var editingName by remember(userName) { mutableStateOf(userName) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "My Profile",
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Learning Statistics",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Words Learned: $wordsLearned",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = editingName,
                onValueChange = { editingName = it },
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.updateUserName(editingName) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Name")
            }
        }
    }
}
