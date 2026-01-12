package com.example.ovoshebaza

import com.google.firebase.firestore.DocumentSnapshot

// Переводим Product -> Map для Firestore
fun Product.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "category" to category.name,
    "price" to price,
    "unit" to unit.name,
    "imageUrl" to imageUrl,
    "originCountry" to originCountry,
    "description" to description,
    "isPopular" to isPopular,
    "isNew" to isNew,
    "inStock" to inStock
)

// Переводим документ Firestore -> Product
fun DocumentSnapshot.toProduct(): Product? {
    val id = getString("id") ?: id              // если поля id нет, берём id документа
    val name = getString("name") ?: return null
    val categoryStr = getString("category") ?: ProductCategory.OTHER.name
    val unitStr = getString("unit") ?: UnitType.KG.name

    val price = getDouble("price") ?: 0.0
    val imageUrl = getString("imageUrl")
    val originCountry = getString("originCountry")
    val description = getString("description")
    val isPopular = getBoolean("isPopular") ?: false
    val isNew = getBoolean("isNew") ?: false
    val inStock = getBoolean("inStock") ?: true

    val category = try {
        ProductCategory.valueOf(categoryStr)
    } catch (_: Exception) {
        ProductCategory.OTHER
    }

    val unit = try {
        UnitType.valueOf(unitStr)
    } catch (_: Exception) {
        UnitType.KG
    }

    return Product(
        id = id,
        name = name,
        category = category,
        price = price,
        unit = unit,
        imageUrl = imageUrl,
        originCountry = originCountry,
        description = description,
        isPopular = isPopular,
        isNew = isNew,
        inStock = inStock
    )
}