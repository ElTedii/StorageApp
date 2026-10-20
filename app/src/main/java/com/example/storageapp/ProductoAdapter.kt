package com.example.storageapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private var listaProductos: List<ProductoItem>,
    private val onItemClick: (ProductoItem) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImagen: ImageView = view.findViewById(R.id.ivItemProducto)
        val tvNombre: TextView = view.findViewById(R.id.tvItemNombre)
        val tvSKU: TextView = view.findViewById(R.id.tvItemSKU)
        val tvCategoria: TextView = view.findViewById(R.id.tvItemCategoria)
        val tvStock: TextView = view.findViewById(R.id.tvItemStock)
        val tvBadgeReorden: TextView = view.findViewById(R.id.tvItemBadgeReorden)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val prod = listaProductos[position]
        val context = holder.itemView.context

        holder.tvNombre.text = prod.nombre
        holder.tvSKU.text = "SKU: ${prod.sku}"
        holder.tvCategoria.text = "${prod.categoria} | ${prod.marca}"
        holder.tvStock.text = "${prod.existencias} pzas"

        // Alerta dinámica de punto de reorden
        if (prod.existencias <= prod.puntoReorden) {
            holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.stock_alert))
            holder.tvBadgeReorden.visibility = View.VISIBLE
        } else {
            holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.stock_ok))
            holder.tvBadgeReorden.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(prod) }
    }

    override fun getItemCount(): Int = listaProductos.size

    fun actualizarLista(nuevaLista: List<ProductoItem>) {
        listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}