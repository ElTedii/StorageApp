package com.example.storageapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.storageapp.data.StorageDbHelper
import com.example.storageapp.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etCorreo = findViewById<EditText>(R.id.etCorreoLogin)
        val etPassword = findViewById<EditText>(R.id.etPasswordLogin)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnIrRegistro = findViewById<Button>(R.id.btnIrRegistro)

        btnIngresar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (correo.isNotEmpty() && password.isNotEmpty()) {
                val usuario = StorageDbHelper.getInstance(this).autenticarUsuario(correo, password)
                if (usuario != null) {
                    SessionManager.guardarSesion(
                        this,
                        usuario.correo,
                        usuario.alias,
                        "${usuario.nombre} ${usuario.apellidos}",
                        usuario.avatarBlob
                    )
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Credenciales incorrectas (Tip: usa admin@storageapp.com / Admin12345 o regístrate)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(this, "Por favor ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}