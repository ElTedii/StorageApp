package com.example.storageapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.storageapp.repository.ProfileUpdateResult
import com.example.storageapp.repository.StorageRepository
import com.example.storageapp.utils.ImageUtils
import com.example.storageapp.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val etCurrentPassword = findViewById<EditText>(R.id.etCurrentPassword)
        val etEditPassword = findViewById<EditText>(R.id.etEditPassword)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfil)
        val btnVolver = findViewById<Button>(R.id.btnVolverPerfil)
        val pbProfile = findViewById<ProgressBar>(R.id.pbProfile)

        val correoActual = SessionManager.obtenerCorreo(this)
        val repo = StorageRepository(this)
        val usuarioActual = repo.obtenerUsuarioPorCorreo(correoActual)
        etEditAlias.setText(usuarioActual?.alias ?: SessionManager.obtenerAlias(this))
        val avatarInicial = usuarioActual?.avatarBlob ?: SessionManager.obtenerAvatar(this)
        ImageUtils.byteArrayToBitmap(avatarInicial)?.let { ivPerfilAvatar.setImageBitmap(it) }

        btnCambiarAvatar.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnVolver.setOnClickListener { finish() }

        btnGuardar.setOnClickListener {
            val alias = etEditAlias.text.toString().trim()
            val currentPass = etCurrentPassword.text.toString().trim()
            val nuevaPass = etEditPassword.text.toString().trim()

            if (alias.isEmpty()) {
                etEditAlias.error = "El alias no puede quedar vacío"
                return@setOnClickListener
            }
            if (currentPass.isEmpty()) {
                etCurrentPassword.error = "Ingresa tu contraseña actual para guardar cambios"
                return@setOnClickListener
            }
            if (nuevaPass.isNotEmpty()) {
                val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,}$".toRegex()
                if (!nuevaPass.matches(passwordPattern)) {
                    etEditPassword.error = "Mín. 10 caracteres, 1 mayúscula, 1 minúscula y 1 número"
                    return@setOnClickListener
                }
            }

            btnGuardar.isEnabled = false
            btnGuardar.text = "Guardando..."
            pbProfile.visibility = View.VISIBLE
            lifecycleScope.launch {
                val resultado = try {
                    withContext(Dispatchers.IO) {
                        repo.actualizarPerfil(correoActual, currentPass, alias, nuevaPass.ifEmpty { null }, nuevoAvatarBytes)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileActivity", "Error al actualizar perfil", e)
                    ProfileUpdateResult.LocalError
                } finally {
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar Cambios"
                    pbProfile.visibility = View.GONE
                }
                when (resultado) {
                    ProfileUpdateResult.SuccessOnline -> {
                        val actualizado = withContext(Dispatchers.IO) { repo.obtenerUsuarioPorCorreo(correoActual) }
                        SessionManager.actualizarPerfil(this@ProfileActivity, alias, actualizado?.avatarBlob ?: nuevoAvatarBytes ?: avatarInicial)
                        Toast.makeText(this@ProfileActivity, "Perfil actualizado y sincronizado en línea", Toast.LENGTH_LONG).show()
                        etCurrentPassword.text.clear()
                        etEditPassword.text.clear()
                        finish()
                    }
                    ProfileUpdateResult.SavedOfflinePending -> {
                        val actualizado = withContext(Dispatchers.IO) { repo.obtenerUsuarioPorCorreo(correoActual) }
                        SessionManager.actualizarPerfil(this@ProfileActivity, alias, actualizado?.avatarBlob ?: nuevoAvatarBytes ?: avatarInicial)
                        Toast.makeText(this@ProfileActivity, "Cambios guardados localmente. Se sincronizarán al volver la conexión.", Toast.LENGTH_LONG).show()
                        etCurrentPassword.text.clear()
                        etEditPassword.text.clear()
                        finish()
                    }
                    ProfileUpdateResult.WrongPassword -> {
                        etCurrentPassword.error = "Contraseña actual incorrecta"
                        Toast.makeText(this@ProfileActivity, "La contraseña actual no es correcta", Toast.LENGTH_LONG).show()
                    }
                    ProfileUpdateResult.LocalError -> {
                        Toast.makeText(this@ProfileActivity, "Error al actualizar el perfil", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
