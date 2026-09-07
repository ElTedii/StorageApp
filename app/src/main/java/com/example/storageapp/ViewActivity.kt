package com.example.storageapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        val tvResultado = findViewById<TextView>(R.id.tvResultado)
        val btnVolver = findViewById<Button>(R.id.btnVolverConsulta)

        // Aquí se consumirá la fuente de datos local o la API
        tvResultado.text = "Mostrando registros activos del almacén..."

        btnVolver.setOnClickListener {
            finish()
        }
    }
}