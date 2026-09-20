package com.example.storageapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Referencias a los componentes del XML
        val etCorreo = findViewById<EditText>(R.id.etCorreoLogin)
        val etPassword = findViewById<EditText>(R.id.etPasswordLogin)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnIrRegistro = findViewById<Button>(R.id.btnIrRegistro)

        // Acción al presionar el botón Ingresar
        btnIngresar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (correo.isNotEmpty() && password.isNotEmpty()) {
                // Validación exitosa: navega a MainActivity y elimina Login de la pila
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Por favor ingresa tu correo y contraseña",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Acción al presionar el botón de Registro
        btnIrRegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}