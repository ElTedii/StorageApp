package com.example.storageapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SaveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save)

        val etCodigo = findViewById<EditText>(R.id.etCodigo)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCantidad = findViewById<EditText>(R.id.etCantidad)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnVolver = findViewById<Button>(R.id.btnVolverSave)

        btnGuardar.setOnClickListener {
            val codigo = etCodigo.text.toString()
            val nombre = etNombre.text.toString()
            val cantidad = etCantidad.text.toString()

            if (codigo.isNotEmpty() && nombre.isNotEmpty() && cantidad.isNotEmpty()) {
                // Lógica de procesamiento/guardado aquí
                Toast.makeText(this, "Datos capturados correctamente", Toast.LENGTH_SHORT).show()
                finish() // Cierra la Activity actual y regresa a MainActivity
            } else {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}