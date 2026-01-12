package com.example.ovoshebaza

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

fun ensureUserDocExists(
    onDone: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    val db = FirebaseFirestore.getInstance()
    val ref = db.collection("users").document(user.uid)
    val now = System.currentTimeMillis()
    val phone = user.phoneNumber ?: ""

    // Транзакция: чтобы createdAt не перетирался и пустые поля не ломали существующие
    db.runTransaction { tr ->
        val snap = tr.get(ref)

        if (!snap.exists()) {
            // создаём нового с пустыми полями
            val data = mapOf(
                "name" to "",
                "address" to "",
                "phone" to phone,
                "createdAt" to now,
                "updatedAt" to now
            )
            tr.set(ref, data, SetOptions.merge())
        } else {
            // пользователь уже был — только обновляем phone/updatedAt
            val data = mapOf(
                "phone" to phone,
                "updatedAt" to now
            )
            tr.set(ref, data, SetOptions.merge())
        }
        null
    }.addOnSuccessListener {
        onDone()
    }.addOnFailureListener { e ->
        onError(e.message ?: "Не удалось создать пользователя в базе")
    }

}

