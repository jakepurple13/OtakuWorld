package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.ui.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel = koinViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (val state = authState) {
                is AuthState.Authenticated -> Column(Modifier.padding(16.dp)) {
                    Text("Signed in as ${state.user.email ?: state.user.phone ?: "Anonymous"}")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = viewModel::signOut) { Text("Sign Out") }
                }

                else -> Column(Modifier.fillMaxSize().padding(16.dp)) {
                    SecondaryTabRow(selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Login") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Sign Up") })
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedTab == 0) viewModel.signInWithEmail(email, password)
                            else viewModel.signUpWithEmail(email, password)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (selectedTab == 0) "Login" else "Create Account") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.signInWithMagicLink(email) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = email.isNotBlank(),
                    ) { Text("Send Magic Link") }
                    if (state is AuthState.Error) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                    if (state is AuthState.Loading) {
                        Spacer(Modifier.height(8.dp))
                        CircularWavyProgressIndicator()
                    }
                }
            }
        }
    }
}
