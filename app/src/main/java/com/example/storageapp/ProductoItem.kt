package com.example.storageapp

data class ProductoItem(
    val idProducto: Int,
    val sku: String,
    val nombre: String,
    val categoria: String,
    val marca: String,
    val existencias: Int,
    val puntoReorden: Int
)