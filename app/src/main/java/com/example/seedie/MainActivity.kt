package com.example.seedie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.seedie.domain.model.UserRole
import com.example.seedie.ui.SeedieNavGraph
import com.example.seedie.ui.navigation.SeedieNavHost
import com.example.seedie.ui.navigation.TeacherNavHost
import com.example.seedie.ui.theme.SeedieTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeedieTheme {
                val authUiState by mainViewModel.authUiState.collectAsState()

                when (val state = authUiState) {
                    is AuthUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is AuthUiState.LoggedIn -> {
                        when (state.session.role) {
                            UserRole.TEACHER -> TeacherNavHost()
                            else -> SeedieNavHost()
                        }
                    }
                    is AuthUiState.LoggedOut -> {
                        SeedieNavGraph(onLoginSuccess = {})
                    }
                }
            }
        }
    }
}
