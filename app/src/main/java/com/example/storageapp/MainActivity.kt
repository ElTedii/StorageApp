package com.example.storageapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnIrCaptura = findViewById<Button>(R.id.btnIrCaptura)
        val btnIrConsulta = findViewById<Button>(R.id.btnIrConsulta)

        btnIrCaptura.setOnClickListener {
            val intent = Intent(this, SaveActivity::class.java)
            startActivity(intent)
        }

        btnIrConsulta.setOnClickListener {
            val intent = Intent(this, ViewActivity::class.java)
            startActivity(intent)
        }
    }
}