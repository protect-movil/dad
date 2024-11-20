package utez.edu.mx

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddChildActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var etChildCode: EditText
    private lateinit var btnAddChild: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_child)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Referencias a los elementos de la interfaz
        etChildCode = findViewById(R.id.etChildCode)
        btnAddChild = findViewById(R.id.btnAddChild)

        // Lógica para el botón de agregar hijo
        btnAddChild.setOnClickListener {
            val childCode = etChildCode.text.toString().trim()
            if (childCode.isEmpty()) {
                etChildCode.error = "Por favor, ingrese el código del hijo"
                etChildCode.requestFocus()
                return@setOnClickListener
            }
            addChildToParent(childCode)
        }
    }

    private fun addChildToParent(childCode: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("children")
            .whereEqualTo("code", childCode)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val childDocument = result.documents[0]
                    val childId = childDocument.id

                    // Registrar la invitación
                    val newInvitation = hashMapOf(
                        "childId" to childId,
                        "parentId" to userId,
                        "status" to "pending"
                    )
                    db.collection("invitations").add(newInvitation)
                        .addOnSuccessListener { documentReference ->
                            Toast.makeText(this, "Invitación enviada con éxito", Toast.LENGTH_SHORT).show()
                            finish() // Cierra la actividad
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al enviar la invitación: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Código de hijo no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al buscar el código: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
