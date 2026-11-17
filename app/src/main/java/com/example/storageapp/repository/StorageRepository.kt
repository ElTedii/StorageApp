package com.example.storageapp.repository

import android.content.Context
import com.example.storageapp.ProductoItem
import com.example.storageapp.data.StorageDbHelper
import com.example.storageapp.model.Product
import com.example.storageapp.model.ProductImage
import com.example.storageapp.model.User
import com.example.storageapp.network.ApiClient
import com.example.storageapp.network.EmailCheckResult
import com.example.storageapp.network.RemoteUserResult
import kotlin.concurrent.thread

sealed class RegisterResult {
    data object SuccessOnline : RegisterResult()
    data object SavedOfflinePending : RegisterResult()
    data object EmailAlreadyExists : RegisterResult()
    data object EmailAlreadyExistsLocal : RegisterResult()
    data object LocalError : RegisterResult()
    data object ServerError : RegisterResult()
}

sealed class ProfileUpdateResult {
    data object SuccessOnline : ProfileUpdateResult()
    data object SavedOfflinePending : ProfileUpdateResult()
    data object WrongPassword : ProfileUpdateResult()
    data object LocalError : ProfileUpdateResult()
}

class StorageRepository(context: Context) {

    private val dbHelper = StorageDbHelper.getInstance(context)

    fun registrarUsuario(user: User): Boolean {
        val exitoLocal = dbHelper.registrarUsuario(user)
        if (exitoLocal) {
            thread { ApiClient.sincronizarUsuario(user) }
        }
        return exitoLocal
    }

    fun registrarUsuarioConValidacionOnline(user: User): RegisterResult {
        if (dbHelper.existeCorreoLocal(user.correo)) {
            return RegisterResult.EmailAlreadyExistsLocal
        }
        val check = ApiClient.verificarCorreoDisponible(user.correo)
        return when (check) {
            EmailCheckResult.Exists -> RegisterResult.EmailAlreadyExists
            EmailCheckResult.Available -> {
                val remoto = ApiClient.sincronizarUsuario(user)
                when (remoto) {
                    RemoteUserResult.Success -> {
                        val ok = dbHelper.registrarUsuario(user.copy(estadoRegistro = "CONFIRMADO", sincronizado = 1))
                        if (ok) RegisterResult.SuccessOnline else RegisterResult.LocalError
                    }
                    RemoteUserResult.Conflict -> RegisterResult.EmailAlreadyExists
                    is RemoteUserResult.ServerError -> RegisterResult.ServerError
                    RemoteUserResult.NetworkError -> guardarComoPendiente(user)
                }
            }
            EmailCheckResult.NetworkError -> guardarComoPendiente(user)
            is EmailCheckResult.ServerError -> RegisterResult.ServerError
        }
    }

    private fun guardarComoPendiente(user: User): RegisterResult {
        val ok = dbHelper.registrarUsuario(user.copy(estadoRegistro = "PENDIENTE", sincronizado = 0))
        return if (ok) RegisterResult.SavedOfflinePending else RegisterResult.LocalError
    }

    fun autenticarUsuario(correo: String, pass: String): User? = dbHelper.autenticarUsuario(correo, pass)

    fun obtenerUsuarioPorCorreo(correo: String): User? = dbHelper.obtenerUsuarioPorCorreo(correo)

    fun actualizarPerfil(correo: String, currentPassword: String, nuevoAlias: String, nuevaPass: String?, nuevoAvatar: ByteArray?): ProfileUpdateResult {
        val actual = dbHelper.obtenerUsuarioPorCorreo(correo) ?: return ProfileUpdateResult.LocalError
        if (actual.contrasenaHash != currentPassword) return ProfileUpdateResult.WrongPassword

        val okLocal = dbHelper.actualizarPerfilUsuario(correo, nuevoAlias, nuevaPass, nuevoAvatar)
        if (!okLocal) return ProfileUpdateResult.LocalError

        val actualizado = dbHelper.obtenerUsuarioPorCorreo(correo) ?: return ProfileUpdateResult.LocalError
        return when (ApiClient.sincronizarUsuario(actualizado)) {
            RemoteUserResult.Success -> {
                dbHelper.marcarUsuarioSincronizado(correo)
                ProfileUpdateResult.SuccessOnline
            }
            else -> {
                dbHelper.marcarUsuarioPendienteSync(correo)
                ProfileUpdateResult.SavedOfflinePending
            }
        }
    }

    fun obtenerCatalogo(onSyncComplete: (() -> Unit)? = null): List<ProductoItem> {
        sincronizarPendientesConServidor { onSyncComplete?.invoke() }
        return dbHelper.obtenerTodosLosProductos()
    }

    fun obtenerCatalogoLocal(): List<ProductoItem> = dbHelper.obtenerTodosLosProductos()

    fun obtenerProductoPorSku(sku: String): Product? = dbHelper.obtenerProductoPorSku(sku)
    fun obtenerImagenesDeProducto(sku: String): List<ByteArray> = dbHelper.obtenerImagenesDeProducto(sku)
    fun obtenerImagenesProducto(sku: String): List<ProductImage> = dbHelper.obtenerImagenesProducto(sku)

    fun guardarProducto(product: Product, imagenes: List<ByteArray>): Boolean {
        val prodParaGuardar = product.copy(sincronizado = 0)
        val exitoLocal = dbHelper.guardarOActualizarProducto(prodParaGuardar, imagenes)
        if (exitoLocal) {
            thread {
                val exitoRemoto = ApiClient.sincronizarProducto(prodParaGuardar, imagenes)
                if (exitoRemoto) dbHelper.marcarProductoSincronizado(product.sku)
            }
        }
        return exitoLocal
    }

    fun eliminarProducto(sku: String): Boolean {
        val exitoLocal = dbHelper.eliminarProducto(sku)
        if (exitoLocal) {
            thread {
                val exitoRemoto = ApiClient.eliminarProductoRemoto(sku)
                if (!exitoRemoto) android.util.Log.e("StorageRepository", "El servidor no pudo borrar el SKU: $sku")
            }
        }
        return exitoLocal
    }

    fun registrarMovimiento(sku: String, correoUsuario: String, tipo: String, cantidad: Int, nuevoStock: Int): Boolean {
        val exitoLocal = dbHelper.registrarMovimiento(sku, correoUsuario, tipo, cantidad, nuevoStock)
        if (exitoLocal) sincronizarPendientesConServidor()
        return exitoLocal
    }

    fun sincronizarPendientesConServidor(onComplete: ((Int) -> Unit)? = null) {
        thread {
            var subidos = 0
            try {
                val usuariosPendientes = dbHelper.obtenerUsuariosPendientes()
                for (u in usuariosPendientes) {
                    when (ApiClient.sincronizarUsuario(u)) {
                        RemoteUserResult.Success -> { dbHelper.marcarUsuarioSincronizado(u.correo); subidos++ }
                        RemoteUserResult.Conflict -> if (u.estadoRegistro == "PENDIENTE") dbHelper.marcarUsuarioConflicto(u.correo)
                        else -> {}
                    }
                }
                val prodPendientes = dbHelper.obtenerProductosNoSincronizados()
                for (prod in prodPendientes) {
                    val fotos = dbHelper.obtenerImagenesDeProducto(prod.sku)
                    if (ApiClient.sincronizarProducto(prod, fotos)) { dbHelper.marcarProductoSincronizado(prod.sku); subidos++ }
                }
                val movPendientes = dbHelper.obtenerMovimientosNoSincronizados()
                for (mov in movPendientes) {
                    if (ApiClient.sincronizarMovimiento(mov)) dbHelper.marcarMovimientoSincronizado(mov.idMovimiento)
                }
                val remotos = ApiClient.descargarProductosRemotos()
                if (remotos.isNotEmpty()) {
                    val skusRemotos = remotos.map { it.first.sku }.toSet()
                    val locales = dbHelper.obtenerTodosLosProductos()
                    for (local in locales) {
                        val prodCompleto = dbHelper.obtenerProductoPorSku(local.sku)
                        if (!skusRemotos.contains(local.sku) && (prodCompleto?.sincronizado == 1)) dbHelper.eliminarProducto(local.sku)
                    }
                    for ((prodRemoto, fotosRemotas) in remotos) {
                        val local = dbHelper.obtenerProductoPorSku(prodRemoto.sku)
                        if (local == null || local.sincronizado == 1) {
                            dbHelper.guardarOActualizarProducto(prodRemoto, fotosRemotas)
                            dbHelper.marcarProductoSincronizado(prodRemoto.sku)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StorageRepository", "Error al sincronizar datos", e)
            } finally {
                onComplete?.invoke(subidos)
            }
        }
    }
}
