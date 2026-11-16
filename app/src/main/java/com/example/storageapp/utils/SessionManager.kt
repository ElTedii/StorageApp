package com.example.storageapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

// PATRÓN SINGLETON: Instancia única para controlar la sesión activa del usuario
object SessionManager {
    private const val PREF_NAME = "StorageAppSession"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    private const val KEY_CORREO = "correo"
    private const val KEY_ALIAS = "alias"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_AVATAR = "avatarBase64"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun guardarSesion(context: Context, correo: String, alias: String, nombre: String, avatarBytes: ByteArray? = null) {
        val editor = getPrefs(context).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_CORREO, correo)
        editor.putString(KEY_ALIAS, alias)
        editor.putString(KEY_NOMBRE, nombre)
        if (avatarBytes != null) {
            editor.putString(KEY_AVATAR, Base64.encodeToString(avatarBytes, Base64.DEFAULT))
        }
        editor.apply()
    }

    fun actualizarPerfil(context: Context, nuevoAlias: String, nuevoAvatar: ByteArray?) {
        val editor = getPrefs(context).edit()
        if (nuevoAlias.isNotEmpty()) {
            editor.putString(KEY_ALIAS, nuevoAlias)
        }
        if (nuevoAvatar != null) {
            editor.putString(KEY_AVATAR, Base64.encodeToString(nuevoAvatar, Base64.DEFAULT))
        }
        editor.apply()
    }

    fun obtenerAlias(context: Context): String = getPrefs(context).getString(KEY_ALIAS, "Usuario") ?: "Usuario"

    fun obtenerCorreo(context: Context): String = getPrefs(context).getString(KEY_CORREO, "operador@storageapp.com") ?: "operador@storageapp.com"

    fun obtenerAvatar(context: Context): ByteArray? {
        val base64 = getPrefs(context).getString(KEY_AVATAR, null) ?: return null
        return try {
            Base64.decode(base64, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    fun haySesionActiva(context: Context): Boolean = getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)

    fun cerrarSesion(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}