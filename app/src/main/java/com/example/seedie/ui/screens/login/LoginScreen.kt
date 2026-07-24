package com.example.seedie.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onNavigateToNext: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isEmailValid = remember(email) { viewModel.isEmailFormatValid(email) }
    val isPhoneValid = remember(phone) { viewModel.isPhoneInputValid(phone) }

    LaunchedEffect(Unit) {
        viewModel.consumePendingLoginError()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Seedie!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "学生登录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("账号/邮箱") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = email.isNotBlank() && !isEmailValid,
                supportingText = {
                    if (email.isNotBlank() && !isEmailValid) {
                        Text("邮箱格式不正确")
                    }
                },
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.onPhoneChange(it) },
                label = { Text("手机号（首次必填）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = phone.isNotBlank() && !isPhoneValid,
                supportingText = {
                    if (phone.isNotBlank() && !isPhoneValid) {
                        Text("手机号格式不正确")
                    }
                },
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }

            val canLogin = !isLoading && isEmailValid && password.isNotBlank() &&
                phone.isNotBlank() && isPhoneValid

            Button(
                onClick = { viewModel.login(onLoginSuccess = { onNavigateToNext() }) },
                enabled = canLogin,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("登录", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
