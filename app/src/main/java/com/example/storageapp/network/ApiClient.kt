package com.example.storageapp.network

import android.util.Base64
import com.example.storageapp.model.InventoryMovement
import com.example.storageapp.model.Product
import com.example.storageapp.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// PATRÓN SINGLETON: Cliente HTTP centralizado para comunicación con el servidor Fedora Linux
object ApiClient {

    private const val BASE_URL = "https://fedorasv-tedss.tailc9606a.ts.net/api"

    private fun postJson(endpoint: String, jsonBody: JSONObject): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$BASE_URL$endpoint")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 8000
                readTimeout = 15000
                doOutput = true
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            conn?.disconnect()
        }
    }

    fun eliminarProductoRemoto(sku: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val skuEncoded = java.net.URLEncoder.encode(sku, "UTF-8")
            val url = URL("$BASE_URL/productos/$skuEncoded")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 5000
                readTimeout = 5000
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            conn?.disconnect()
        }
    }

    fun sincronizarUsuario(user: User): Boolean {
        val json = JSONObject().apply {
            put("nombre", user.nombre)
            put("apellidos", user.apellidos)
            put("alias", user.alias)
            put("correo", user.correo)
            put("contrasenaHash", user.contrasenaHash)
            put("telefono", user.telefono ?: "")
            put("direccion", user.direccion ?: "")
            if (user.avatarBlob != null) {
                put("avatarBase64", Base64.encodeToString(user.avatarBlob, Base64.NO_WRAP))
            }
        }
        return postJson("/usuarios/sync", json)
    }

    fun sincronizarProducto(product: Product, imagenes: List<ByteArray>): Boolean {
        val jsonArrayImgs = JSONArray()
        imagenes.forEach { bytes ->
            jsonArrayImgs.put(Base64.encodeToString(bytes, Base64.NO_WRAP))
        }

        val json = JSONObject().apply {
            put("sku", product.sku)
            put("nombre", product.nombre)
            put("categoria", product.categoria)
            put("marca", product.marca)
            put("modelo", product.modelo)
            put("existencias", product.existencias)
            put("puntoReorden", product.puntoReorden)
            put("descripcion", product.descripcion ?: "")
            put("estado", product.estado)
            put("fechaCreacion", product.fechaCreacion)
            put("fechaModificacion", product.fechaModificacion)
            put("correoUsuarioCreador", product.correoUsuarioCreador ?: "admin@storageapp.com")
            put("imagenesBase64", jsonArrayImgs)
        }
        return postJson("/productos/sync", json)
    }

    fun sincronizarMovimiento(mov: InventoryMovement): Boolean {
        val json = JSONObject().apply {
            put("skuProducto", mov.skuProducto)
            put("correoUsuario", mov.correoUsuario)
            put("tipoMovimiento", mov.tipoMovimiento)
            put("cantidad", mov.cantidad)
            put("fechaHora", mov.fechaHora)
        }
        return postJson("/movimientos/sync", json)
    }

    fun descargarProductosRemotos(): List<Pair<Product, List<ByteArray>>> {
        val resultado = mutableListOf<Pair<Product, List<ByteArray>>>()
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/productos")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 15000
            }
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val prod = Product(
                        sku = obj.getString("sku"),
                        nombre = obj.getString("nombre"),
                        categoria = obj.getString("categoria"),
                        marca = obj.getString("marca"),
                        modelo = obj.getString("modelo"),
                        existencias = obj.getInt("existencias"),
                        puntoReorden = obj.getInt("punto_reorden"),
                        descripcion = obj.optString("descripcion", ""),
                        estado = obj.getString("estado"),
                        fechaCreacion = obj.getString("fecha_creacion"),
                        fechaModificacion = obj.getString("fecha_modificacion"),
                        sincronizado = 1,
                        correoUsuarioCreador = obj.optString("correo_usuario_creador", "admin@storageapp.com")
                    )

                    val fotos = mutableListOf<ByteArray>()
                    val imgsArray = obj.optJSONArray("imagenesBase64")
                    if (imgsArray != null) {
                        for (j in 0 until imgsArray.length()) {
                            val b64 = imgsArray.getString(j)
                            fotos.add(Base64.decode(b64, Base64.DEFAULT))
                        }
                    }
                    resultado.add(Pair(prod, fotos))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.disconnect()
        }
        return resultado
    }
}