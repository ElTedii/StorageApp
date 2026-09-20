package com.example.storageapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {

    // Variable simulada de stock actual en existencia
    private var stockActual = 15
    private val puntoReorden = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val tvStock = findViewById<TextView>(R.id.tvStockActual)
        val etCantidad = findViewById<EditText>(R.id.etCantidadMovimiento)
        val btnEntrada = findViewById<Button>(R.id.btnEntrada)
        val btnSalida = findViewById<Button>(R.id.btnSalida)
        val btnVolver = findViewById<Button>(R.id.btnVolverDetalle)

        // Registrar ENTRADA de inventario
        btnEntrada.setOnClickListener {
            val inputCant = etCantidad.text.toString()

            if (inputCant.isNotEmpty()) {
                val cantidad = inputCant.toInt()
                if (cantidad > 0) {
                    stockActual += cantidad
                    tvStock.text = "Stock Disponible: $stockActual unidades"
                    etCantidad.text.clear()
                    Toast.makeText(this, "Entrada de $cantidad unidades registrada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Ingresa una cantidad mayor a 0", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor especifica la cantidad", Toast.LENGTH_SHORT).show()
            }
        }

        // Registrar SALIDA de inventario (Validando existencias y Notificación de Reorden)
        btnSalida.setOnClickListener {
            val inputCant = etCantidad.text.toString()

            if (inputCant.isNotEmpty()) {
                val cantidad = inputCant.toInt()

                if (cantidad <= 0) {
                    Toast.makeText(this, "Ingresa una cantidad mayor a 0", Toast.LENGTH_SHORT).show()
                } else if (cantidad > stockActual) {
                    // VALIDACIÓN OBLIGATORIA POR RÚBRICA: No permitir más salidas que existencias
                    Toast.makeText(this, "Error: No hay suficientes unidades disponibles ($stockActual)", Toast.LENGTH_LONG).show()
                } else {
                    stockActual -= cantidad
                    tvStock.text = "Stock Disponible: $stockActual unidades"
                    etCantidad.text.clear()
                    Toast.makeText(this, "Salida de $cantidad unidades registrada", Toast.LENGTH_SHORT).show()

                    // ALERTAS Y NOTIFICACIONES DE REORDEN
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

        btnVolver.setOnClickListener {
            finish()
        }
    }
}