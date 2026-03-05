package com.example.ovoshebaza

// Категории товаров в магазине
enum class ProductCategory {
    VEGETABLES,   // Овощи
    FRUITS,       // Фрукты
    BERRIES,      // Ягоды
    GREENS,       // Зелень
    NUTS,         // Орехи / сухофрукты
    OTHER         // Другое
}

// Единицы измерения: кг или штука
enum class UnitType {
    KG,   // килограммы
    PIECE // штуки
}

// Модель одного товара
data class Product(
    val id: String,                // Уникальный id товара
    val name: String,              // Название товара (например, "Помидор сливка")
    val category: ProductCategory, // Категория (овощи, фрукты и т.п.)
    val price: Double,             // Цена за 1 кг или 1 шт
    val unit: UnitType,            // Единица измерения (кг или шт)
    val originCountry: String?,    // Страна происхождения (может быть null)
    val imageUrl: String?,         // URL картинки (может быть null)
    val description: String?,      // Описание товара (может быть null)
    val isPopular: Boolean,        // Флаг "популярный товар"
    val isNew: Boolean,            // Флаг "новинка"
    val inStock: Boolean           // В наличии или нет
)

// Экраны приложения
enum class Screen(val route: String, val label: String) {
    Catalog("catalog", "Каталог"),
    Cart("cart", "Корзина"),
    Request("request", "Заявка"),
    Profile("profile", "Профиль"),
    Admin("admin", "Админ"),
    ProductDetails("product/{productId}", "Товар")
}

// Один элемент в корзине
data class CartItem(
    val product: Product, // какой товар
    val quantity: Double  // сколько (кг или шт)
)