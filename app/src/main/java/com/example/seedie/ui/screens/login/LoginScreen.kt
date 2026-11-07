package com.example.seedie.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(onNavigateToNext: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val emailValid = remember(email) { viewModel.isEmailFormatValid(email) }
    val phoneValid = remember(phone) { viewModel.isPhoneInputValid(phone) }
    LaunchedEffect(Unit) { viewModel.consumePendingLoginError() }
    LoginContent(email, password, phone, isLoading, errorMessage, emailValid, phoneValid,
        viewModel::onEmailChange, viewModel::onPasswordChange, viewModel::onPhoneChange,
        onLogin = { viewModel.login(onLoginSuccess = onNavigateToNext) })
}

@Composable
internal fun LoginContent(email: String, password: String, phone: String, loading: Boolean, error: String?,
    emailValid: Boolean, phoneValid: Boolean, onEmail: (String) -> Unit, onPassword: (String) -> Unit,
    onPhone: (String) -> Unit, onLogin: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding().safeDrawingPadding()) {
        val wide = maxWidth >= 820.dp
        Row(Modifier.fillMaxSize().padding(if (wide) 36.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            if (wide) Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.Spa, null, Modifier.padding(14.dp).size(30.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Seedie", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text("每天一点，\n英语慢慢生长。", style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("从一个单词、一段听力开始。\n让每一次投入，都成为看得见的成长。",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("词汇 · 阅读 · 听力 · 写作", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                Surface(Modifier.widthIn(max = 460.dp).fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("欢迎回来", style = MaterialTheme.typography.headlineMedium)
                        Text("登录 Seedie，继续今天的学习。", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text("账号 / 邮箱") },
                            singleLine = true, enabled = !loading, shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = email.isNotBlank() && !emailValid,
                            supportingText = { if (email.isNotBlank() && !emailValid) Text("请检查邮箱格式") })
                        var visible by remember { mutableStateOf(false) }
                        OutlinedTextField(password, onPassword, Modifier.fillMaxWidth(), label = { Text("密码") },
                            singleLine = true, enabled = !loading, shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { IconButton(onClick = { visible = !visible }) {
                                Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    if (visible) "隐藏密码" else "显示密码")
                            } })
                        OutlinedTextField(phone, onPhone, Modifier.fillMaxWidth(), label = { Text("手机号") },
                            singleLine = true, enabled = !loading, shape = MaterialTheme.shapes.small,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phone.isNotBlank() && !phoneValid,
                            supportingText = { Text(if (phone.isNotBlank() && !phoneValid) "请检查手机号格式" else "首次登录用于绑定学生账号") })
                        if (error != null) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                            Text(error, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Button(onClick = onLogin, enabled = !loading && emailValid && password.isNotBlank() && phone.isNotBlank() && phoneValid,
                            modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("进入我的学习空间")
                        }
                        Text("学生端登录 · 机构与教师请使用网页控制台", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
