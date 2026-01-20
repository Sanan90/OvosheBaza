package com.example.ovoshebaza

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

const val ORDER_STATUS_CHANNEL_ID = "order_status"
private const val ORDER_STATUS_CHANNEL_NAME = "Статусы заказов"
private const val ORDER_STATUS_CHANNEL_DESC = "Уведомления об изменении статуса заказа"

fun createOrderStatusNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        ORDER_STATUS_CHANNEL_ID,
        ORDER_STATUS_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = ORDER_STATUS_CHANNEL_DESC
    }
    manager.createNotificationChannel(channel)
}

fun showOrderStatusNotification(context: Context, title: String, body: String) {
    createOrderStatusNotificationChannel(context)

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, ORDER_STATUS_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(System.currentTimeMillis().toInt(), notification)
}

fun saveFcmToken(
    token: String,
    onDone: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    val data = mapOf(
        "fcmTokens" to FieldValue.arrayUnion(token),
        "updatedAt" to System.currentTimeMillis()
    )

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
        .set(data, SetOptions.merge())
        .addOnSuccessListener { onDone() }
        .addOnFailureListener { e -> onError(e.message ?: "Не удалось сохранить FCM токен") }
}

fun fetchAndSaveFcmToken(
    onError: (String) -> Unit = {}
) {
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token -> saveFcmToken(token, onError = onError) }
        .addOnFailureListener { e ->
            onError(e.message ?: "Не удалось получить FCM токен")
        }
}

fun removeFcmToken(
    onDone: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token ->
            val data = mapOf(
                "fcmTokens" to FieldValue.arrayRemove(token),
                "updatedAt" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    FirebaseMessaging.getInstance().deleteToken()
                        .addOnSuccessListener { onDone() }
                        .addOnFailureListener { e ->
                            onError(e.message ?: "Не удалось удалить FCM токен")
                        }
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Не удалось удалить FCM токен")
                }
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Не удалось получить FCM токен")
        }
}


fun sendBroadcastNotification(
    title: String = "Овощебаза",
    message: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val functions = Firebase.functions
    val payload = mapOf(
        "title" to title,
        "text" to message
    )

    functions
        .getHttpsCallable("sendBroadcastNotification")
        .call(payload)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e ->
            onError(e.message ?: "Ошибка отправки уведомления")
        }
}


@Composable
fun NotificationSetup() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var userId by remember { mutableStateOf(auth.currentUser?.uid) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchAndSaveFcmToken()
        }
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            userId = firebaseAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        createOrderStatusNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@LaunchedEffect
            }
        }
        fetchAndSaveFcmToken()
    }
}

class OrderMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Статус заказа изменён"
        val body = message.notification?.body ?: return
        showOrderStatusNotification(applicationContext, title, body)
    }

    override fun onNewToken(token: String) {
        saveFcmToken(token)
    }
}