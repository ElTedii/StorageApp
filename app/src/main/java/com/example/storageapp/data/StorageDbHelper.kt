package com.example.storageapp.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
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
        private const val DATABASE_VERSION = 3

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
                avatar_blob BLOB,
                estado_registro VARCHAR NOT NULL DEFAULT 'CONFIRMADO',
                sincronizado INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())

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

        db.execSQL("""
            CREATE TABLE IMAGEN_PRODUCTO (
                id_imagen INTEGER PRIMARY KEY AUTOINCREMENT,
                sku_producto VARCHAR NOT NULL,
                imagen_blob BLOB NOT NULL,
                descripcion VARCHAR NOT NULL DEFAULT 'Imagen de producto',
                fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (sku_producto) REFERENCES PRODUCTO(sku) ON DELETE CASCADE
            )
        """.trimIndent())

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
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE USUARIO ADD COLUMN estado_registro VARCHAR NOT NULL DEFAULT 'CONFIRMADO'") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE USUARIO ADD COLUMN sincronizado INTEGER NOT NULL DEFAULT 1") } catch (_: Exception) {}
            db.execSQL("UPDATE USUARIO SET estado_registro = 'CONFIRMADO', sincronizado = 1 WHERE estado_registro IS NULL OR sincronizado IS NULL")
        }
        if (oldVersion < 3) {
            val cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'IMAGEN_PRODUCTO'",
                null
            )
            val existeTabla = cursor.moveToFirst()
            cursor.close()
            if (existeTabla) {
                try { db.execSQL("ALTER TABLE IMAGEN_PRODUCTO ADD COLUMN descripcion VARCHAR NOT NULL DEFAULT 'Imagen de producto'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE IMAGEN_PRODUCTO ADD COLUMN fecha_creacion DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE IMAGEN_PRODUCTO ADD COLUMN fecha_modificacion DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00'") } catch (_: Exception) {}
                val ahora = obtenerFechaActual()
                db.execSQL("UPDATE IMAGEN_PRODUCTO SET descripcion = 'Imagen de producto' WHERE descripcion IS NULL OR TRIM(descripcion) = ''")
                db.execSQL("UPDATE IMAGEN_PRODUCTO SET fecha_creacion = ? WHERE fecha_creacion IS NULL OR TRIM(fecha_creacion) = ''", arrayOf(ahora))
                db.execSQL("UPDATE IMAGEN_PRODUCTO SET fecha_modificacion = ? WHERE fecha_modificacion IS NULL OR TRIM(fecha_modificacion) = ''", arrayOf(ahora))
            }
        }
    }

    private fun insertarDatosSemilla(db: SQLiteDatabase) {
        val ahora = obtenerFechaActual()
        val userValues = ContentValues().apply {
            put("nombre", "Administrador")
            put("apellidos", "Almacén")
            put("alias", "Admin")
            put("correo", "admin@storageapp.com")
            put("contrasena", "Admin12345")
            put("estado_registro", "CONFIRMADO")
            put("sincronizado", 1)
        }
        db.insert("USUARIO", null, userValues)

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

    private fun cursorToUser(cursor: Cursor): User {
        val estadoIdx = cursor.getColumnIndex("estado_registro")
        val syncIdx = cursor.getColumnIndex("sincronizado")
        return User(
            idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario")),
            nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
            apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
            alias = cursor.getString(cursor.getColumnIndexOrThrow("alias")),
            correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
            contrasenaHash = cursor.getString(cursor.getColumnIndexOrThrow("contrasena")),
            telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono")),
            direccion = cursor.getString(cursor.getColumnIndexOrThrow("direccion")),
            avatarBlob = cursor.getBlob(cursor.getColumnIndexOrThrow("avatar_blob")),
            estadoRegistro = if (estadoIdx >= 0) cursor.getString(estadoIdx) ?: "CONFIRMADO" else "CONFIRMADO",
            sincronizado = if (syncIdx >= 0) cursor.getInt(syncIdx) else 1
        )
    }

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
            put("estado_registro", user.estadoRegistro)
            put("sincronizado", user.sincronizado)
        }
        return db.insert("USUARIO", null, values) != -1L
    }

    fun existeCorreoLocal(correo: String): Boolean {
        val cursor = readableDatabase.rawQuery("SELECT 1 FROM USUARIO WHERE correo = ?", arrayOf(correo))
        val existe = cursor.moveToFirst()
        cursor.close()
        return existe
    }

    fun obtenerUsuarioPorCorreo(correo: String): User? {
        val cursor = readableDatabase.rawQuery("SELECT * FROM USUARIO WHERE correo = ?", arrayOf(correo))
        var user: User? = null
        if (cursor.moveToFirst()) user = cursorToUser(cursor)
        cursor.close()
        return user
    }

    fun obtenerUsuariosPendientes(): List<User> {
        val lista = mutableListOf<User>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM USUARIO WHERE sincronizado = 0", null)
        if (cursor.moveToFirst()) {
            do { lista.add(cursorToUser(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun marcarUsuarioSincronizado(correo: String) {
        val cv = ContentValues().apply {
            put("sincronizado", 1)
            put("estado_registro", "CONFIRMADO")
        }
        writableDatabase.update("USUARIO", cv, "correo = ?", arrayOf(correo))
    }

    fun marcarUsuarioConflicto(correo: String) {
        val cv = ContentValues().apply {
            put("sincronizado", 0)
            put("estado_registro", "CONFLICTO")
        }
        writableDatabase.update("USUARIO", cv, "correo = ?", arrayOf(correo))
    }

    fun marcarUsuarioPendienteSync(correo: String) {
        val actual = obtenerUsuarioPorCorreo(correo)
        val cv = ContentValues().apply {
            put("sincronizado", 0)
            if (actual?.estadoRegistro == "CONFIRMADO") {
                put("estado_registro", "CONFIRMADO")
            }
        }
        writableDatabase.update("USUARIO", cv, "correo = ?", arrayOf(correo))
    }

    fun insertarProductoOffline(product: Product, imagenes: List<ByteArray>): Boolean {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM PRODUCTO WHERE sku = ?", arrayOf(product.sku))
        val existe = cursor.moveToFirst()
        cursor.close()
        if (existe) return false
        return guardarOActualizarProducto(product.copy(sincronizado = 0), imagenes)
    }

    fun autenticarUsuario(correo: String, pass: String): User? {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM USUARIO WHERE correo = ? AND contrasena = ?",
            arrayOf(correo, pass)
        )
        var user: User? = null
        if (cursor.moveToFirst()) user = cursorToUser(cursor)
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
        return db.update("USUARIO", values, "correo = ?", arrayOf(correo)) > 0
    }

    fun guardarOActualizarProducto(product: Product, imagenes: List<ByteArray>): Boolean {
        val db = writableDatabase
        val imagenesExistentes = obtenerImagenesProducto(product.sku)
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
                    val anterior = imagenesExistentes.getOrNull(index)
                    val conservarMetadatos = anterior != null && anterior.imagenBlob.contentEquals(bytes)
                    val descripcion = if (conservarMetadatos) {
                        anterior!!.descripcion.ifBlank { "Imagen ${index + 1} de ${product.nombre}" }
                    } else {
                        "Imagen ${index + 1} de ${product.nombre}"
                    }
                    val fechaCreacion = if (conservarMetadatos) {
                        anterior!!.fechaCreacion.ifBlank { ahora }
                    } else {
                        ahora
                    }
                    val imgValues = ContentValues().apply {
                        put("sku_producto", product.sku)
                        put("imagen_blob", bytes)
                        put("descripcion", descripcion)
                        put("fecha_creacion", fechaCreacion)
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
        val cursor = readableDatabase.rawQuery("SELECT * FROM PRODUCTO", null)
        if (cursor.moveToFirst()) {
            do {
                val sku = cursor.getString(cursor.getColumnIndexOrThrow("sku"))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                val categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria"))
                val marca = cursor.getString(cursor.getColumnIndexOrThrow("marca"))
                val existencias = cursor.getInt(cursor.getColumnIndexOrThrow("existencias"))
                val puntoReorden = cursor.getInt(cursor.getColumnIndexOrThrow("punto_reorden"))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
                val fechaCreacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_creacion")) ?: ""
                val fechaModificacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_modificacion")) ?: ""
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
                        imagenPrincipal = miniatura,
                        descripcion = descripcion,
                        fechaCreacion = fechaCreacion,
                        fechaModificacion = fechaModificacion
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun obtenerProductoPorSku(sku: String): Product? {
        val cursor = readableDatabase.rawQuery("SELECT * FROM PRODUCTO WHERE sku = ?", arrayOf(sku))
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

    fun obtenerImagenesProducto(sku: String): List<ProductImage> {
        val imagenes = mutableListOf<ProductImage>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id_imagen, sku_producto, imagen_blob, descripcion, fecha_creacion, fecha_modificacion " +
                "FROM IMAGEN_PRODUCTO WHERE sku_producto = ? ORDER BY id_imagen",
            arrayOf(sku)
        )
        if (cursor.moveToFirst()) {
            do {
                imagenes.add(
                    ProductImage(
                        idImagen = cursor.getInt(cursor.getColumnIndexOrThrow("id_imagen")),
                        skuProducto = cursor.getString(cursor.getColumnIndexOrThrow("sku_producto")),
                        imagenBlob = cursor.getBlob(cursor.getColumnIndexOrThrow("imagen_blob")) ?: ByteArray(0),
                        descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")) ?: "Imagen de producto",
                        fechaCreacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_creacion")) ?: "",
                        fechaModificacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_modificacion")) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return imagenes
    }

    fun obtenerImagenesDeProducto(sku: String): List<ByteArray> =
        obtenerImagenesProducto(sku).map { it.imagenBlob }

    fun eliminarProducto(sku: String): Boolean {
        val db = writableDatabase
        db.delete("IMAGEN_PRODUCTO", "sku_producto = ?", arrayOf(sku))
        db.delete("MOVIMIENTO_INVENTARIO", "sku_producto = ?", arrayOf(sku))
        return db.delete("PRODUCTO", "sku = ?", arrayOf(sku)) > 0
    }

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

    fun obtenerProductosNoSincronizados(): List<Product> {
        val lista = mutableListOf<Product>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM PRODUCTO WHERE sincronizado = 0", null)
        if (cursor.moveToFirst()) {
            do {
                lista.add(Product(
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
                    sincronizado = 0,
                    correoUsuarioCreador = cursor.getString(cursor.getColumnIndexOrThrow("correo_usuario_creador"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun marcarProductoSincronizado(sku: String) {
        val cv = ContentValues().apply { put("sincronizado", 1) }
        writableDatabase.update("PRODUCTO", cv, "sku = ?", arrayOf(sku))
    }

    fun obtenerMovimientosNoSincronizados(): List<InventoryMovement> {
        val lista = mutableListOf<InventoryMovement>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM MOVIMIENTO_INVENTARIO WHERE sincronizado = 0", null)
        if (cursor.moveToFirst()) {
            do {
                lista.add(InventoryMovement(
                    idMovimiento = cursor.getInt(cursor.getColumnIndexOrThrow("id_movimiento")),
                    skuProducto = cursor.getString(cursor.getColumnIndexOrThrow("sku_producto")),
                    correoUsuario = cursor.getString(cursor.getColumnIndexOrThrow("correo_usuario")),
                    tipoMovimiento = cursor.getString(cursor.getColumnIndexOrThrow("tipo_movimiento")),
                    cantidad = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad")),
                    fechaHora = cursor.getString(cursor.getColumnIndexOrThrow("fecha_hora")),
                    sincronizado = 0
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun marcarMovimientoSincronizado(idMovimiento: Int) {
        val cv = ContentValues().apply { put("sincronizado", 1) }
        writableDatabase.update("MOVIMIENTO_INVENTARIO", cv, "id_movimiento = ?", arrayOf(idMovimiento.toString()))
    }

    fun marcarProductoComoPendiente(sku: String) {
        val cv = ContentValues().apply { put("sincronizado", 0) }
        writableDatabase.update("PRODUCTO", cv, "sku = ?", arrayOf(sku))
    }
}
