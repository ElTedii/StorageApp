package com.example.storageapp.model

data class Product(
    val sku: String, // Clave Primaria (PK)
    val nombre: String,
    val categoria: String,
    val marca: String,
    val modelo: String,
    val existencias: Int = 0,
    val puntoReorden: Int = 0,
    val descripcion: String? = null,
    val estado: String, // "BORRADOR" o "PUBLICADO"
    val fechaCreacion: String,
    val fechaModificacion: String,
    val sincronizado: Int = 0, // 0 = Pendiente, 1 = Sincronizado (Offline First)
    val correoUsuarioCreador: String? = null // FK hacia User.correo
)