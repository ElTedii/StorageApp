package com.example.storageapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ViewActivity : AppCompatActivity() {

    private lateinit var adapter: ProductoAdapter
    private var listaProductos = mutableListOf<ProductoItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        val rvProductos = findViewById<RecyclerView>(R.id.rvProductos)
        val btnVolver = findViewById<Button>(R.id.btnVolverConsulta)

        // Lista simulada inicial (Mockup)
        listaProductos = mutableListOf(
            ProductoItem(1, "SK-TL-0921", "Taladro Percutor 20V", "Herramientas", "DeWalt", 15, 5),
            ProductoItem(2, "SK-LL-4410", "Juego de Llaves Allen", "Herramientas", "Stanley", 3, 5), // Dispara alerta
            ProductoItem(3, "SK-MT-1102", "Multímetro Digital Pro", "Electricidad", "Truper", 8, 2)
        )

        adapter = ProductoAdapter(listaProductos) { producto ->
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("PRODUCTO_ID", producto.idProducto)
                putExtra("SKU", producto.sku)
                putExtra("NOMBRE", producto.nombre)
                putExtra("STOCK", producto.existencias)
                putExtra("PUNTO_REORDEN", producto.puntoReorden)
            }
            startActivity(intent)
        }

        rvProductos.layoutManager = LinearLayoutManager(this)
        rvProductos.adapter = adapter

        // Filtrado dinámico en tiempo real
        etBuscar.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val listaFiltrada = listaProductos.filter {
                it.nombre.lowercase().contains(query) ||
                        it.sku.lowercase().contains(query) ||
                        it.categoria.lowercase().contains(query)
            }
            adapter.actualizarLista(listaFiltrada)
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}