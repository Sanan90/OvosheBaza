package com.example.ovoshebaza.ui.auth

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.ovoshebaza.loadPasswordStatusForPhone
import com.example.ovoshebaza.loadPasswordStatusForUser
import com.example.ovoshebaza.passwordEmailForPhone
import com.example.ovoshebaza.updatePasswordStatus
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    enum class AuthStage {
        Phone,
        Code,
        PasswordLogin,
        SetPassword
    }

    private val auth = FirebaseAuth.getInstance()
    private val phonePrefix = "+"
    private var toastContext: Context? = null

    var phone by mutableStateOf(phonePrefix)
    var code by mutableStateOf("")
    var password by mutableStateOf("")
    var resendSeconds by mutableStateOf(0)

    var isLoading by mutableStateOf(false)
    var errorText by mutableStateOf<String?>(null)

    private var verificationId by mutableStateOf<String?>(null)
    private var resendToken by mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null)
    var stage by mutableStateOf(AuthStage.Phone)
    private var pendingPhone by mutableStateOf("")
    var isPasswordReset by mutableStateOf(false)
    var showPasswordLogin by mutableStateOf(false)
    var showPasswordSetup by mutableStateOf(false)
    private var openCodeAfterSend by mutableStateOf(false)

    private val callbacks = object : OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            isLoading = true
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user == null) {
                            errorText = "Не удалось авторизоваться"
                            return@addOnCompleteListener
                        }
                        if (isPasswordReset) {
                            stage = AuthStage.SetPassword
                            return@addOnCompleteListener
                        }
                        loadPasswordStatusForUser(
                            uid = user.uid,
                            onResult = { hasPassword ->
                                if (hasPassword) {
                                    signedInCallback?.invoke()
                                } else {
                                    stage = AuthStage.SetPassword
                                }
                            },
                            onError = { msg -> errorText = msg }
                        )
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
            openCodeAfterSend = false
        }

        override fun onCodeSent(
            verifId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            isLoading = false
            verificationId = verifId
            resendToken = token
            resendSeconds = 60
            if (openCodeAfterSend) {
                stage = AuthStage.Code
                openCodeAfterSend = false
            }
            toastContext?.let {
                Toast.makeText(it, "Код отправлен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var signedInCallback: (() -> Unit)? = null

    fun updateSignedInCallback(onSignedIn: () -> Unit) {
        signedInCallback = onSignedIn
    }

    fun requestPhone(activity: Activity, context: Context) {
        errorText = null
        toastContext = context.applicationContext
        val phoneTrim = phone.trim()
        if (phoneTrim.length <= phonePrefix.length || !phoneTrim.startsWith(phonePrefix)) {
            errorText = "Введите номер в формате +7..., начиная с 9"
            return
        }

        isLoading = true
        pendingPhone = phoneTrim
        loadPasswordStatusForPhone(
            phone = phoneTrim,
            onResult = { hasPassword ->
                isLoading = false
                if (hasPassword) {
                    stage = AuthStage.PasswordLogin
                    password = ""
                    verificationId = null
                    resendSeconds = 0
                    resendToken = null
                } else {
                    isPasswordReset = false
                    openCodeAfterSend = true
                    sendCode(activity, context, false)
                }
            },
            onError = { msg ->
                isLoading = false
                errorText = msg
            }
        )
    }

    fun quickTestSignIn(activity: Activity, context: Context) {
        phone = "+79999999999"
        code = ""
        requestPhone(activity, context)
    }

    fun sendCode(activity: Activity, context: Context, forceResend: Boolean) {
        errorText = null
        toastContext = context.applicationContext
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
                    val user = auth.currentUser
                    if (user == null) {
                        errorText = "Не удалось авторизоваться"
                        return@addOnCompleteListener
                    }
                    if (isPasswordReset) {
                        stage = AuthStage.SetPassword
                        return@addOnCompleteListener
                    }
                    loadPasswordStatusForUser(
                        uid = user.uid,
                        onResult = { hasPassword ->
                            if (hasPassword) {
                                signedInCallback?.invoke()
                            } else {
                                stage = AuthStage.SetPassword
                            }
                        },
                        onError = { msg -> errorText = msg }
                    )
                } else {
                    errorText = task.exception?.localizedMessage ?: "Неверный код"
                }
            }
    }

    fun loginWithPassword() {
        errorText = null
        val passwordTrim = password.trim()
        if (passwordTrim.isBlank()) {
            errorText = "Введите пароль"
            return
        }
        val phoneTrim = pendingPhone.ifBlank { phone.trim() }
        if (phoneTrim.isBlank()) {
            errorText = "Введите номер телефона"
            stage = AuthStage.Phone
            return
        }
        isLoading = true
        val email = passwordEmailForPhone(phoneTrim)
        auth.signInWithEmailAndPassword(email, passwordTrim)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    signedInCallback?.invoke()
                } else {
                    errorText = task.exception?.localizedMessage ?: "Неверный пароль"
                }
            }
    }

    fun enterPasswordReset(activity: Activity, context: Context) {
        isPasswordReset = true
        code = ""
        verificationId = null
        resendSeconds = 0
        sendCode(activity, context, false)
        stage = AuthStage.Code
    }

    fun savePassword() {
        errorText = null
        val passwordTrim = password.trim()
        if (passwordTrim.isBlank()) {
            errorText = "Введите пароль"
            return
        }
        val user = auth.currentUser
        if (user == null) {
            errorText = "Не удалось авторизоваться"
            return
        }
        val phoneValue = user.phoneNumber ?: pendingPhone
        if (phoneValue.isBlank()) {
            errorText = "Не удалось определить номер телефона"
            return
        }
        val email = passwordEmailForPhone(phoneValue)
        val hasEmailProvider = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
        val task = if (hasEmailProvider) {
            user.updatePassword(passwordTrim)
        } else {
            user.linkWithCredential(
                EmailAuthProvider.getCredential(email, passwordTrim)
            )
        }
        isLoading = true
        task.addOnCompleteListener { updateTask ->
            if (!updateTask.isSuccessful) {
                isLoading = false
                errorText = updateTask.exception?.localizedMessage ?: "Не удалось сохранить пароль"
                return@addOnCompleteListener
            }
            updatePasswordStatus(
                enabled = true,
                onDone = {
                    isLoading = false
                    signedInCallback?.invoke()
                },
                onError = { msg ->
                    isLoading = false
                    errorText = msg
                }
            )
        }
    }

    fun disablePasswordAndContinue() {
        errorText = null
        val user = auth.currentUser
        if (user == null) {
            errorText = "Не удалось авторизоваться"
            return
        }
        isLoading = true
        val hasEmailProvider = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
        val task = if (hasEmailProvider) {
            user.unlink(EmailAuthProvider.PROVIDER_ID)
        } else {
            null
        }

        val finish: () -> Unit = {
            updatePasswordStatus(
                enabled = false,
                onDone = {
                    isLoading = false
                    signedInCallback?.invoke()
                },
                onError = { msg ->
                    isLoading = false
                    errorText = msg
                }
            )
        }

        if (task == null) {
            finish()
        } else {
            task.addOnCompleteListener { unlinkTask ->
                if (!unlinkTask.isSuccessful) {
                    isLoading = false
                    errorText = unlinkTask.exception?.localizedMessage ?: "Не удалось отключить пароль"
                    return@addOnCompleteListener
                }
                finish()
            }
        }
    }

    fun tickResendCountdown() {
        viewModelScope.launch {
            if (verificationId != null && resendSeconds > 0) {
                delay(1000)
                resendSeconds -= 1
            }
        }
    }
}