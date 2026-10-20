package com.example.storageapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {

    private var stockActual = 15
    private var puntoReorden = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val tvNombre = findViewById<TextView>(R.id.tvNombreProductoDetalle)
        val tvSKU = findViewById<TextView>(R.id.tvSKUDetalle)
        val tvStock = findViewById<TextView>(R.id.tvStockActual)
        val tvReorden = findViewById<TextView>(R.id.tvPuntoReordenDetalle)
        val etCantidad = findViewById<EditText>(R.id.etCantidadMovimiento)
        val btnEntrada = findViewById<Button>(R.id.btnEntrada)
        val btnSalida = findViewById<Button>(R.id.btnSalida)
        val btnVolver = findViewById<Button>(R.id.btnVolverDetalle)

        // Recuperar datos enviados desde ViewActivity
        intent.extras?.let {
            val nombre = it.getString("NOMBRE", "Taladro Percutor 20V")
            val sku = it.getString("SKU", "SK-TL-0921")
            stockActual = it.getInt("STOCK", 15)
            puntoReorden = it.getInt("PUNTO_REORDEN", 5)

            tvNombre.text = nombre
            tvSKU.text = "SKU: $sku"
            tvReorden.text = "Punto de Reorden: $puntoReorden unidades"
            actualizarTextoStock(tvStock)
        }

        // Registrar ENTRADA (+)
        btnEntrada.setOnClickListener {
            val inputCant = etCantidad.text.toString()
            if (inputCant.isNotEmpty()) {
                val cantidad = inputCant.toInt()
                if (cantidad > 0) {
                    stockActual += cantidad
                    actualizarTextoStock(tvStock)
                    etCantidad.text.clear()
                    Toast.makeText(this, "Entrada de $cantidad unidades registrada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Ingresa una cantidad mayor a 0", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor especifica la cantidad", Toast.LENGTH_SHORT).show()
            }
        }

        // Registrar SALIDA (-)
        btnSalida.setOnClickListener {
            val inputCant = etCantidad.text.toString()
            if (inputCant.isNotEmpty()) {
                val cantidad = inputCant.toInt()
                if (cantidad <= 0) {
                    Toast.makeText(this, "Ingresa una cantidad mayor a 0", Toast.LENGTH_SHORT).show()
                } else if (cantidad > stockActual) {
                    Toast.makeText(this, "Error: No hay suficientes unidades disponibles ($stockActual)", Toast.LENGTH_LONG).show()
                } else {
                    stockActual -= cantidad
                    actualizarTextoStock(tvStock)
                    etCantidad.text.clear()
                    Toast.makeText(this, "Salida de $cantidad unidades registrada", Toast.LENGTH_SHORT).show()

                    if (stockActual <= puntoReorden) {
                        Toast.makeText(
                            this,
                            "⚠️ ALERTA: El producto ha alcanzado o superado el punto de reorden ($puntoReorden pzas)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Por favor especifica la cantidad", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }

    private fun actualizarTextoStock(tvStock: TextView) {
        tvStock.text = "Stock Disponible: $stockActual unidades"
    }
}