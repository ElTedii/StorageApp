package com.example.storageapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.storageapp.viewmodel.StorageViewModel

class ViewActivity : AppCompatActivity() {

    private lateinit var viewModel: StorageViewModel
    private lateinit var adapter: ProductoAdapter
    private var listaProductos = mutableListOf<ProductoItem>()

    private lateinit var etBuscar: EditText
    private lateinit var spFiltroCategoria: Spinner
    private lateinit var spOrdenamiento: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        // Inicializar ViewModel (Patrón MVVM)
        viewModel = ViewModelProvider(this)[StorageViewModel::class.java]

        etBuscar = findViewById(R.id.etBuscar)
        spFiltroCategoria = findViewById(R.id.spFiltroCategoria)
        spOrdenamiento = findViewById(R.id.spOrdenamiento)
        val rvProductos = findViewById<RecyclerView>(R.id.rvProductos)
        val btnVolver = findViewById<Button>(R.id.btnVolverConsulta)

        val categorias = arrayOf("Todas las categorías", "Herramientas", "Electricidad", "Plomería", "Seguridad", "General")
        spFiltroCategoria.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categorias)

        val ordenamientos = arrayOf("Ordenar: Nombre (A-Z)", "Ordenar: Menor Stock", "Ordenar: Mayor Stock", "Ordenar: SKU")
        spOrdenamiento.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ordenamientos)

        adapter = ProductoAdapter(listaProductos) { producto ->
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("PRODUCTO_ID", producto.idProducto)
                putExtra("SKU", producto.sku)
                putExtra("NOMBRE", producto.nombre)
                putExtra("CATEGORIA", producto.categoria)
                putExtra("MARCA", producto.marca)
                putExtra("STOCK", producto.existencias)
                putExtra("PUNTO_REORDEN", producto.puntoReorden)
            }
            startActivity(intent)
        }

        rvProductos.layoutManager = LinearLayoutManager(this)
        rvProductos.adapter = adapter

        // Observar cambios reactivos desde el ViewModel (MVVM)
        viewModel.productosLiveData.observe(this) { productos ->
            listaProductos = productos.toMutableList()
            aplicarFiltrosYOrden()
        }

        etBuscar.addTextChangedListener { aplicarFiltrosYOrden() }

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                aplicarFiltrosYOrden()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spFiltroCategoria.onItemSelectedListener = spinnerListener
        spOrdenamiento.onItemSelectedListener = spinnerListener

        btnVolver.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarCatalogo()
    }

    private fun aplicarFiltrosYOrden() {
        val query = etBuscar.text.toString().lowercase().trim()
        val categoriaSeleccionada = spFiltroCategoria.selectedItem?.toString() ?: "Todas las categorías"
        val ordenSeleccionado = spOrdenamiento.selectedItemPosition

        var resultado = listaProductos.filter { prod ->
            val coincideTexto = prod.nombre.lowercase().contains(query) ||
                    prod.sku.lowercase().contains(query) ||
                    prod.categoria.lowercase().contains(query)

            val coincideCategoria = (categoriaSeleccionada == "Todas las categorías") ||
                    prod.categoria.equals(categoriaSeleccionada, ignoreCase = true)

            coincideTexto && coincideCategoria
        }

        resultado = when (ordenSeleccionado) {
            0 -> resultado.sortedBy { it.nombre.lowercase() }
            1 -> resultado.sortedBy { it.existencias }
            2 -> resultado.sortedByDescending { it.existencias }
            3 -> resultado.sortedBy { it.sku }
            else -> resultado
        }

        adapter.actualizarLista(resultado)
    }
}