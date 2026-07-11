package com.example.verestscannerapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val barcode: String,
    val name: String,
    val plu: String,
    val useByDate: String = "",
    val groupPlu: String = "",
    val imagePath: String = "",
    val imageHistory: List<String> = emptyList()
)

data class JsonProduct(
    val EAN: String?,
    val NAME: String?,
    val NUM: String?,
    val USEDBYDATE: String?,
    val F_GROUPPLU: String?
)

class StringListConverter {
    @TypeConverter
    fun fromString(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(",")
    }

    @TypeConverter
    fun toString(list: List<String>): String {
        return list.joinToString(",")
    }
}