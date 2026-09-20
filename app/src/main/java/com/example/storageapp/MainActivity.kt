package com.example.storageapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnIrPerfil = findViewById<Button>(R.id.btnIrPerfil)
        val btnIrCaptura = findViewById<Button>(R.id.btnIrCaptura)
        val btnIrConsulta = findViewById<Button>(R.id.btnIrConsulta)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        btnIrPerfil.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        btnIrCaptura.setOnClickListener {
            val intent = Intent(this, SaveActivity::class.java)
            startActivity(intent)
        }

        btnIrConsulta.setOnClickListener {
            val intent = Intent(this, ViewActivity::class.java)
            startActivity(intent)
        }

        btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            // Limpia el stack de actividades para evitar volver atrás con el botón 'back'
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}