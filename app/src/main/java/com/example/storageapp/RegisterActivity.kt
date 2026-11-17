package com.example.storageapp

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storageapp.data.StorageDbHelper
import com.example.storageapp.model.User
import com.example.storageapp.network.ApiClient
import com.example.storageapp.network.EmailCheckResult
import com.example.storageapp.network.RemoteUserResult
import com.example.storageapp.utils.ImageUtils
import com.example.storageapp.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private var avatarBytes: ByteArray? = null
    private lateinit var ivAvatarPreview: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = ImageUtils.uriToByteArray(this, uri, maxSize = 400)
            if (bytes != null) {
                avatarBytes = bytes
                ivAvatarPreview.setImageBitmap(ImageUtils.byteArrayToBitmap(bytes))
                Toast.makeText(this, "Avatar cargado (${bytes.size / 1024} KB)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        ivAvatarPreview = findViewById(R.id.ivAvatarPreview)
        val btnSeleccionarAvatar = findViewById<Button>(R.id.btnSeleccionarAvatar)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etAlias = findViewById<EditText>(R.id.etAlias)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etDireccion = findViewById<EditText>(R.id.etDireccion)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarCuenta)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        btnSeleccionarAvatar.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnVolver.setOnClickListener { finish() }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val alias = etAlias.text.toString().trim()
            val correo = etCorreo.text.toString().trim().lowercase(Locale.getDefault())
            val pass = etPassword.text.toString().trim()
            val tel = etTelefono.text.toString().trim()
            val dir = etDireccion.text.toString().trim()

            var valido = true
            if (nombre.isEmpty()) { etNombre.error = "Requerido"; valido = false }
            if (apellidos.isEmpty()) { etApellidos.error = "Requerido"; valido = false }
            if (alias.isEmpty()) { etAlias.error = "Requerido"; valido = false }
            if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                etCorreo.error = "Correo inválido"; valido = false
            }
            val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,}$".toRegex()
            if (!pass.matches(passwordPattern)) {
                etPassword.error = "Mín. 10 caracteres, 1 mayúscula, 1 minúscula y 1 número"; valido = false
            }
            if (avatarBytes == null) {
                Toast.makeText(this, "Selecciona un avatar para continuar", Toast.LENGTH_SHORT).show()
                valido = false
            }
            if (!valido) return@setOnClickListener

            val nuevoUsuario = User(
                nombre = nombre,
                apellidos = apellidos,
                alias = alias,
                correo = correo,
                contrasenaHash = pass,
                telefono = tel.ifEmpty { null },
                direccion = dir.ifEmpty { null },
                avatarBlob = avatarBytes
            )

            btnRegistrar.isEnabled = false
            btnRegistrar.text = "Sincronizando con servidor..."

            lifecycleScope.launch {
                val db = StorageDbHelper.getInstance(this@RegisterActivity)
                val mensaje = withContext(Dispatchers.IO) {

                    when (val check = ApiClient.verificarCorreoDisponible(correo)) {
                        is EmailCheckResult.Exists -> "REMOTE_EXISTS"
                        is EmailCheckResult.ServerError -> "CHECK_ERR_${check.code}"
                        is EmailCheckResult.NetworkError -> {
                            db.registrarUsuario(nuevoUsuario)
                            "OFFLINE_SAVED"
                        }
                        is EmailCheckResult.Available -> {
                            when (val sync = ApiClient.sincronizarUsuario(nuevoUsuario)) {
                                is RemoteUserResult.Success -> {
                                    db.registrarUsuario(nuevoUsuario)
                                    "ONLINE_OK"
                                }
                                is RemoteUserResult.Conflict -> "REMOTE_EXISTS"
                                is RemoteUserResult.ServerError -> "SYNC_ERR_${sync.code}"
                                is RemoteUserResult.NetworkError -> {
                                    db.registrarUsuario(nuevoUsuario)
                                    "OFFLINE_SAVED"
                                }
                            }
                        }
                    }
                }

                btnRegistrar.isEnabled = true
                btnRegistrar.text = "Crear Cuenta"

                when {
                    mensaje == "ONLINE_OK" -> {
                        SessionManager.guardarSesion(this@RegisterActivity, correo, alias, "$nombre $apellidos", avatarBytes)
                        Toast.makeText(this@RegisterActivity, "¡Cuenta creada y guardada en MySQL!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    mensaje == "OFFLINE_SAVED" -> {
                        Toast.makeText(this@RegisterActivity, "Sin red: guardado en SQLite local (pendiente)", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    mensaje == "LOCAL_EXISTS" || mensaje == "REMOTE_EXISTS" -> {
                        etCorreo.error = "El correo ya está registrado"
                        Toast.makeText(this@RegisterActivity, "Ese correo ya existe en la base de datos", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        // Muestra el código exacto si el servidor devolvió 404, 413 o 500
                        Toast.makeText(this@RegisterActivity, "Error del servidor ($mensaje)", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}