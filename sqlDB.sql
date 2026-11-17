CREATE DATABASE IF NOT EXISTS storageapp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'storage_user'@'localhost' IDENTIFIED BY 'StoragePass123!';
GRANT ALL PRIVILEGES ON storageapp_db.* TO 'storage_user'@'localhost';
FLUSH PRIVILEGES;

USE storageapp_db;

CREATE TABLE IF NOT EXISTS USUARIO (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    alias VARCHAR(50) NOT NULL,
    correo VARCHAR(150) UNIQUE NOT NULL,
    contrasena VARCHAR(255) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    avatar_blob LONGBLOB
);

CREATE TABLE IF NOT EXISTS PRODUCTO (
    sku VARCHAR(60) PRIMARY KEY NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(100) NOT NULL,
    marca VARCHAR(100) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    existencias INT NOT NULL DEFAULT 0,
    punto_reorden INT NOT NULL DEFAULT 0,
    descripcion TEXT,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_modificacion DATETIME NOT NULL,
    sincronizado INT DEFAULT 1,
    correo_usuario_creador VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS IMAGEN_PRODUCTO (
    id_imagen INT AUTO_INCREMENT PRIMARY KEY,
    sku_producto VARCHAR(60) NOT NULL,
    imagen_blob LONGBLOB NOT NULL,
    descripcion VARCHAR(255),
    fecha_creacion DATETIME NOT NULL,
    fecha_modificacion DATETIME NOT NULL,
    FOREIGN KEY (sku_producto) REFERENCES PRODUCTO(sku) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS MOVIMIENTO_INVENTARIO (
    id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
    sku_producto VARCHAR(60) NOT NULL,
    correo_usuario VARCHAR(150) NOT NULL,
    tipo_movimiento VARCHAR(20) NOT NULL,
    cantidad INT NOT NULL,
    fecha_hora DATETIME NOT NULL,
    sincronizado INT DEFAULT 1
);