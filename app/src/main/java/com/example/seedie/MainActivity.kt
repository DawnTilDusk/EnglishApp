package com.example.seedie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.seedie.ui.navigation.SeedieNavHost
import com.example.seedie.ui.theme.SeedieTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeedieTheme {
                val navController = rememberNavController()
                SeedieNavHost(navController = navController)
            }
        }
    }
}