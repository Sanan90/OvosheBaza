package com.example.ovoshebaza


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

data class UserProfile(
    val name: String = "",
    val phone: String = "",
    val addresses: List<String> = emptyList(),
    val lastAddress: String = "",
    val hasPassword: Boolean = false
)

data class OrderItem(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val sum: Double
)

data class OrderSummary(
    val orderId: String,
    val createdAt: Long,
    val total: Double,
    val itemsCount: Int,
    val channel: String,
    val status: String,
    val items: List<OrderItem>
)

object UserProfileCache {
    private val profiles = mutableMapOf<String, UserProfile?>()

    fun get(uid: String): UserProfile? = profiles[uid]

    fun set(uid: String, profile: UserProfile?) {
        profiles[uid] = profile
    }
}

fun getCachedUserProfile(uid: String?): UserProfile? {
    if (uid.isNullOrBlank()) {
        return null
    }
    return UserProfileCache.get(uid)
}

fun setCachedUserProfile(uid: String?, profile: UserProfile?) {
    if (uid.isNullOrBlank()) {
        return
    }
    UserProfileCache.set(uid, profile)
}


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
            val mapped = mapUserProfile(doc)
            setCachedUserProfile(user.uid, mapped)
            onResult(mapped)
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Не удалось загрузить профиль")
        }
}

fun loadUserOrders(
    limit: Int = 10,
    onResult: (List<OrderSummary>) -> Unit,
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onResult(emptyList())
        return
    }

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
        .collection("orders")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit.toLong())
        .get()
        .addOnSuccessListener { snapshot ->
            val orders = snapshot.documents.map { doc -> mapOrderSummary(doc) }
            onResult(orders)
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Не удалось загрузить список заказов")
        }
}

fun listenUserOrders(
    limit: Int = 10,
    onResult: (List<OrderSummary>) -> Unit,
    onError: (String) -> Unit = {}
): ListenerRegistration? {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onResult(emptyList())
        return null
    }

    return FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
        .collection("orders")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit.toLong())
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Не удалось загрузить список заказов")
                return@addSnapshotListener
            }
            val orders = snapshot?.documents?.map { doc -> mapOrderSummary(doc) } ?: emptyList()
            onResult(orders)
        }
}

fun listenUserProfile(
    onResult: (UserProfile?) -> Unit,
    onError: (String) -> Unit = {}
): ListenerRegistration? {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onResult(null)
        return null
    }

    return FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Не удалось загрузить профиль")
                return@addSnapshotListener
            }
            val mapped = snapshot?.let { mapUserProfile(it) }
            setCachedUserProfile(user.uid, mapped)
            onResult(mapped)
        }
}

private fun mapUserProfile(doc: DocumentSnapshot): UserProfile? {
    if (!doc.exists()) {
        return null
    }
    val storedAddress = (doc.getString("address") ?: "").trim()
    val addresses = (doc.get("addresses") as? List<*>)?.mapNotNull { it?.toString()?.trim() }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        ?: emptyList()
    val mergedAddresses = if (storedAddress.isNotBlank() && !addresses.contains(storedAddress)) {
        addresses + storedAddress
    } else {
        addresses
    }
    val lastAddress = (doc.getString("lastAddress") ?: "").trim().ifBlank {
        storedAddress.ifBlank { mergedAddresses.lastOrNull().orEmpty() }
    }

    val hasPassword = doc.getBoolean("passwordEnabled") == true

    return UserProfile(
        name = (doc.getString("name") ?: "").trim(),
        phone = (doc.getString("phone") ?: "").trim(),
        addresses = mergedAddresses,
        lastAddress = lastAddress,
        hasPassword = hasPassword
    )
}

fun loadPasswordStatusForPhone(
    phone: String,
    onResult: (Boolean) -> Unit,
    onError: (String) -> Unit = {}
) {
    val trimmed = phone.trim()
    if (trimmed.isBlank()) {
        onResult(false)
        return
    }

    FirebaseFirestore.getInstance()
        .collection("users")
        .whereEqualTo("phone", trimmed)
        .limit(1)
        .get()
        .addOnSuccessListener { snapshot ->
            val hasPassword = snapshot.documents.firstOrNull()
                ?.getBoolean("passwordEnabled") == true
            onResult(hasPassword)
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Не удалось проверить пароль")
        }
}

fun loadPasswordStatusForUser(
    uid: String,
    onResult: (Boolean) -> Unit,
    onError: (String) -> Unit = {}
) {
    if (uid.isBlank()) {
        onResult(false)
        return
    }

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(uid)
        .get()
        .addOnSuccessListener { doc ->
            val hasPassword = doc.getBoolean("passwordEnabled") == true
            onResult(hasPassword)
        }
        .addOnFailureListener { e ->
            onError(e.message ?: "Не удалось загрузить статус пароля")
        }
}

fun updatePasswordStatus(
    enabled: Boolean,
    onDone: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    val data = mapOf(
        "passwordEnabled" to enabled,
        "passwordUpdatedAt" to System.currentTimeMillis()
    )

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
        .set(data, SetOptions.merge())
        .addOnSuccessListener { onDone() }
        .addOnFailureListener { e ->
            onError(e.message ?: "Не удалось сохранить пароль")
        }
}


private fun mapOrderSummary(doc: DocumentSnapshot): OrderSummary {
    val createdAt = doc.getLong("createdAt") ?: 0L
    val total = doc.getDouble("total") ?: 0.0
    val rawItems = doc.get("items") as? List<*>
    val items = rawItems?.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val id = map["id"]?.toString() ?: return@mapNotNull null
        val name = map["name"]?.toString() ?: "Товар"
        val quantity = (map["quantity"] as? Number)?.toDouble() ?: 0.0
        val unit = map["unit"]?.toString() ?: ""
        val price = (map["price"] as? Number)?.toDouble() ?: 0.0
        val sum = (map["sum"] as? Number)?.toDouble() ?: 0.0
        OrderItem(
            id = id,
            name = name,
            quantity = quantity,
            unit = unit,
            price = price,
            sum = sum
        )
    } ?: emptyList()
    val itemsCount = items.size
    val channel = doc.getString("channel") ?: ""
    val status = doc.getString("status") ?: "RECEIVED"

    return OrderSummary(
        orderId = doc.id,
        createdAt = createdAt,
        total = total,
        itemsCount = itemsCount,
        channel = channel,
        status = status,
        items = items
    )
}


    fun saveUserName(
name: String,
onDone: () -> Unit = {},
onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    val data = mapOf(
        "name" to name.trim(),
        "updatedAt" to System.currentTimeMillis()
    )

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
        .set(data, SetOptions.merge())
        .addOnSuccessListener { onDone() }
        .addOnFailureListener { e -> onError(e.message ?: "Не удалось сохранить профиль") }
}

fun addUserAddress(
    address: String,
    onDone: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    val trimmed = address.trim()
    if (trimmed.isBlank()) {
        onDone()
        return
    }

    val ref = FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)

    FirebaseFirestore.getInstance().runTransaction { tr ->
        val snap = tr.get(ref)
        val storedAddress = (snap.getString("address") ?: "").trim()
        val addresses = (snap.get("addresses") as? List<*>)?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()
        val updatedAddresses = buildList {
            addAll(addresses)
            if (storedAddress.isNotBlank() && !contains(storedAddress)) {
                add(storedAddress)
            }
            if (trimmed.isNotBlank() && !contains(trimmed)) {
                add(trimmed)
            }
        }
        tr.set(
            ref,
            mapOf(
                "addresses" to updatedAddresses,
                "lastAddress" to trimmed,
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        )
        null
    }.addOnSuccessListener { onDone() }
        .addOnFailureListener { e -> onError(e.message ?: "Не удалось сохранить адрес") }
}

fun deleteUserAddress(
    address: String,
    onDone: (List<String>, String) -> Unit = { _, _ -> },
    onError: (String) -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("Пользователь не авторизован")
        return
    }

    val trimmed = address.trim()
    val ref = FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)

    FirebaseFirestore.getInstance().runTransaction { tr ->
        val snap = tr.get(ref)
        val addresses = (snap.get("addresses") as? List<*>)?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()
        val updatedAddresses = addresses.filterNot { it == trimmed }
        val lastAddress = (snap.getString("lastAddress") ?: "").trim()
        val updatedLastAddress = if (lastAddress == trimmed) {
            updatedAddresses.lastOrNull().orEmpty()
        } else {
            lastAddress
        }
        tr.set(
            ref,
            mapOf(
                "addresses" to updatedAddresses,
                "lastAddress" to updatedLastAddress,
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        )
        updatedAddresses to updatedLastAddress
    }.addOnSuccessListener { (updated, last) ->
        onDone(updated, last)
    }.addOnFailureListener { e ->
        onError(e.message ?: "Не удалось удалить адрес")
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

    val ref = FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)

    FirebaseFirestore.getInstance().runTransaction { tr ->
        val snap = tr.get(ref)
        val storedAddress = (snap.getString("address") ?: "").trim()
        val addresses = (snap.get("addresses") as? List<*>)?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()
        val trimmedAddress = address.trim()
        val updatedAddresses = buildList {
            addAll(addresses)
            if (storedAddress.isNotBlank() && !contains(storedAddress)) {
                add(storedAddress)
            }
            if (trimmedAddress.isNotBlank() && !contains(trimmedAddress)) {
                add(trimmedAddress)
            }
        }

        tr.set(
            ref,
            mapOf(
                "name" to name.trim(),
                "phone" to phone.trim(),
                "addresses" to updatedAddresses,
                "lastAddress" to trimmedAddress,
                "updatedAt" to now
            ),
            SetOptions.merge()
        )
        null
    }.addOnSuccessListener { onDone() }
        .addOnFailureListener { e -> onError(e.message ?: "Не удалось сохранить профиль") }
}

fun saveOrderToHistory(
    order: Map<String, Any>,
    channel: String, // "TELEGRAM" / "WHATSAPP"
    onDone: (String) -> Unit = {},
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
    if (!payload.containsKey("status")) payload["status"] = "RECEIVED"
    if (!payload.containsKey("statusUpdatedAt")) payload["statusUpdatedAt"] = System.currentTimeMillis()

    ref.set(payload)
        .addOnSuccessListener { onDone(ref.id) }
        .addOnFailureListener { e -> onError(e.message ?: "Не удалось сохранить заказ в историю") }
}
