# StorageApp - Sistema de Control e Inventario de Almacén

Aplicación móvil nativa para Android desarrollada en Kotlin que permite gestionar el flujo de inventario, registro de productos, consulta con filtros y control de entradas/salidas de almacén.

---

## Características Principales

* **Autenticación y Perfil:**
  * Inicio de sesión con validación de credenciales.
  * Registro de nuevos usuarios con reglas estrictas de contraseña (mínimo 10 caracteres, 1 mayúscula, 1 minúscula y 1 número) y personalización de avatar.
  * Edición de perfil para actualización de avatar, alias y datos personales.

* **Gestión de Inventario (CRUD):**
  * Alta de productos con código SKU, categoría, marca, punto de reorden y múltiples imágenes de galería.
  * Guardado parcial como borrador y publicación definitiva.
  * Catálogo de consulta con búsqueda dinámica por texto, filtrado por categoría/marca y reordenamiento.

* **Control de Stock y Alertas:**
  * Módulo de Entradas (+) y Salidas (-) de almacén.
  * Validación automática para evitar salidas superiores al stock disponible.
  * Notificación/Alerta al alcanzar el **Punto de Reorden** programado.

---

## Pantallas del Sistema (`Activities`)

1. **`LoginActivity`**: Pantalla inicial de acceso al sistema.
2. **`RegisterActivity`**: Formulario de registro de usuario con validaciones de seguridad.
3. **`MainActivity`**: Dashboard principal de navegación hacia los módulos del sistema.
4. **`ProfileActivity`**: Módulo de consulta y edición de la cuenta del usuario.
5. **`SaveActivity`**: Formulario de captura y registro de nuevos productos o edición de existentes.
6. **`ViewActivity`**: Catálogo general con barra de búsqueda, ordenamiento y filtros por categoría.
7. **`DetailActivity`**: Vista a detalle del producto, galería de fotos y registro de movimientos (Entradas/Salidas).

---

## Tecnologías y Herramientas Utilizadas

* **Lenguaje:** Kotlin
* **IDE:** Android Studio
* **Diseño UI:** XML Layouts
* **SDK Mínimo:** Android 8.0 
* **SDK Objetivo:** Android 14