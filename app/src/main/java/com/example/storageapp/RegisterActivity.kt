package com.example.storageapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.storageapp.data.StorageDbHelper
import com.example.storageapp.model.User
import com.example.storageapp.utils.ImageUtils
import com.example.storageapp.utils.SessionManager

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

        btnSeleccionarAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val alias = etAlias.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val tel = etTelefono.text.toString().trim()
            val dir = etDireccion.text.toString().trim()

            if (nombre.isEmpty() || apellidos.isEmpty() || alias.isEmpty() || correo.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos obligatorios (*)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,}$".toRegex()
            if (!pass.matches(passwordPattern)) {
                Toast.makeText(
                    this,
                    "La contraseña requiere mín. 10 caracteres, 1 mayúscula, 1 minúscula y 1 número",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

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

            val exito = StorageDbHelper.getInstance(this).registrarUsuario(nuevoUsuario)
            if (exito) {
                SessionManager.guardarSesion(this, correo, alias, "$nombre $apellidos", avatarBytes)
                Toast.makeText(this, "Usuario guardado en SQLite exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error: El correo ya está registrado", Toast.LENGTH_LONG).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }
}