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

        val etSKU = findViewById<EditText>(R.id.etSKU)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etUnidades = findViewById<EditText>(R.id.etUnidades)
        val etPuntoReorden = findViewById<EditText>(R.id.etPuntoReorden)

        val btnGuardarBorrador = findViewById<Button>(R.id.btnGuardarBorrador)
        val btnPublicar = findViewById<Button>(R.id.btnPublicar)
        val btnVolver = findViewById<Button>(R.id.btnVolverSave)

        btnGuardarBorrador.setOnClickListener {
            Toast.makeText(this, "Guardado localmente como borrador", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnPublicar.setOnClickListener {
            if (etSKU.text.isNotEmpty() && etNombre.text.isNotEmpty() && etUnidades.text.isNotEmpty()) {
                Toast.makeText(this, "Producto publicado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}