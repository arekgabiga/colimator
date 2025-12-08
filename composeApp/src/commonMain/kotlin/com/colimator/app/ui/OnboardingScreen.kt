package com.colimator.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.colimator.app.viewmodel.OnboardingState
import com.colimator.app.viewmodel.OnboardingViewModel

/**
 * Onboarding screen that checks for required CLI dependencies.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onReady: () -> Unit,
    onExit: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    when (val currentState = state) {
        is OnboardingState.Checking -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking dependencies...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        is OnboardingState.Ready -> {
            // Auto-proceed to main app
            onReady()
        }
        
        is OnboardingState.Missing -> {
            AlertDialog(
                onDismissRequest = { /* Cannot dismiss */ },
                title = { 
                    Text("Missing Dependencies") 
                },
                text = { 
                    Text(currentState.message) 
                },
                confirmButton = {
                    Button(onClick = { viewModel.checkDependencies() }) {
                        Text("Retry")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onExit) {
                        Text("Exit")
                    }
                }
            )
        }
    }
}
