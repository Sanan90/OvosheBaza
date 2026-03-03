package com.example.ovoshebaza.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// ViewModel для AdminScreen.
// Здесь живёт вся логика работы с Firebase — загрузка пользователей и их заказов.
// AdminScreen только показывает данные, ничего сам не грузит.
class AdminViewModel : ViewModel() {

    // Firestore — точка входа в базу данных
    private val db = FirebaseFirestore.getInstance()

    // ---------- ПОЛЬЗОВАТЕЛИ ----------

    // Список пользователей (обновляется автоматически, UI его наблюдает)
    var users by mutableStateOf<List<AdminUserSummary>>(emptyList())
        private set

    // Идёт ли сейчас загрузка пользователей
    var isLoadingUsers by mutableStateOf(false)
        private set

    // Текст ошибки при загрузке пользователей (null = ошибки нет)
    var usersError by mutableStateOf<String?>(null)
        private set

    // ---------- ЗАКАЗЫ ПОЛЬЗОВАТЕЛЕЙ ----------

    // Карта: uid пользователя -> список его заказов
    // mutableStateMapOf — как обычный Map, но Compose замечает изменения
    val userOrders = mutableStateMapOf<String, List<AdminOrderSummary>>()

    // Карта: uid пользователя -> грузятся ли сейчас его заказы
    val loadingOrders = mutableStateMapOf<String, Boolean>()

    // ---------- МЕТОДЫ ----------

    // Загружает список всех пользователей из Firestore.
    // Вызывается когда админ переключается на вкладку "Пользователи".
    fun loadUsers() {
        // Если уже грузим — не запускаем повторно
        if (isLoadingUsers) return

        isLoadingUsers = true
        usersError = null

        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                // Преобразуем каждый документ Firestore в удобный объект AdminUserSummary
                users = snapshot.documents.map { doc ->
                    val addresses = (doc.get("addresses") as? List<*>)
                        ?.mapNotNull { it?.toString() }
                        ?: emptyList()

                    AdminUserSummary(
                        uid = doc.id,
                        name = (doc.getString("name") ?: "").trim(),
                        phone = (doc.getString("phone") ?: "").trim(),
                        lastAddress = (doc.getString("lastAddress") ?: "").trim(),
                        addresses = addresses
                    )
                }.sortedBy { it.name.ifBlank { it.phone } } // сортируем по имени или телефону

                isLoadingUsers = false
            }
            .addOnFailureListener { e ->
                usersError = e.message ?: "Не удалось загрузить пользователей"
                isLoadingUsers = false
            }
    }

    // Загружает заказы конкретного пользователя по его uid.
    // Вызывается когда админ раскрывает карточку пользователя.
    fun loadOrdersForUser(uid: String) {
        // Если заказы уже загружены или грузятся — ничего не делаем
        if (userOrders.containsKey(uid) || loadingOrders[uid] == true) return

        loadingOrders[uid] = true

        db.collection("users")
            .document(uid)
            .collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING) // сначала новые
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents.map { doc ->
                    val items = doc.get("items") as? List<*>
                    AdminOrderSummary(
                        orderId = doc.id,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        total = doc.getDouble("total") ?: 0.0,
                        itemsCount = items?.size ?: 0
                    )
                }
                userOrders[uid] = orders
                loadingOrders[uid] = false
            }
            .addOnFailureListener {
                // При ошибке просто ставим пустой список, чтобы не крутился индикатор вечно
                userOrders[uid] = emptyList()
                loadingOrders[uid] = false
            }
    }
}