package com.example.storageapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.storageapp.data.StorageDbHelper
import com.example.storageapp.model.User
import com.example.storageapp.network.ApiClient
import com.example.storageapp.repository.StorageRepository
import com.example.storageapp.utils.SessionManager
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    private var pbLoginSync: ProgressBar? = null
    private lateinit var btnIngresar: Button
    private var syncsActivos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etCorreo = findViewById<EditText>(R.id.etCorreoLogin)
        val etPassword = findViewById<EditText>(R.id.etPasswordLogin)
        btnIngresar = findViewById(R.id.btnIngresar)
        val btnIrRegistro = findViewById<Button>(R.id.btnIrRegistro)
        pbLoginSync = findViewById(R.id.pbLoginSync)

        val repo = StorageRepository(this)
        iniciarSincronizacion()
        repo.sincronizarPendientesConServidor {
            runOnUiThread {
                if (!isFinishing && !isDestroyed) finalizarSincronizacion()
            }
        }

        btnIngresar.setOnClickListener {
            val correo = etCorreo.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = StorageDbHelper.getInstance(this)
            val usuarioLocal = db.autenticarUsuario(correo, password)

            if (usuarioLocal != null) {
                entrar(usuarioLocal)
            } else {
                // Si no está en local, buscar en el servidor MySQL (para cuentas creadas en otros dispositivos)
                iniciarSincronizacion()
                btnIngresar.isEnabled = false
                thread {
                    val usuarioRemoto = ApiClient.loginRemoto(correo, password)
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        btnIngresar.isEnabled = true
                        finalizarSincronizacion()
                        if (usuarioRemoto != null) {
                            db.registrarUsuario(usuarioRemoto)
                            entrar(usuarioRemoto)
                        } else {
                            Toast.makeText(this, "Credenciales incorrectas o usuario no encontrado", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun entrar(usuario: User) {
        SessionManager.guardarSesion(
            this,
            usuario.correo,
            usuario.alias,
            "${usuario.nombre} ${usuario.apellidos}",
            usuario.avatarBlob
        )
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun iniciarSincronizacion() {
        syncsActivos++
        pbLoginSync?.visibility = View.VISIBLE
    }

    private fun finalizarSincronizacion() {
        syncsActivos = (syncsActivos - 1).coerceAtLeast(0)
        pbLoginSync?.visibility = if (syncsActivos > 0) View.VISIBLE else View.GONE
    }
}