const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');

const app = express();
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ limit: '50mb', extended: true }));
app.use(cors());

const pool = mysql.createPool({
    host: 'localhost',
    user: 'storage_user',
    password: 'StoragePass123!',
    database: 'storageapp_db',
    waitForConnections: true,
    connectionLimit: 10
});

// Función para limpiar fechas ISO de Android a formato MySQL (YYYY-MM-DD HH:MM:SS)
function parseFechaMySQL(fechaIso) {
    if (!fechaIso) return new Date().toISOString().slice(0, 19).replace('T', ' ');
    return fechaIso.replace('T', ' ').replace('Z', '').split('.')[0];
}

app.get('/api/ping', (req, res) => {
    res.json({ status: 'OK', message: 'API StorageApp activa en Fedora Linux' });
});

// ==================== PRODUCTOS ====================

app.get('/api/productos', async (req, res) => {
    try {
        const [productos] = await pool.query('SELECT * FROM PRODUCTO');
        for (let prod of productos) {
            const [imgs] = await pool.query(
                'SELECT imagen_blob FROM IMAGEN_PRODUCTO WHERE sku_producto = ?',
                [prod.sku]
            );
            prod.imagenesBase64 = imgs
                .map(row => (row.imagen_blob ? row.imagen_blob.toString('base64') : null))
                .filter(Boolean);
        }
        res.json(productos);
    } catch (err) {
        console.error('Error GET /api/productos:', err);
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/productos/sync', async (req, res) => {
    const conn = await pool.getConnection();
    try {
        await conn.beginTransaction();
        const p = req.body;

        const fechaCreacionSQL = parseFechaMySQL(p.fechaCreacion);
        const fechaModificacionSQL = parseFechaMySQL(p.fechaModificacion);

        await conn.query(
            `INSERT INTO PRODUCTO (sku, nombre, categoria, marca, modelo, existencias, punto_reorden, descripcion, estado, fecha_creacion, fecha_modificacion, sincronizado, correo_usuario_creador)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
             ON DUPLICATE KEY UPDATE
             nombre=VALUES(nombre), categoria=VALUES(categoria), marca=VALUES(marca), modelo=VALUES(modelo),
             existencias=VALUES(existencias), punto_reorden=VALUES(punto_reorden), descripcion=VALUES(descripcion),
             estado=VALUES(estado), fecha_modificacion=VALUES(fecha_modificacion), sincronizado=1`,
            [p.sku, p.nombre, p.categoria, p.marca, p.modelo, p.existencias, p.puntoReorden, p.descripcion, p.estado, fechaCreacionSQL, fechaModificacionSQL, p.correoUsuarioCreador]
        );

        if (Array.isArray(p.imagenesBase64) && p.imagenesBase64.length > 0) {
            await conn.query('DELETE FROM IMAGEN_PRODUCTO WHERE sku_producto = ?', [p.sku]);
            for (let i = 0; i < p.imagenesBase64.length; i++) {
                const buffer = Buffer.from(p.imagenesBase64[i], 'base64');
                await conn.query(
                    `INSERT INTO IMAGEN_PRODUCTO (sku_producto, imagen_blob, descripcion, fecha_creacion, fecha_modificacion)
                     VALUES (?, ?, ?, ?, ?)`,
                    [p.sku, buffer, `Imagen ${i + 1}`, fechaCreacionSQL, fechaModificacionSQL]
                );
            }
        }

        await conn.commit();
        res.json({ success: true, sku: p.sku });
    } catch (err) {
        await conn.rollback();
        console.error('Error POST /api/productos/sync:', err);
        res.status(500).json({ success: false, error: err.message });
    } finally {
        conn.release();
    }
});

app.delete('/api/productos/:sku', async (req, res) => {
    try {
        const sku = req.params.sku;
        await pool.query('DELETE FROM IMAGEN_PRODUCTO WHERE sku_producto = ?', [sku]);
        const [result] = await pool.query('DELETE FROM PRODUCTO WHERE sku = ?', [sku]);
        res.json({ success: true, affectedRows: result.affectedRows });
    } catch (err) {
        console.error('Error DELETE /api/productos:', err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// ==================== MOVIMIENTOS ====================

app.post('/api/movimientos/sync', async (req, res) => {
    const m = req.body;
    try {
        await pool.query(
            `INSERT INTO MOVIMIENTO_INVENTARIO (sku_producto, correo_usuario, tipo_movimiento, cantidad, fecha_hora, sincronizado)
             VALUES (?, ?, ?, ?, ?, 1)`,
            [m.skuProducto, m.correoUsuario, m.tipoMovimiento, m.cantidad, parseFechaMySQL(m.fechaHora)]
        );
        res.json({ success: true });
    } catch (err) {
        console.error('Error POST /api/movimientos/sync:', err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// ==================== USUARIOS ====================

// 1. Verificar si el correo ya existe
app.get('/api/usuarios/existe', async (req, res) => {
    try {
        const correo = (req.query.correo || '').trim().toLowerCase();
        console.log('Verificando existencia de correo:', correo);
        const [rows] = await pool.query('SELECT 1 FROM USUARIO WHERE correo = ? LIMIT 1', [correo]);
        res.status(200).json({ existe: rows.length > 0 });
    } catch (error) {
        console.error('Error GET /api/usuarios/existe:', error);
        res.status(500).json({ error: error.message });
    }
});

// 2. Registrar o actualizar perfil de usuario (ÚNICA RUTA, ANTES DE LISTEN)
app.post('/api/usuarios/sync', async (req, res) => {
    try {
        const { nombre, apellidos, alias, correo, contrasenaHash, telefono, direccion, avatarBase64 } = req.body;
        const correoLimpio = (correo || '').trim().toLowerCase();
        console.log('Sincronizando usuario:', correoLimpio, '| Tiene avatar:', Boolean(avatarBase64));

        const avatarBuffer = avatarBase64 ? Buffer.from(avatarBase64, 'base64') : null;
        const [existentes] = await pool.query('SELECT id_usuario FROM USUARIO WHERE correo = ?', [correoLimpio]);

        const sql = `
            INSERT INTO USUARIO (nombre, apellidos, alias, correo, contrasena, telefono, direccion, avatar_blob)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                alias = VALUES(alias),
                contrasena = VALUES(contrasena),
                telefono = VALUES(telefono),
                direccion = VALUES(direccion),
                avatar_blob = COALESCE(VALUES(avatar_blob), avatar_blob)
        `;

        await pool.query(sql, [
            nombre,
            apellidos,
            alias,
            correoLimpio,
            contrasenaHash,
            telefono || null,
            direccion || null,
            avatarBuffer
        ]);

        res.status(existentes.length === 0 ? 201 : 200).json({ success: true, ok: true });
    } catch (error) {
        console.error('Error POST /api/usuarios/sync:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// 3. Login remoto
app.post('/api/usuarios/login', async (req, res) => {
    try {
        const correo = (req.body.correo || '').trim().toLowerCase();
        const contrasena = req.body.contrasena || '';
        const [rows] = await pool.query(
            'SELECT id_usuario, nombre, apellidos, alias, correo, contrasena, telefono, direccion, avatar_blob FROM USUARIO WHERE correo = ? AND contrasena = ? LIMIT 1',
            [correo, contrasena]
        );
        if (rows.length === 0) {
            return res.status(401).json({ ok: false, mensaje: 'Credenciales incorrectas' });
        }
        const u = rows[0];
        res.status(200).json({
            ok: true,
            usuario: {
                id_usuario: u.id_usuario,
                nombre: u.nombre,
                apellidos: u.apellidos,
                alias: u.alias,
                correo: u.correo,
                contrasenaHash: u.contrasena,
                telefono: u.telefono || '',
                direccion: u.direccion || '',
                avatarBase64: u.avatar_blob ? u.avatar_blob.toString('base64') : null
            }
        });
    } catch (error) {
        console.error('Error POST /api/usuarios/login:', error);
        res.status(500).json({ ok: false, error: error.message });
    }
});

const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Servidor API StorageApp escuchando en el puerto ${PORT}`);
});