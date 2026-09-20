package com.example.storageapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        val spFiltro = findViewById<Spinner>(R.id.spFiltroCategoria)
        val spOrden = findViewById<Spinner>(R.id.spOrdenamiento)
        val rvProductos = findViewById<RecyclerView>(R.id.rvProductos)
        val btnVolver = findViewById<Button>(R.id.btnVolverConsulta)

        // Configuración básica del RecyclerView
        rvProductos.layoutManager = LinearLayoutManager(this)

        btnVolver.setOnClickListener {
            finish()
        }
    }
}