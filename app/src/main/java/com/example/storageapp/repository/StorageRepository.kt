package com.example.storageapp.repository

import android.content.Context
import com.example.storageapp.ProductoItem
import com.example.storageapp.data.StorageDbHelper
import com.example.storageapp.model.InventoryMovement
import com.example.storageapp.model.Product
import com.example.storageapp.model.User
import com.example.storageapp.network.ApiClient
import kotlin.concurrent.thread

/**
 * PATRÓN REPOSITORY (Offline-First Bidireccional):
 * Maneja SQLite localmente y sincroniza altas, ediciones y borrados
 * con el servidor MySQL en Fedora Linux.
 */
class StorageRepository(context: Context) {

    private val dbHelper = StorageDbHelper.getInstance(context)

    fun registrarUsuario(user: User): Boolean {
        val exitoLocal = dbHelper.registrarUsuario(user)
        if (exitoLocal) {
            thread { ApiClient.sincronizarUsuario(user) }
        }
        return exitoLocal
    }

    fun autenticarUsuario(correo: String, pass: String): User? = dbHelper.autenticarUsuario(correo, pass)

    fun actualizarPerfil(correo: String, nuevoAlias: String, nuevaPass: String?, nuevoAvatar: ByteArray?): Boolean {
        return dbHelper.actualizarPerfilUsuario(correo, nuevoAlias, nuevaPass, nuevoAvatar)
    }

    fun obtenerCatalogo(): List<ProductoItem> {
        // Sincroniza pendientes y limpia eliminados cada vez que se abre el catálogo
        sincronizarPendientesConServidor()
        return dbHelper.obtenerTodosLosProductos()
    }

    fun obtenerProductoPorSku(sku: String): Product? = dbHelper.obtenerProductoPorSku(sku)

    fun obtenerImagenesDeProducto(sku: String): List<ByteArray> = dbHelper.obtenerImagenesDeProducto(sku)

    // GUARDAR / EDITAR (Si el SKU ya existe, el servidor lo actualiza por ON DUPLICATE KEY)
    fun guardarProducto(product: Product, imagenes: List<ByteArray>): Boolean {
        // Al guardar/editar, lo ponemos en sincronizado = 0 temporalmente hasta que el servidor responda OK
        val prodParaGuardar = product.copy(sincronizado = 0)
        val exitoLocal = dbHelper.guardarOActualizarProducto(prodParaGuardar, imagenes)

        if (exitoLocal) {
            thread {
                val exitoRemoto = ApiClient.sincronizarProducto(prodParaGuardar, imagenes)
                if (exitoRemoto) {
                    dbHelper.marcarProductoSincronizado(product.sku)
                }
            }
        }
        return exitoLocal
    }

    // BORRAR (Elimina localmente y manda la orden DELETE al servidor Fedora)
    fun eliminarProducto(sku: String): Boolean {
        // 1. Borrar localmente en SQLite primero para que la interfaz reaccione al instante
        val exitoLocal = dbHelper.eliminarProducto(sku)

        // 2. Mandar la orden DELETE al servidor Fedora en segundo plano
        if (exitoLocal) {
            thread {
                val exitoRemoto = ApiClient.eliminarProductoRemoto(sku)
                if (!exitoRemoto) {
                    android.util.Log.e("StorageRepository", "El servidor no pudo borrar el SKU: $sku")
                }
            }
        }
        return exitoLocal
    }

    fun registrarMovimiento(sku: String, correoUsuario: String, tipo: String, cantidad: Int, nuevoStock: Int): Boolean {
        val exitoLocal = dbHelper.registrarMovimiento(sku, correoUsuario, tipo, cantidad, nuevoStock)
        if (exitoLocal) {
            sincronizarPendientesConServidor()
        }
        return exitoLocal
    }

    // SINCRONIZACIÓN BIDIRECCIONAL (Subir pendientes y limpiar borrados del servidor)
// SINCRONIZACIÓN BIDIRECCIONAL (Subir pendientes y limpiar borrados del servidor de forma segura)
    fun sincronizarPendientesConServidor(onComplete: ((Int) -> Unit)? = null) {
        thread {
            var subidos = 0

            // 1. Subir productos creados o editados offline
            val prodPendientes = dbHelper.obtenerProductosNoSincronizados()
            for (prod in prodPendientes) {
                val fotos = dbHelper.obtenerImagenesDeProducto(prod.sku)
                if (ApiClient.sincronizarProducto(prod, fotos)) {
                    dbHelper.marcarProductoSincronizado(prod.sku)
                    subidos++
                }
            }

            // 2. Subir movimientos pendientes
            val movPendientes = dbHelper.obtenerMovimientosNoSincronizados()
            for (mov in movPendientes) {
                if (ApiClient.sincronizarMovimiento(mov)) {
                    dbHelper.marcarMovimientoSincronizado(mov.idMovimiento)
                }
            }

            // 3. Descargar catálogo remoto de forma segura
            val remotos = ApiClient.descargarProductosRemotos()

            // Validamos que la API haya respondido (si no hay internet, remotos estará vacío y no tocamos nada local)
            if (remotos.isNotEmpty()) {
                val skusRemotos = remotos.map { it.first.sku }.toSet()
                val locales = dbHelper.obtenerTodosLosProductos()

                for (local in locales) {
                    // Consultamos el producto completo en SQLite para verificar su estado de sincronización real
                    val prodCompleto = dbHelper.obtenerProductoPorSku(local.sku)

                    // 🛡️ REGLA DE ORO OFFLINE-FIRST:
                    // Si no está en el servidor Y el registro local está sincronizado (1), se borra porque lo eliminaron en la nube.
                    // Si el estado es 0 (creado/editado offline), SE RESPETA y no se toca.
                    if (!skusRemotos.contains(local.sku) && (prodCompleto?.sincronizado == 1)) {
                        dbHelper.eliminarProducto(local.sku)
                    }
                }

                // Guardar o actualizar los productos remotos en SQLite local
                for ((prodRemoto, fotosRemotas) in remotos) {
                    val local = dbHelper.obtenerProductoPorSku(prodRemoto.sku)
                    if (local == null || local.sincronizado == 1) {
                        dbHelper.guardarOActualizarProducto(prodRemoto, fotosRemotas)
                        dbHelper.marcarProductoSincronizado(prodRemoto.sku)
                    }
                }
            }

            onComplete?.invoke(subidos)
        }
    }
}