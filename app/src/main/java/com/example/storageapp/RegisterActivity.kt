package com.example.storageapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etAlias = findViewById<EditText>(R.id.etAlias)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarCuenta)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val alias = etAlias.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (nombre.isEmpty() || apellidos.isEmpty() || alias.isEmpty() || correo.isEmpty()) {
                Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Expresión Regular: Mínimo 10 caracteres, al menos 1 mayúscula, 1 minúscula y 1 número
            val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,}$".toRegex()

            if (!pass.matches(passwordPattern)) {
                Toast.makeText(
                    this,
                    "La contraseña requiere mín. 10 caracteres, 1 mayúscula, 1 minúscula y 1 número",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Si pasa las validaciones:
            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}