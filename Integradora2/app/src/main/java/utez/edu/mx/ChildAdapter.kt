package utez.edu.mx

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChildAdapter(
    private val context: Context,
    private val childrenList: MutableList<Child>
) : RecyclerView.Adapter<ChildAdapter.ChildViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_child, parent, false)
        return ChildViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        val child = childrenList[position]
        holder.bind(child, position)
    }

    override fun getItemCount(): Int = childrenList.size

    inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvChildName: TextView = itemView.findViewById(R.id.tvChildName)
        private val btnViewLocation: Button = itemView.findViewById(R.id.btnViewLocation)
        private val btnEditChildName: Button = itemView.findViewById(R.id.btnEditChildName)

        fun bind(child: Child, position: Int) {
            tvChildName.text = child.name

            // Acción para ver la ubicación
            btnViewLocation.setOnClickListener {
                Toast.makeText(context, "Funcionalidad no implementada aún", Toast.LENGTH_SHORT).show()
            }

            // Acción para editar el nombre
            btnEditChildName.setOnClickListener {
                showEditNameDialog(child, position)
            }
        }

        private fun showEditNameDialog(child: Child, position: Int) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Editar Nombre")

            // Crear un EditText para ingresar el nuevo nombre
            val input = EditText(context)
            input.hint = "Nuevo nombre"
            builder.setView(input)

            // Configurar el botón de confirmación
            builder.setPositiveButton("Guardar") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateChildNameInFirestore(child, newName, position)
                } else {
                    Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            // Botón de cancelar
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        private fun updateChildNameInFirestore(child: Child, newName: String, position: Int) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val db = FirebaseFirestore.getInstance()

            // Actualizar el nombre en Firestore
            db.collection("usuarios").document(userId).collection("hijos")
                .document(child.id)
                .update("name", newName)
                .addOnSuccessListener {
                    // Actualizar el nombre en la lista local y notificar el cambio
                    child.name = newName
                    notifyItemChanged(adapterPosition)
                    Toast.makeText(context, "Nombre actualizado exitosamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al actualizar el nombre", Toast.LENGTH_SHORT).show()
                    Log.e("FirestoreUpdate", "Error: ${e.message}")
                }
        }
    }
}
