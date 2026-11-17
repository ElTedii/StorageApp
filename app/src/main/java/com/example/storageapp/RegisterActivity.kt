package com.example.storageapp

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storageapp.model.User
import com.example.storageapp.repository.RegisterResult
import com.example.storageapp.repository.StorageRepository
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
        val pbRegister = findViewById<ProgressBar>(R.id.pbRegister)

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
            btnRegistrar.text = "Verificando..."
            pbRegister.visibility = View.VISIBLE
            val repo = StorageRepository(this)

            lifecycleScope.launch {
                val resultado = try {
                    withContext(Dispatchers.IO) {
                        repo.registrarUsuarioConValidacionOnline(nuevoUsuario)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RegisterActivity", "Error al registrar usuario", e)
                    RegisterResult.LocalError
                } finally {
                    btnRegistrar.isEnabled = true
                    btnRegistrar.text = "Crear Cuenta"
                    pbRegister.visibility = View.GONE
                }
                when (resultado) {
                    RegisterResult.SuccessOnline -> {
                        SessionManager.guardarSesion(this@RegisterActivity, correo, alias, "$nombre $apellidos", avatarBytes)
                        Toast.makeText(this@RegisterActivity, "Cuenta creada y sincronizada en línea", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    RegisterResult.SavedOfflinePending -> {
                        Toast.makeText(this@RegisterActivity, "Sin conexión: registro guardado como borrador pendiente. Se verificará y subirá al conectarse.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    RegisterResult.EmailAlreadyExists, RegisterResult.EmailAlreadyExistsLocal -> {
                        etCorreo.error = "El correo ya está registrado"
                        Toast.makeText(this@RegisterActivity, "El correo ya está registrado (local o en servidor)", Toast.LENGTH_LONG).show()
                    }
                    RegisterResult.LocalError -> {
                        Toast.makeText(this@RegisterActivity, "Error al guardar localmente", Toast.LENGTH_LONG).show()
                    }
                    RegisterResult.ServerError -> {
                        Toast.makeText(this@RegisterActivity, "Error del servidor al validar el registro. Intenta más tarde.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
