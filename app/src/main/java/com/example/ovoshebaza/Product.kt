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
    val id: String,                    // Уникальный id товара (пока просто строка)
    val name: String,                  // Название товара (например, "Помидор сливка")
    val category: ProductCategory,     // Категория (овощи, фрукты и т.п.)
    val price: Double,                 // Цена за 1 кг или 1 шт
    val unit: UnitType,                // Единица измерения (кг или шт)
    val originCountry: String?, // Страна происхождения (может быть null)
    val imageUrl: String?,             // URL картинки (может быть null)
    val description: String?,          // Описание товара (может быть null)
    val isPopular: Boolean,            // Флаг "популярный товар"
    val isNew: Boolean,                // Флаг "новинка"
    val inStock: Boolean               // В наличии или нет
    // Позже добавим сюда ссылки на фотографии
)


enum class Screen(val route: String, val label: String) {
    Catalog("catalog", "Каталог"),
    Cart("cart", "Корзина"),
    Request("request", "Заявка"),
    Profile("profile", "Профиль"),
    Admin("admin", "Админ"),
    ProductDetails("product/{productId}", "Товар")
}


// Временный список товаров (для теста приложения)
// В будущем мы будем загружать товары из "облака"
val sampleProducts = listOf(
    Product(
        id = "tomato_1",
        name = "Помидор сливка",
        category = ProductCategory.VEGETABLES,
        price = 180.0,
        unit = UnitType.KG,
        imageUrl = "https://images.pexels.com/photos/8390/food-wood-tomatoes.jpg", // просто пример
        originCountry = "Азербайджан",
        description = "Плотные и сладкие томаты для салатов, соусов и закаток.",
        isPopular = true,
        isNew = true,
        inStock = true
    ),
    Product(
        id = "cucumber_1",
        name = "Огурец тепличный",
        category = ProductCategory.VEGETABLES,
        price = 140.0,
        unit = UnitType.KG,
        imageUrl = "https://images.pexels.com/photos/143133/pexels-photo-143133.jpeg",
        originCountry = "Россия",
        description = "Хрустящие огурцы с тонкой кожицей — отлично для салатов.",
        isPopular = true,
        isNew = false,
        inStock = true
    ),
    Product(
        id = "potato_1",
        name = "Картофель молодой",
        category = ProductCategory.VEGETABLES,
        price = 60.0,
        unit = UnitType.KG,
        imageUrl = "https://images.pexels.com/photos/4110470/pexels-photo-4110470.jpeg",
        originCountry = null,
        description = "Молодой картофель с нежной мякотью, идеален для запекания.",
        isPopular = false,
        isNew = false,
        inStock = true
    ),
    Product(
        id = "apple_1",
        name = "Яблоко красное",
        category = ProductCategory.FRUITS,
        price = 130.0,
        unit = UnitType.KG,
        imageUrl = "https://images.pexels.com/photos/102104/pexels-photo-102104.jpeg",
        originCountry = "Турция",
        description = "Сочные красные яблоки с ярким ароматом.",
        isPopular = true,
        isNew = false,
        inStock = true
    ),
    Product(
        id = "banana_1",
        name = "Банан",
        category = ProductCategory.FRUITS,
        price = 150.0,
        unit = UnitType.KG,
        imageUrl = "https://images.pexels.com/photos/461208/pexels-photo-461208.jpeg",
        originCountry = "Эквадор",
        description = "Спелые сладкие бананы для перекуса и десертов.",
        isPopular = false,
        isNew = true,
        inStock = true
    ),
    Product(
        id = "greens_1",
        name = "Укроп пучок",
        category = ProductCategory.GREENS,
        price = 40.0,
        unit = UnitType.PIECE,
        imageUrl = "https://images.pexels.com/photos/1438672/pexels-photo-1438672.jpeg",
        originCountry = null,
        description = "Ароматный укроп для свежести и вкуса блюд.",
        isPopular = true,
        isNew = false,
        inStock = true
    )
)



// Один элемент в корзине
data class CartItem(
    val product: Product, // какой товар
    val quantity: Double  // сколько (кг или шт) — храним как число с плавающей точкой
)
