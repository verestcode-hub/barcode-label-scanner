package com.example.verestscannerapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Query("SELECT * FROM products WHERE barcode LIKE :searchCode LIMIT 1")
    suspend fun searchByBarcode(searchCode: String): Product?
    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsList(): List<Product>

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products WHERE barcode LIKE :prefix || '%'")
    suspend fun getProductsByPrefix(prefix: String): List<Product>
}