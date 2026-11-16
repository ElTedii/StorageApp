package com.example.storageapp.repository

import android.content.Context
import com.example.storageapp.ProductoItem
import com.example.storageapp.data.StorageDbHelper
import com.example.storageapp.model.Product
import com.example.storageapp.model.User

/**
 * PATRÓN REPOSITORY:
 * Actúa como única fuente de la verdad (Single Source of Truth) y mediador entre
 * la fuente de datos local (SQLite) y la futura API REST (Offline First).
 */
class StorageRepository(context: Context) {

    private val dbHelper = StorageDbHelper.getInstance(context)

    // --- Operaciones de Usuario ---
    fun registrarUsuario(user: User): Boolean = dbHelper.registrarUsuario(user)

    fun autenticarUsuario(correo: String, pass: String): User? = dbHelper.autenticarUsuario(correo, pass)

    fun actualizarPerfil(correo: String, nuevoAlias: String, nuevaPass: String?, nuevoAvatar: ByteArray?): Boolean {
        return dbHelper.actualizarPerfilUsuario(correo, nuevoAlias, nuevaPass, nuevoAvatar)
    }

    // --- Operaciones de Productos ---
    fun obtenerCatalogo(): List<ProductoItem> = dbHelper.obtenerTodosLosProductos()

    fun obtenerProductoPorSku(sku: String): Product? = dbHelper.obtenerProductoPorSku(sku)

    fun obtenerImagenesDeProducto(sku: String): List<ByteArray> = dbHelper.obtenerImagenesDeProducto(sku)

    fun guardarProducto(product: Product, imagenes: List<ByteArray>): Boolean {
        return dbHelper.guardarOActualizarProducto(product, imagenes)
    }

    fun eliminarProducto(sku: String): Boolean = dbHelper.eliminarProducto(sku)

    // --- Operaciones de Movimientos de Inventario ---
    fun registrarMovimiento(sku: String, correoUsuario: String, tipo: String, cantidad: Int, nuevoStock: Int): Boolean {
        return dbHelper.registrarMovimiento(sku, correoUsuario, tipo, cantidad, nuevoStock)
    }
}