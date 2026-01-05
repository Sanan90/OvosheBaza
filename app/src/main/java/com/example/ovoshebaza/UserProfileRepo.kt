package com.example.ovoshebaza

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class UserProfile(
    val name: String = "",
    val phone: String = "",
    val address: String = ""
)

fun loadUserProfile(
    onResult: (UserProfile?) -> Unit,
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onResult(null)
        return
    }

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
        .get()
        .addOnSuccessListener { doc ->
            if (!doc.exists()) {
                onResult(null)
                return@addOnSuccessListener
            }
            onResult(
                UserProfile(
                    name = (doc.getString("name") ?: "").trim(),
                    phone = (doc.getString("phone") ?: "").trim(),
                    address = (doc.getString("address") ?: "").trim()
                )
            )
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Не удалось загрузить профиль")
        }
}

fun saveUserProfileFromOrder(
    name: String,
    phone: String,
    address: String,
    onDone: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    val now = System.currentTimeMillis()
    val data = mapOf(
        "name" to name.trim(),
        "phone" to phone.trim(),
        "address" to address.trim(),
        "updatedAt" to now
    )

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
        .set(data, SetOptions.merge())
        .addOnSuccessListener { onDone() }
        .addOnFailureListener { e -> onError(e.message ?: "Не удалось сохранить профиль") }
}

fun saveOrderToHistory(
    order: Map<String, Any>,
    channel: String, // "TELEGRAM" / "WHATSAPP"
    onDone: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    val db = FirebaseFirestore.getInstance()
    val ref = db.collection("users").document(user.uid).collection("orders").document()
    val payload = HashMap<String, Any>()
    payload.putAll(order)
    payload["uid"] = user.uid
    payload["orderId"] = ref.id
    payload["channel"] = channel
    if (!payload.containsKey("createdAt")) payload["createdAt"] = System.currentTimeMillis()

    ref.set(payload)
        .addOnSuccessListener { onDone() }
        .addOnFailureListener { e -> onError(e.message ?: "Не удалось сохранить заказ в историю") }
}
