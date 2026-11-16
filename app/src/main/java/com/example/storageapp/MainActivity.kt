package com.example.storageapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.storageapp.utils.ImageUtils
import com.example.storageapp.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var ivAvatarHeader: ImageView
    private lateinit var tvUserAlias: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivAvatarHeader = findViewById(R.id.ivAvatarHeader)
        tvUserAlias = findViewById(R.id.tvUserAlias)
        val btnIrPerfil = findViewById<Button>(R.id.btnIrPerfil)
        val btnIrCaptura = findViewById<Button>(R.id.btnIrCaptura)
        val btnIrConsulta = findViewById<Button>(R.id.btnIrConsulta)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        btnIrPerfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnIrCaptura.setOnClickListener {
            startActivity(Intent(this, SaveActivity::class.java))
        }

        btnIrConsulta.setOnClickListener {
            startActivity(Intent(this, ViewActivity::class.java))
        }

        btnCerrarSesion.setOnClickListener {
            SessionManager.cerrarSesion(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val alias = SessionManager.obtenerAlias(this)
        tvUserAlias.text = "¡Hola, $alias!"

        val avatarBytes = SessionManager.obtenerAvatar(this)
        val bitmap = ImageUtils.byteArrayToBitmap(avatarBytes)
        if (bitmap != null) {
            ivAvatarHeader.setImageBitmap(bitmap)
        }
    }
}