package com.example.storageapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.storageapp.utils.ImageUtils
import com.example.storageapp.utils.SessionManager
import com.example.storageapp.data.StorageDbHelper

class ProfileActivity : AppCompatActivity() {

    private var nuevoAvatarBytes: ByteArray? = null
    private lateinit var ivPerfilAvatar: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = ImageUtils.uriToByteArray(this, uri, maxSize = 400)
            if (bytes != null) {
                nuevoAvatarBytes = bytes
                ivPerfilAvatar.setImageBitmap(ImageUtils.byteArrayToBitmap(bytes))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        ivPerfilAvatar = findViewById(R.id.ivPerfilAvatar)
        val btnCambiarAvatar = findViewById<Button>(R.id.btnCambiarAvatar)
        val etEditAlias = findViewById<EditText>(R.id.etEditAlias)
        val etEditPassword = findViewById<EditText>(R.id.etEditPassword)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfil)
        val btnVolver = findViewById<Button>(R.id.btnVolverPerfil)

        // Cargar datos actuales
        etEditAlias.setText(SessionManager.obtenerAlias(this))
        ImageUtils.byteArrayToBitmap(SessionManager.obtenerAvatar(this))?.let {
            ivPerfilAvatar.setImageBitmap(it)
        }

        btnCambiarAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            val alias = etEditAlias.text.toString().trim()
            val nuevaPass = etEditPassword.text.toString().trim()

            if (alias.isEmpty()) {
                Toast.makeText(this, "El alias no puede quedar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nuevaPass.isNotEmpty()) {
                val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,}$".toRegex()
                if (!nuevaPass.matches(passwordPattern)) {
                    Toast.makeText(
                        this,
                        "La nueva contraseña requiere mín. 10 caracteres, 1 mayúscula, 1 minúscula y 1 número",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
            }

            val correoActual = SessionManager.obtenerCorreo(this)
            StorageDbHelper.getInstance(this).actualizarPerfilUsuario(
                correoActual,
                alias,
                nuevaPass.ifEmpty { null },
                nuevoAvatarBytes
            )
            SessionManager.actualizarPerfil(this, alias, nuevoAvatarBytes)

            SessionManager.actualizarPerfil(this, alias, nuevoAvatarBytes)
            Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnVolver.setOnClickListener { finish() }
    }
}