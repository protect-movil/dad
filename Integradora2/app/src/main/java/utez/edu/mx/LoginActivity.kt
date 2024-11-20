package utez.edu.mx

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import utez.edu.mx.DashboardActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth  // Autenticación de Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Referencias a los elementos en el diseño
        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerButton = findViewById<Button>(R.id.btnCreateAccount)
        val forgotPasswordText = findViewById<TextView>(R.id.tvForgotPassword) // Referencia al TextView

        // Lógica para redirigir a la pantalla de registro
        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Lógica para redirigir a la pantalla de recuperación de contraseña
        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }

        // Lógica para iniciar sesión
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty()) {
                emailField.error = "El correo electrónico es obligatorio"
                emailField.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordField.error = "La contraseña es obligatoria"
                passwordField.requestFocus()
                return@setOnClickListener
            }

            // Intento de inicio de sesión en Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()  // Finalizar LoginActivity
                    } else {
                        Toast.makeText(
                            this,
                            "Error al iniciar sesión: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}
