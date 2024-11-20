package utez.edu.mx
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializar Firebase Authentication y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Referencias a los elementos de la interfaz
        val nameField = findViewById<EditText>(R.id.etName)
        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val createAccountButton = findViewById<Button>(R.id.btnCreateAccount)

        // Lógica para el botón de crear cuenta
        createAccountButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            // Validación de los campos
            if (name.isEmpty()) {
                nameField.error = "El nombre es obligatorio"
                nameField.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                emailField.error = "El correo es obligatorio"
                emailField.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordField.error = "La contraseña es obligatoria"
                passwordField.requestFocus()
                return@setOnClickListener
            }
            if (password.length < 8) {
                passwordField.error = "La contraseña debe tener al menos 8 caracteres"
                passwordField.requestFocus()
                return@setOnClickListener
            }
            if (!password.matches(Regex(".*[!@#\$%^&*()\\-_=+\\[\\]{};:'\"\\\\|,.<>?/`~].*"))) {
                passwordField.error = "La contraseña debe contener al menos un carácter especial"
                passwordField.requestFocus()
                return@setOnClickListener
            }

            // Intentar crear el usuario en Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid

                        // Almacenar el nombre del usuario en Firestore
                        if (userId != null) {
                            val user = hashMapOf(
                                "name" to name,
                                "email" to email
                            )
                            db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    finish() // Cierra la actividad y regresa al login
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error al guardar el usuario: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
