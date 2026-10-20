package com.example.storageapp.model

data class InventoryMovement(
    val idMovimiento: Int = 0,
    val skuProducto: String, // FK hacia Product.sku
    val correoUsuario: String, // FK hacia User.correo
    val tipoMovimiento: String, // "ENTRADA" o "SALIDA"
    val cantidad: Int,
    val fechaHora: String,
    val sincronizado: Int = 0 // 0 = Pendiente, 1 = Sincronizado (Offline First)
)