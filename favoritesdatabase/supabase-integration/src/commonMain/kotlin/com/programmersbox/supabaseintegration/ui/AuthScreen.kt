package com.programmersbox.supabaseintegration.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpmodels.ManagedTable
import com.programmersbox.kmpmodels.SupportedTableAction
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.ui.viewmodel.AuthViewModel
import com.programmersbox.supabaseintegration.ui.viewmodel.LogoutUiState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = koinViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val logoutUiState by viewModel.logoutUiState.collectAsStateWithLifecycle()

    val isLoading = authState is AuthState.Loading

    SupabaseLoadingDialog(
        showLoadingDialog = logoutUiState.isLoggingOut,
        onDismissRequest = {},
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = authState, label = "Auth State Transition") { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        // --- AUTHENTICATED STATE ---
                        AuthenticatedState(
                            state = state,
                            logoutUiState = logoutUiState,
                            onConfirmLogout = viewModel::confirmLogout,
                            onManageDatabasesEnabledChange = viewModel::setManageDatabasesEnabled,
                            onTableActionChange = viewModel::setTableAction,
                        )
                    }

                    else -> {
                        // --- UNAUTHENTICATED STATE ---
                        UnauthenticatedState(
                            state = state,
                            isLoading = isLoading,
                            onSignIn = viewModel::signInWithEmail,
                            onSignUp = viewModel::signUpWithEmail,
                            signInWithMagicLink = viewModel::signInWithMagicLink,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedState(
    state: AuthState.Authenticated,
    logoutUiState: LogoutUiState,
    onConfirmLogout: () -> Unit,
    onManageDatabasesEnabledChange: (Boolean) -> Unit,
    onTableActionChange: (ManagedTable, SupportedTableAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showLogOutDialog by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        LogoutBottomSheet(
            manageDatabasesEnabled = logoutUiState.manageDatabasesEnabled,
            tableSelections = logoutUiState.tableSelections,
            onManageDatabasesEnabledChange = onManageDatabasesEnabledChange,
            onTableActionChange = onTableActionChange,
            onContinueToLogout = {
                showBottomSheet = false
                showLogOutDialog = true
            },
            onCancel = { showBottomSheet = false },
            onDismissRequest = { showBottomSheet = false },
        )
    }

    if (showLogOutDialog) {
        AlertDialog(
            onDismissRequest = { showLogOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmLogout()
                        showLogOutDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogOutDialog = false },
                ) { Text("No") }
            },
        )
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.user.email ?: state.user.phone ?: "Anonymous",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(
                text = "ID: ${state.user.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { showBottomSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun UnauthenticatedState(
    state: AuthState,
    isLoading: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    signInWithMagicLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .imePadding() // Handles keyboard pushing up UI
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Login") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Sign Up") }
            )
        }

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (selectedTab == 0) onSignIn(email, password)
                    else onSignUp(email, password)
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        // Error Message Box
        AnimatedVisibility(
            visible = state is AuthState.Error,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = (state as? AuthState.Error)?.message ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                if (selectedTab == 0) onSignIn(email, password)
                else onSignUp(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(if (selectedTab == 0) "Login" else "Create Account")
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                signInWithMagicLink(email)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = email.isNotBlank() && !isLoading,
        ) {
            Text("Send Magic Link")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SupabaseLoadingDialog(
    showLoadingDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (showLoadingDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.0.dp))
            ) {
                Column {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(text = "Loading...", Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }

    }
}
