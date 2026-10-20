package com.example.storageapp.model

data class ProductImage(
    val idImagen: Int = 0,
    val skuProducto: String, // FK hacia Product.sku
    val imagenBlob: ByteArray, // BLOB para la imagen
    val descripcion: String? = null,
    val fechaCreacion: String,
    val fechaModificacion: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProductImage
        return idImagen == other.idImagen && skuProducto == other.skuProducto
    }

    override fun hashCode(): Int {
        var result = idImagen
        result = 31 * result + skuProducto.hashCode()
        return result
    }
}