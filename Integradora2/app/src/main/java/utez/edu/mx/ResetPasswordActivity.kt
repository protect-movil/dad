package utez.edu.mx
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import utez.edu.mx.R

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Referencias a los elementos en el diseño
        val emailField = findViewById<EditText>(R.id.etEmail)
        val resetButton = findViewById<Button>(R.id.btnResetPassword)

        // Configurar el botón de envío de recuperación de contraseña
        resetButton.setOnClickListener {
            val email = emailField.text.toString().trim()

            if (email.isEmpty()) {
                emailField.error = "Por favor ingresa tu correo electrónico"
                emailField.requestFocus()
                return@setOnClickListener
            }

            // Enviar enlace de recuperación de contraseña
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Se ha enviado un enlace a tu correo", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error al enviar el enlace: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
