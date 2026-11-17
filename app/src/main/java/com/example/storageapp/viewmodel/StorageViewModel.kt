package com.example.storageapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.storageapp.ProductoItem
import com.example.storageapp.model.Product
import com.example.storageapp.model.User
import com.example.storageapp.repository.ProfileUpdateResult
import com.example.storageapp.repository.StorageRepository
import java.util.concurrent.atomic.AtomicInteger

/**
 * PATRÓN MVVM (Model - View - ViewModel):
 * Separa la lógica de negocio de la interfaz gráfica (Activities) y mantiene
 * el estado de los datos ante rotaciones de pantalla mediante LiveData.
 */
class StorageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StorageRepository(application)

    private val _productosLiveData = MutableLiveData<List<ProductoItem>>()
    val productosLiveData: LiveData<List<ProductoItem>> get() = _productosLiveData

    private val _cargandoLiveData = MutableLiveData(false)
    val cargandoLiveData: LiveData<Boolean> get() = _cargandoLiveData

    private val syncsActivos = AtomicInteger(0)

    private val _productoDetalleLiveData = MutableLiveData<Product?>()
    val productoDetalleLiveData: LiveData<Product?> get() = _productoDetalleLiveData

    private val _imagenesDetalleLiveData = MutableLiveData<List<ByteArray>>()
    val imagenesDetalleLiveData: LiveData<List<ByteArray>> get() = _imagenesDetalleLiveData

    // --- Catálogo Reactivo ---
    fun cargarCatalogo() {
        syncsActivos.incrementAndGet()
        _cargandoLiveData.value = true
        _productosLiveData.value = repository.obtenerCatalogoLocal()
        repository.sincronizarPendientesConServidor {
            val restantes = syncsActivos.decrementAndGet()
            _productosLiveData.postValue(repository.obtenerCatalogoLocal())
            if (restantes <= 0) _cargandoLiveData.postValue(false)
        }
    }

    // --- Detalle Reactivo ---
    fun cargarDetalleProducto(sku: String) {
        _productoDetalleLiveData.value = repository.obtenerProductoPorSku(sku)
        _imagenesDetalleLiveData.value = repository.obtenerImagenesDeProducto(sku)
    }

    // --- Acciones CRUD de Producto ---
    fun guardarProducto(product: Product, imagenes: List<ByteArray>): Boolean {
        val exito = repository.guardarProducto(product, imagenes)
        if (exito) cargarCatalogo()
        return exito
    }

    fun eliminarProducto(sku: String): Boolean {
        val exito = repository.eliminarProducto(sku)
        if (exito) cargarCatalogo()
        return exito
    }

    fun registrarMovimiento(sku: String, correoUsuario: String, tipo: String, cantidad: Int, nuevoStock: Int): Boolean {
        val exito = repository.registrarMovimiento(sku, correoUsuario, tipo, cantidad, nuevoStock)
        if (exito) cargarDetalleProducto(sku)
        return exito
    }

    // --- Acciones de Usuario ---
    fun registrarUsuario(user: User): Boolean = repository.registrarUsuario(user)

    fun autenticarUsuario(correo: String, pass: String): User? = repository.autenticarUsuario(correo, pass)

    fun actualizarPerfil(correo: String, currentPassword: String, nuevoAlias: String, nuevaPass: String?, nuevoAvatar: ByteArray?): ProfileUpdateResult {
        return repository.actualizarPerfil(correo, currentPassword, nuevoAlias, nuevaPass, nuevoAvatar)
    }
}