package com.example.ovoshebaza.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class AuthStage {
    Phone,
    Code,
    PasswordLogin,
    SetPassword
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onSignedIn: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val viewModel: AuthViewModel = viewModel()
    viewModel.updateSignedInCallback(onSignedIn)

    LaunchedEffect(viewModel.resendSeconds) {
        viewModel.tickResendCountdown()
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Вход по телефону") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (viewModel.stage == AuthViewModel.AuthStage.Phone) {
                OutlinedTextField(
                    value = viewModel.phone,
                    onValueChange = { input ->
                        val sanitized = input.replace(" ", "")
                        viewModel.phone = if (sanitized.startsWith("+")) {
                            sanitized
                        } else {
                            val trimmed = sanitized.removePrefix("+").removePrefix("7")
                            "+" + trimmed
                        }
                    },
                    label = { Text("Телефон") },
                    placeholder = { Text("9XXXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.requestPhone(activity, context) },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Продолжить")

                }

                //    временный код. Удалить--------------------------------------

                OutlinedButton(
                    onClick = { viewModel.quickTestSignIn(activity, context) },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Тестовый вход (+79999999999)")
                }
                //   до сюда-------------------------------------------
            }


            if (viewModel.stage == AuthViewModel.AuthStage.PasswordLogin) {
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (viewModel.showPasswordLogin) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.showPasswordLogin = !viewModel.showPasswordLogin
                        }) {
                            Icon(
                                imageVector = if (viewModel.showPasswordLogin) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (viewModel.showPasswordLogin) {
                                    "Скрыть пароль"
                                } else {
                                    "Показать пароль"
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.loginWithPassword() },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Войти")
                }

                TextButton(
                    onClick = {
                        viewModel.enterPasswordReset(activity, context)
                    },
                    enabled = !viewModel.isLoading
                ) {
                    Text("Войти через код проверки")
                }
            }

            if (viewModel.stage == AuthViewModel.AuthStage.Code) {
                OutlinedTextField(
                    value = viewModel.code,
                    onValueChange = { viewModel.code = it },
                    label = { Text("Код из SMS") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.confirmCode() },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Войти")
                }

                if (viewModel.resendSeconds > 0) {
                    Text("Повторная отправка через ${viewModel.resendSeconds} сек.")
                } else {
                    TextButton(
                        onClick = { viewModel.sendCode(activity, context, true) },
                        enabled = !viewModel.isLoading
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("Не пришел код, ")
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                append("Отправить")
                                pop()
                                append(" еще раз")
                            }
                        )
                    }
                }
            }

            if (viewModel.stage == AuthViewModel.AuthStage.SetPassword) {
                Text(
                    text = if (viewModel.isPasswordReset) "Новый пароль" else "Установите пароль",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (viewModel.showPasswordSetup) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.showPasswordSetup = !viewModel.showPasswordSetup
                        }) {
                            Icon(
                                imageVector = if (viewModel.showPasswordSetup) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (viewModel.showPasswordSetup) {
                                    "Скрыть пароль"
                                } else {
                                    "Показать пароль"
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.savePassword() },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить и войти")
                }
                OutlinedButton(
                    onClick = { viewModel.disablePasswordAndContinue() },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Войти без пароля")
                }
            }

            if (viewModel.errorText != null) {
                Text(
                    text = viewModel.errorText!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (viewModel.isLoading) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text("Подождите…")
                }
            },
            confirmButton = {}
        )
    }
}