package com.example.ovoshebaza.ui.auth

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onSignedIn: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = remember { FirebaseAuth.getInstance() }
    val phonePrefix = "+7"

    var phone by remember { mutableStateOf(phonePrefix) }
    var code by remember { mutableStateOf("") }
    var resendSeconds by remember { mutableStateOf(0) }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }

    // callbacks должны быть стабильными
    val callbacks = remember {
        object : OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Авто-подтверждение (иногда срабатывает само)
                isLoading = true
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            onSignedIn()
                        } else {
                            errorText = task.exception?.localizedMessage ?: "Ошибка авторизации"
                        }
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                errorText = e.localizedMessage ?: "Ошибка отправки кода"
                verificationId = null
                resendSeconds = 0
            }

            override fun onCodeSent(
                verifId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                isLoading = false
                verificationId = verifId
                resendToken = token
                resendSeconds = 60
                Toast.makeText(context, "Код отправлен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendCode(forceResend: Boolean) {
        errorText = null
        val phoneTrim = phone.trim()

        if (phoneTrim.length <= phonePrefix.length || !phoneTrim.startsWith(phonePrefix)) {
            errorText = "Введите номер в формате +7..., начиная с 9"
            verificationId = null
            resendSeconds = 0
            return
        }

        isLoading = true

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneTrim)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (forceResend && resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    //    временный код. Удалить----------
    fun quickTestSignIn() {
        phone = "+79999999999"
        code = ""
        sendCode(false)
    }
//   до сюда--------------------------

    fun confirmCode() {
        errorText = null
        val vId = verificationId
        val codeTrim = code.trim()

        if (vId.isNullOrBlank()) {
            errorText = "Сначала запросите код"
            return
        }
        if (codeTrim.length < 4) {
            errorText = "Введите код из SMS"
            return
        }

        isLoading = true
        val credential = PhoneAuthProvider.getCredential(vId, codeTrim)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    onSignedIn()
                } else {
                    errorText = task.exception?.localizedMessage ?: "Неверный код"
                }
            }
    }

    LaunchedEffect(verificationId, resendSeconds) {
        if (verificationId != null && resendSeconds > 0) {
            delay(1000)
            resendSeconds -= 1
        }
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

            if (verificationId == null && resendSeconds == 0) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        val sanitized = input.replace(" ", "")
                        phone = if (sanitized.startsWith(phonePrefix)) {
                            sanitized
                        } else {
                            val trimmed = sanitized.removePrefix("+").removePrefix("7")
                            phonePrefix + trimmed
                        }
                    },
                    label = { Text("Телефон") },
                    placeholder = { Text("9XXXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { sendCode(false) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отправить код")

                }

                //    временный код. Удалить--------------------------------------

                OutlinedButton(
                    onClick = { quickTestSignIn() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Тестовый вход (+79999999999)")
                }
                //   до сюда-------------------------------------------
            }


                if (verificationId != null || resendSeconds > 0) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Код из SMS") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { confirmCode() },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Войти")
                    }

                    if (resendSeconds > 0) {
                        Text("Повторная отправка через $resendSeconds сек.")
                    } else {
                        TextButton(
                            onClick = { sendCode(true) },
                            enabled = !isLoading
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

                if (isLoading) {
                    CircularProgressIndicator()
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
