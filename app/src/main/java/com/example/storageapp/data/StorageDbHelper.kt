package com.example.storageapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.storageapp.ProductoItem
import com.example.storageapp.model.InventoryMovement
import com.example.storageapp.model.Product
import com.example.storageapp.model.ProductImage
import com.example.storageapp.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StorageDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "StorageApp.db"
        private const val DATABASE_VERSION = 1

        // PATRÓN SINGLETON: Garantiza una única conexión activa a SQLite en toda la app
        @Volatile
        private var instance: StorageDbHelper? = null

        fun getInstance(context: Context): StorageDbHelper {
            return instance ?: synchronized(this) {
                instance ?: StorageDbHelper(context).also { instance = it }
            }
        }

        fun obtenerFechaActual(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. TABLA USUARIO
        db.execSQL("""
            CREATE TABLE USUARIO (
                id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre VARCHAR NOT NULL,
                apellidos VARCHAR NOT NULL,
                alias VARCHAR NOT NULL,
                correo VARCHAR UNIQUE NOT NULL,
                contrasena VARCHAR NOT NULL,
                telefono VARCHAR,
                direccion VARCHAR,
                avatar_blob BLOB
            )
        """.trimIndent())

        // 2. TABLA PRODUCTO (SKU como Clave Primaria)
        db.execSQL("""
            CREATE TABLE PRODUCTO (
                sku VARCHAR PRIMARY KEY NOT NULL,
                nombre VARCHAR NOT NULL,
                categoria VARCHAR NOT NULL,
                marca VARCHAR NOT NULL,
                modelo VARCHAR NOT NULL,
                existencias INTEGER NOT NULL DEFAULT 0,
                punto_reorden INTEGER NOT NULL DEFAULT 0,
                descripcion TEXT,
                estado VARCHAR NOT NULL,
                fecha_creacion DATETIME NOT NULL,
                fecha_modificacion DATETIME NOT NULL,
                sincronizado INTEGER DEFAULT 0,
                correo_usuario_creador VARCHAR,
                FOREIGN KEY (correo_usuario_creador) REFERENCES USUARIO(correo)
            )
        """.trimIndent())

        // 3. TABLA IMAGEN_PRODUCTO
        db.execSQL("""
            CREATE TABLE IMAGEN_PRODUCTO (
                id_imagen INTEGER PRIMARY KEY AUTOINCREMENT,
                sku_producto VARCHAR NOT NULL,
                imagen_blob BLOB NOT NULL,
                descripcion VARCHAR,
                fecha_creacion DATETIME NOT NULL,
                fecha_modificacion DATETIME NOT NULL,
                FOREIGN KEY (sku_producto) REFERENCES PRODUCTO(sku) ON DELETE CASCADE
            )
        """.trimIndent())

        // 4. TABLA MOVIMIENTO_INVENTARIO
        db.execSQL("""
            CREATE TABLE MOVIMIENTO_INVENTARIO (
                id_movimiento INTEGER PRIMARY KEY AUTOINCREMENT,
                sku_producto VARCHAR NOT NULL,
                correo_usuario VARCHAR NOT NULL,
                tipo_movimiento VARCHAR NOT NULL,
                cantidad INTEGER NOT NULL,
                fecha_hora DATETIME NOT NULL,
                sincronizado INTEGER DEFAULT 0,
                FOREIGN KEY (sku_producto) REFERENCES PRODUCTO(sku),
                FOREIGN KEY (correo_usuario) REFERENCES USUARIO(correo)
            )
        """.trimIndent())

        insertarDatosSemilla(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS MOVIMIENTO_INVENTARIO")
        db.execSQL("DROP TABLE IF EXISTS IMAGEN_PRODUCTO")
        db.execSQL("DROP TABLE IF EXISTS PRODUCTO")
        db.execSQL("DROP TABLE IF EXISTS USUARIO")
        onCreate(db)
    }

    private fun insertarDatosSemilla(db: SQLiteDatabase) {
        val ahora = obtenerFechaActual()
        // Usuario por defecto para pruebas rápidas
        val userValues = ContentValues().apply {
            put("nombre", "Administrador")
            put("apellidos", "Almacén")
            put("alias", "Admin")
            put("correo", "admin@storageapp.com")
            put("contrasena", "Admin12345")
        }
        db.insert("USUARIO", null, userValues)

        // Productos iniciales de demostración
        val iniciales = listOf(
            Product("SK-TL-0921", "Taladro Percutor 20V", "Herramientas", "DeWalt", "DCD778", 15, 5, "Taladro inalámbrico brushless", "PUBLICADO", ahora, ahora, 0, "admin@storageapp.com"),
            Product("SK-LL-4410", "Juego de Llaves Allen", "Herramientas", "Stanley", "ST-69-253", 3, 5, "Juego de 10 llaves milimétricas", "PUBLICADO", ahora, ahora, 0, "admin@storageapp.com"),
            Product("SK-MT-1102", "Multímetro Digital Pro", "Electricidad", "Truper", "MUT-105", 8, 2, "Medidor de voltaje y continuidad", "PUBLICADO", ahora, ahora, 0, "admin@storageapp.com")
        )
        for (p in iniciales) {
            val cv = ContentValues().apply {
                put("sku", p.sku)
                put("nombre", p.nombre)
                put("categoria", p.categoria)
                put("marca", p.marca)
                put("modelo", p.modelo)
                put("existencias", p.existencias)
                put("punto_reorden", p.puntoReorden)
                put("descripcion", p.descripcion)
                put("estado", p.estado)
                put("fecha_creacion", p.fechaCreacion)
                put("fecha_modificacion", p.fechaModificacion)
                put("sincronizado", p.sincronizado)
                put("correo_usuario_creador", p.correoUsuarioCreador)
            }
            db.insert("PRODUCTO", null, cv)
        }
    }

    // ==================== OPERACIONES DE USUARIO ====================

    fun registrarUsuario(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nombre", user.nombre)
            put("apellidos", user.apellidos)
            put("alias", user.alias)
            put("correo", user.correo)
            put("contrasena", user.contrasenaHash)
            put("telefono", user.telefono)
            put("direccion", user.direccion)
            put("avatar_blob", user.avatarBlob)
        }
        val res = db.insert("USUARIO", null, values)
        return res != -1L
    }

    fun autenticarUsuario(correo: String, pass: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM USUARIO WHERE correo = ? AND contrasena = ?",
            arrayOf(correo, pass)
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                alias = cursor.getString(cursor.getColumnIndexOrThrow("alias")),
                correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                contrasenaHash = cursor.getString(cursor.getColumnIndexOrThrow("contrasena")),
                telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono")),
                direccion = cursor.getString(cursor.getColumnIndexOrThrow("direccion")),
                avatarBlob = cursor.getBlob(cursor.getColumnIndexOrThrow("avatar_blob"))
            )
        }
        cursor.close()
        return user
    }

    fun actualizarPerfilUsuario(correo: String, nuevoAlias: String, nuevaPass: String?, nuevoAvatar: ByteArray?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("alias", nuevoAlias)
            if (!nuevaPass.isNullOrEmpty()) put("contrasena", nuevaPass)
            if (nuevoAvatar != null) put("avatar_blob", nuevoAvatar)
        }
        val filas = db.update("USUARIO", values, "correo = ?", arrayOf(correo))
        return filas > 0
    }

    // ==================== OPERACIONES DE PRODUCTOS E IMÁGENES ====================

    fun guardarOActualizarProducto(product: Product, imagenes: List<ByteArray>): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put("sku", product.sku)
                put("nombre", product.nombre)
                put("categoria", product.categoria)
                put("marca", product.marca)
                put("modelo", product.modelo)
                put("existencias", product.existencias)
                put("punto_reorden", product.puntoReorden)
                put("descripcion", product.descripcion)
                put("estado", product.estado)
                put("fecha_creacion", product.fechaCreacion)
                put("fecha_modificacion", product.fechaModificacion)
                put("sincronizado", 0)
                put("correo_usuario_creador", product.correoUsuarioCreador)
            }

            db.insertWithOnConflict("PRODUCTO", null, values, SQLiteDatabase.CONFLICT_REPLACE)

            if (imagenes.isNotEmpty()) {
                db.delete("IMAGEN_PRODUCTO", "sku_producto = ?", arrayOf(product.sku))
                val ahora = obtenerFechaActual()
                imagenes.forEachIndexed { index, bytes ->
                    val imgValues = ContentValues().apply {
                        put("sku_producto", product.sku)
                        put("imagen_blob", bytes)
                        put("descripcion", "Imagen ${index + 1} de ${product.nombre}")
                        put("fecha_creacion", ahora)
                        put("fecha_modificacion", ahora)
                    }
                    db.insert("IMAGEN_PRODUCTO", null, imgValues)
                }
            }

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
        }
    }

    fun obtenerTodosLosProductos(): List<ProductoItem> {
        val lista = mutableListOf<ProductoItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM PRODUCTO", null)

        if (cursor.moveToFirst()) {
            do {
                val sku = cursor.getString(cursor.getColumnIndexOrThrow("sku"))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                val categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria"))
                val marca = cursor.getString(cursor.getColumnIndexOrThrow("marca"))
                val existencias = cursor.getInt(cursor.getColumnIndexOrThrow("existencias"))
                val puntoReorden = cursor.getInt(cursor.getColumnIndexOrThrow("punto_reorden"))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))

                val miniatura = obtenerImagenesDeProducto(sku).firstOrNull()
                val nombreMostrar = if (estado == "BORRADOR") "[BORRADOR] $nombre" else nombre

                lista.add(
                    ProductoItem(
                        idProducto = sku.hashCode(),
                        sku = sku,
                        nombre = nombreMostrar,
                        categoria = categoria,
                        marca = marca,
                        existencias = existencias,
                        puntoReorden = puntoReorden,
                        imagenPrincipal = miniatura
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun obtenerProductoPorSku(sku: String): Product? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM PRODUCTO WHERE sku = ?", arrayOf(sku))
        var prod: Product? = null
        if (cursor.moveToFirst()) {
            prod = Product(
                sku = cursor.getString(cursor.getColumnIndexOrThrow("sku")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria")),
                marca = cursor.getString(cursor.getColumnIndexOrThrow("marca")),
                modelo = cursor.getString(cursor.getColumnIndexOrThrow("modelo")),
                existencias = cursor.getInt(cursor.getColumnIndexOrThrow("existencias")),
                puntoReorden = cursor.getInt(cursor.getColumnIndexOrThrow("punto_reorden")),
                descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
                fechaCreacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_creacion")),
                fechaModificacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_modificacion")),
                sincronizado = cursor.getInt(cursor.getColumnIndexOrThrow("sincronizado")),
                correoUsuarioCreador = cursor.getString(cursor.getColumnIndexOrThrow("correo_usuario_creador"))
            )
        }
        cursor.close()
        return prod
    }

    fun obtenerImagenesDeProducto(sku: String): List<ByteArray> {
        val fotos = mutableListOf<ByteArray>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT imagen_blob FROM IMAGEN_PRODUCTO WHERE sku_producto = ?", arrayOf(sku))
        if (cursor.moveToFirst()) {
            do {
                val blob = cursor.getBlob(0)
                if (blob != null) fotos.add(blob)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return fotos
    }

    fun eliminarProducto(sku: String): Boolean {
        val db = writableDatabase
        db.delete("IMAGEN_PRODUCTO", "sku_producto = ?", arrayOf(sku))
        db.delete("MOVIMIENTO_INVENTARIO", "sku_producto = ?", arrayOf(sku))
        return db.delete("PRODUCTO", "sku = ?", arrayOf(sku)) > 0
    }

    // ==================== OPERACIONES DE INVENTARIO (ENTRADAS / SALIDAS) ====================

    fun registrarMovimiento(sku: String, correoUsuario: String, tipo: String, cantidad: Int, nuevoStock: Int): Boolean {
        val db = writableDatabase
        val ahora = obtenerFechaActual()
        db.beginTransaction()
        return try {
            val prodValues = ContentValues().apply {
                put("existencias", nuevoStock)
                put("fecha_modificacion", ahora)
                put("sincronizado", 0)
            }
            db.update("PRODUCTO", prodValues, "sku = ?", arrayOf(sku))

            val movValues = ContentValues().apply {
                put("sku_producto", sku)
                put("correo_usuario", correoUsuario)
                put("tipo_movimiento", tipo)
                put("cantidad", cantidad)
                put("fecha_hora", ahora)
                put("sincronizado", 0)
            }
            db.insert("MOVIMIENTO_INVENTARIO", null, movValues)

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
        }
    }
}