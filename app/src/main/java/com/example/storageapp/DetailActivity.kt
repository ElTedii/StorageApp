package com.example.storageapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.storageapp.data.StorageDbHelper
import com.example.storageapp.utils.ImageUtils
import com.example.storageapp.utils.SessionManager

class DetailActivity : AppCompatActivity() {

    private var stockActual = 0
    private var puntoReorden = 0
    private var nombreProducto = ""
    private var skuProducto = ""

    private lateinit var tvNombre: TextView
    private lateinit var tvSKU: TextView
    private lateinit var tvCatMarca: TextView
    private lateinit var tvStock: TextView
    private lateinit var tvReorden: TextView
    private lateinit var ivImg1: ImageView
    private lateinit var ivImg2: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        crearCanalNotificaciones()
        solicitarPermisoNotificaciones()

        tvNombre = findViewById(R.id.tvNombreProductoDetalle)
        tvSKU = findViewById(R.id.tvSKUDetalle)
        tvCatMarca = findViewById(R.id.tvCategoriaMarcaDetalle)
        tvStock = findViewById(R.id.tvStockActual)
        tvReorden = findViewById(R.id.tvPuntoReordenDetalle)
        ivImg1 = findViewById(R.id.ivDetalleImg1)
        ivImg2 = findViewById(R.id.ivDetalleImg2)

        val etCantidad = findViewById<EditText>(R.id.etCantidadMovimiento)
        val btnEntrada = findViewById<Button>(R.id.btnEntrada)
        val btnSalida = findViewById<Button>(R.id.btnSalida)
        val btnVolver = findViewById<Button>(R.id.btnVolverDetalle)

        skuProducto = intent.getStringExtra("SKU") ?: "SK-TL-0921"
        cargarDatosDesdeSQLite()

        // Al tocar el título del producto o las fotos, ofrece Editar o Eliminar el producto (CRUD completo)
        tvNombre.setOnClickListener { mostrarOpcionesCRUD() }

        val db = StorageDbHelper.getInstance(this)
        val correoUsuario = SessionManager.obtenerCorreo(this)

        // Registrar ENTRADA (+)
        btnEntrada.setOnClickListener {
            val cantidad = etCantidad.text.toString().toIntOrNull()
            if (cantidad == null || cantidad <= 0) {
                mostrarDialogo("Cantidad Inválida", "Por favor ingresa un número entero mayor a 0.")
                return@setOnClickListener
            }

            stockActual += cantidad
            db.registrarMovimiento(skuProducto, correoUsuario, "ENTRADA", cantidad, stockActual)
            actualizarTextoStock()
            etCantidad.text.clear()
            mostrarDialogo("Entrada Registrada", "Se agregaron $cantidad unidades en SQLite por $correoUsuario.")
        }

        // Registrar SALIDA (-)
        btnSalida.setOnClickListener {
            val cantidad = etCantidad.text.toString().toIntOrNull()
            if (cantidad == null || cantidad <= 0) {
                mostrarDialogo("Cantidad Inválida", "Por favor ingresa un número entero mayor a 0.")
                return@setOnClickListener
            }

            if (cantidad > stockActual) {
                mostrarDialogo(
                    "Error de Existencias",
                    "No puedes retirar $cantidad unidades. Solo hay $stockActual disponibles en almacén."
                )
                return@setOnClickListener
            }

            stockActual -= cantidad
            db.registrarMovimiento(skuProducto, correoUsuario, "SALIDA", cantidad, stockActual)
            actualizarTextoStock()
            etCantidad.text.clear()

            if (stockActual <= puntoReorden) {
                lanzarNotificacionReorden()
                mostrarDialogo(
                    "⚠️ Alerta de Punto de Reorden",
                    "Salida guardada. ATENCIÓN: '$nombreProducto' quedó con $stockActual unidades (Punto de reorden: $puntoReorden)."
                )
            } else {
                mostrarDialogo("Salida Registrada", "Se retiraron $cantidad unidades correctamente.")
            }
        }

        btnVolver.setOnClickListener { finish() }
        btnVolver.setOnLongClickListener {
            mostrarOpcionesCRUD()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatosDesdeSQLite()
    }

    private fun cargarDatosDesdeSQLite() {
        val db = StorageDbHelper.getInstance(this)
        db.obtenerProductoPorSku(skuProducto)?.let { prod ->
            nombreProducto = prod.nombre
            stockActual = prod.existencias
            puntoReorden = prod.puntoReorden

            tvNombre.text = "${prod.nombre} ✏️"
            tvSKU.text = "SKU: ${prod.sku} | Modelo: ${prod.modelo} (${prod.estado})"
            tvCatMarca.text = "Categoría: ${prod.categoria} | Marca: ${prod.marca}\nDescripción: ${prod.descripcion ?: "Sin descripción"}"
            tvReorden.text = "Punto de Reorden: $puntoReorden unidades"
            actualizarTextoStock()

            val fotos = db.obtenerImagenesDeProducto(skuProducto)
            if (fotos.isNotEmpty()) {
                ivImg1.setImageBitmap(ImageUtils.byteArrayToBitmap(fotos[0]))
            }
            if (fotos.size > 1) {
                ivImg2.setImageBitmap(ImageUtils.byteArrayToBitmap(fotos[1]))
            }
        }
    }

    private fun mostrarOpcionesCRUD() {
        val opciones = arrayOf("✏️ Editar Producto / Publicar Borrador", "🗑️ Eliminar Producto del Almacén", "Cancelar")
        AlertDialog.Builder(this)
            .setTitle(" Administrar: $nombreProducto")
            .setItems(opciones) { dialog, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, SaveActivity::class.java)
                        intent.putExtra("EDITAR_SKU", skuProducto)
                        startActivity(intent)
                    }
                    1 -> {
                        AlertDialog.Builder(this)
                            .setTitle("Confirmar Eliminación")
                            .setMessage("¿Estás seguro de eliminar permanentemente '$nombreProducto' ($skuProducto)?")
                            .setPositiveButton("Eliminar") { _, _ ->
                                StorageDbHelper.getInstance(this).eliminarProducto(skuProducto)
                                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun actualizarTextoStock() {
        tvStock.text = "Stock Disponible: $stockActual unidades"
    }

    private fun mostrarDialogo(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "REORDEN_CHANNEL",
                "Alertas de Inventario",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando un producto alcanza su punto de reorden"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun lanzarNotificacionReorden() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val builder = NotificationCompat.Builder(this, "REORDEN_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Stock Bajo: $nombreProducto")
            .setContentText("Quedan $stockActual pzas (SKU: $skuProducto). Requiere reabastecimiento.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(skuProducto.hashCode(), builder.build())
    }
}