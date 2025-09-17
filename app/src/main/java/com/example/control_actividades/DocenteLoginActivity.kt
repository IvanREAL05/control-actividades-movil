package com.example.control_actividades

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class DocenteLoginActivity : AppCompatActivity() {

    private lateinit var btnLogin: Button
    private lateinit var etUsuario: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docente_login)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(resources.getColor(android.R.color.white))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Login Docente"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        etUsuario = findViewById(R.id.etUsuario)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginDocente(usuario, password)
        }

    }

    private fun loginDocente(usuario: String, password: String) {
        val request = LoginRequest(usuario_login = usuario, contrasena = password)

        RetrofitClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null && loginResponse.success) {

                        val usuarioData = loginResponse.data?.usuario

                        if (usuarioData?.rol == "docente") {
                            // Guardar datos del docente en SharedPreferences
                            val sharedPref = getSharedPreferences("docentePrefs", MODE_PRIVATE)
                            sharedPref.edit()
                                .putInt("id_profesor", usuarioData.id_profesor ?: -1)
                                .putString("nombre_profesor", usuarioData.nombre_profesor ?: "")
                                .apply()

                            Toast.makeText(
                                this@DocenteLoginActivity,
                                "Bienvenido ${usuarioData.nombre_profesor}",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Ir al home del docente
                            startActivity(Intent(this@DocenteLoginActivity, DocenteHomeActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@DocenteLoginActivity, "No es docente", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@DocenteLoginActivity, loginResponse?.message ?: "Error en login", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@DocenteLoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@DocenteLoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


}