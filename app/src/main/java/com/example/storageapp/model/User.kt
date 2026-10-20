package com.example.storageapp.model

data class User(
    val idUsuario: Int = 0,
    val nombre: String,
    val apellidos: String,
    val alias: String,
    val correo: String, // Clave única de negocio / login
    val contrasenaHash: String,
    val telefono: String? = null,
    val direccion: String? = null,
    val avatarBlob: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as User
        return idUsuario == other.idUsuario && correo == other.correo
    }

    override fun hashCode(): Int {
        var result = idUsuario
        result = 31 * result + correo.hashCode()
        return result
    }
}