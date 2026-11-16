package com.example.storageapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.storageapp.data.StorageDbHelper
import com.example.storageapp.model.Product
import com.example.storageapp.utils.ImageUtils
import com.example.storageapp.utils.SessionManager

class SaveActivity : AppCompatActivity() {

    private var img1Bytes: ByteArray? = null
    private var img2Bytes: ByteArray? = null
    private var seleccionandoImagenNumero = 1
    private var fechaCreacionOriginal: String? = null

    private lateinit var ivImg1: ImageView
    private lateinit var ivImg2: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = ImageUtils.uriToByteArray(this, uri, maxSize = 600)
            if (bytes != null) {
                val bitmap = ImageUtils.byteArrayToBitmap(bytes)
                if (seleccionandoImagenNumero == 1) {
                    img1Bytes = bytes
                    ivImg1.setImageBitmap(bitmap)
                } else {
                    img2Bytes = bytes
                    ivImg2.setImageBitmap(bitmap)
                }
                Toast.makeText(this, "Imagen $seleccionandoImagenNumero lista (${bytes.size / 1024} KB)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save)

        val etSKU = findViewById<EditText>(R.id.etSKU)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCategoria = findViewById<EditText>(R.id.etCategoria)
        val etMarca = findViewById<EditText>(R.id.etMarca)
        val etModelo = findViewById<EditText>(R.id.etModelo)
        val etUnidades = findViewById<EditText>(R.id.etUnidades)
        val etPuntoReorden = findViewById<EditText>(R.id.etPuntoReorden)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)

        ivImg1 = findViewById(R.id.ivImg1)
        ivImg2 = findViewById(R.id.ivImg2)

        val btnGuardarBorrador = findViewById<Button>(R.id.btnGuardarBorrador)
        val btnPublicar = findViewById<Button>(R.id.btnPublicar)
        val btnVolver = findViewById<Button>(R.id.btnVolverSave)

        val db = StorageDbHelper.getInstance(this)

        // Si venimos desde "Editar Producto", precargamos sus datos y fotos
        intent.getStringExtra("EDITAR_SKU")?.let { skuEditar ->
            db.obtenerProductoPorSku(skuEditar)?.let { prod ->
                etSKU.setText(prod.sku)
                etSKU.isEnabled = false // El SKU es Primary Key y no se cambia al editar
                etNombre.setText(prod.nombre)
                etCategoria.setText(prod.categoria)
                etMarca.setText(prod.marca)
                etModelo.setText(prod.modelo)
                etUnidades.setText(prod.existencias.toString())
                etPuntoReorden.setText(prod.puntoReorden.toString())
                etDescripcion.setText(prod.descripcion ?: "")
                fechaCreacionOriginal = prod.fechaCreacion

                val fotos = db.obtenerImagenesDeProducto(skuEditar)
                if (fotos.isNotEmpty()) {
                    img1Bytes = fotos[0]
                    ivImg1.setImageBitmap(ImageUtils.byteArrayToBitmap(img1Bytes))
                }
                if (fotos.size > 1) {
                    img2Bytes = fotos[1]
                    ivImg2.setImageBitmap(ImageUtils.byteArrayToBitmap(img2Bytes))
                }
            }
        }

        ivImg1.setOnClickListener {
            seleccionandoImagenNumero = 1
            pickImageLauncher.launch("image/*")
        }

        ivImg2.setOnClickListener {
            seleccionandoImagenNumero = 2
            pickImageLauncher.launch("image/*")
        }

        fun guardarEnBaseDeDatos(estado: String) {
            val ahora = StorageDbHelper.obtenerFechaActual()
            val producto = Product(
                sku = etSKU.text.toString().trim(),
                nombre = etNombre.text.toString().trim(),
                categoria = etCategoria.text.toString().trim().ifEmpty { "General" },
                marca = etMarca.text.toString().trim().ifEmpty { "Genérica" },
                modelo = etModelo.text.toString().trim().ifEmpty { "S/M" },
                existencias = etUnidades.text.toString().toIntOrNull() ?: 0,
                puntoReorden = etPuntoReorden.text.toString().toIntOrNull() ?: 0,
                descripcion = etDescripcion.text.toString().trim(),
                estado = estado,
                fechaCreacion = fechaCreacionOriginal ?: ahora,
                fechaModificacion = ahora,
                sincronizado = 0,
                correoUsuarioCreador = SessionManager.obtenerCorreo(this)
            )

            val listaFotos = listOfNotNull(img1Bytes, img2Bytes)
            val exito = db.guardarOActualizarProducto(producto, listaFotos)
            if (exito) {
                Toast.makeText(this, "Producto guardado en SQLite como $estado", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show()
            }
        }

        btnGuardarBorrador.setOnClickListener {
            if (etSKU.text.isEmpty() || etNombre.text.isEmpty()) {
                Toast.makeText(this, "Para guardar un borrador ingresa al menos SKU y Nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            guardarEnBaseDeDatos("BORRADOR")
        }

        btnPublicar.setOnClickListener {
            if (etSKU.text.isEmpty() || etNombre.text.isEmpty() || etCategoria.text.isEmpty() ||
                etMarca.text.isEmpty() || etModelo.text.isEmpty() || etUnidades.text.isEmpty() || etPuntoReorden.text.isEmpty()
            ) {
                AlertDialog.Builder(this)
                    .setTitle("Campos Incompletos")
                    .setMessage("Por favor completa todos los datos obligatorios del producto.")
                    .setPositiveButton("Aceptar", null)
                    .show()
                return@setOnClickListener
            }

            if (img1Bytes == null || img2Bytes == null) {
                AlertDialog.Builder(this)
                    .setTitle("Faltan Imágenes")
                    .setMessage("Todo producto publicado requiere mínimo 2 imágenes descriptivas. Toca los recuadros de cámara para seleccionarlas.")
                    .setPositiveButton("Entendido", null)
                    .show()
                return@setOnClickListener
            }

            guardarEnBaseDeDatos("PUBLICADO")
        }

        btnVolver.setOnClickListener { finish() }
    }
}