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

---

## Verificación y pendientes

> **Compilación verificada:** `.\gradlew.bat :app:compileDebugKotlin` terminó con `BUILD SUCCESSFUL` el 16/11/2026, usando `local.properties` con `sdk.dir=C:/Users/sejei/AppData/Local/Android/Sdk`.
> `local.properties` está en `.gitignore`; cada entorno debe configurar su propia ruta del SDK.

### 1. Compilación
1. Abrir el proyecto en Android Studio o crear `local.properties` con `sdk.dir=<ruta del Android SDK>`.
2. Ejecutar `Sync Gradle`.
3. Ejecutar `Build > Make Project` o `.\gradlew.bat :app:compileDebugKotlin`.

### 2. Riesgos de compilación ya cubiertos
- Dependencias `kotlinx-coroutines-android` y `lifecycle-runtime-ktx` resueltas.
- Sintaxis Kotlin (`data object`, corrutinas, `lifecycleScope`) compilando con el AGP actual.
- Recursos/XML válidos (`activity_view.xml`, `activity_profile.xml`, `activity_register.xml`, `activity_login.xml`).
- `ProductoItem`, `User` (`estadoRegistro`, `sincronizado`) y sus usos compilando.
- `StorageViewModel.actualizarPerfil` corregido para recibir `currentPassword` y devolver `ProfileUpdateResult`.

### 3. Probar en emulador/dispositivo
- [ ] Registro: correo único (online y local), contraseña (mín. 10, 1 mayúscula, 1 minúscula, 1 número), alias y avatar obligatorio; teléfono/dirección opcionales.
- [ ] Registro sin conexión: se guarda como borrador pendiente y se sincroniza al reconectar.
- [ ] Perfil: editar alias, cambiar avatar y cambiar contraseña solicitando la contraseña actual; cambios offline pendientes de sync.
- [ ] Login: bloqueo de cuentas `PENDIENTE`/`CONFLICTO` y acceso normal a `CONFIRMADO`.
- [ ] Indicador de carga: `ProgressBar` visible en login, registro, perfil y catálogo mientras se consulta/sincroniza con el servidor; se oculta al terminar o fallar.
- [ ] Catálogo: búsqueda por título, descripción, SKU y categoría.
- [ ] Catálogo: orden por nombre, stock (menor/mayor), SKU, fecha de creación (reciente/antigua) y fecha de modificación (reciente/antigua).
- [ ] Imágenes: `IMAGEN_PRODUCTO` guarda `imagen_blob`, `descripcion`, `fecha_creacion` y `fecha_modificacion` no nulos; al editar sin cambiar una imagen se conserva su `fecha_creacion`.
- [ ] Migración SQLite v3: columnas de `USUARIO` y `IMAGEN_PRODUCTO` sin pérdida de datos existentes.
- [ ] CRUD de productos, movimientos de entrada/salida y alerta de punto de reorden.

### 4. Desplegar/verificar el endpoint Kotlin
- El archivo `server/UsuariosEndpoint.kt` **no forma parte del build de Gradle**; debe desplegarse aparte en el servidor Fedora/MySQL.
- Verificar `GET /api/usuarios/existe` y `POST /api/usuarios/sync` (upsert por correo) con la tabla `USUARIO` y `correo UNIQUE`.
- Confirmar que la app apunta a la URL correcta en `ApiClient.BASE_URL`.