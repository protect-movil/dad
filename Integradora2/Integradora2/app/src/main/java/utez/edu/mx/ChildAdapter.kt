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
        Log.d("Adapter", "Vinculando hijo: ${child.name}, ID: ${child.id}")
        holder.bind(child)
    }

    override fun getItemCount(): Int = childrenList.size

    inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvChildName: TextView = itemView.findViewById(R.id.tvChildName)
        private val btnViewLocation: Button = itemView.findViewById(R.id.btnViewLocation)
        private val btnEditChildName: Button = itemView.findViewById(R.id.btnEditChildName)

        fun bind(child: Child) {
            Log.d("Adapter", "Configurando vista para: ${child.name}")
            tvChildName.text = child.name

            btnViewLocation.setOnClickListener {
                Toast.makeText(context, "Funcionalidad no implementada aún", Toast.LENGTH_SHORT)
                    .show()
            }

            btnEditChildName.setOnClickListener {
                showEditNameDialog(child)
            }
        }

        private fun showEditNameDialog(child: Child) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Editar Nombre")

            val input = EditText(context)
            input.hint = "Nuevo nombre"
            builder.setView(input)

            builder.setPositiveButton("Guardar") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateChildNameInFirestore(child, newName)
                } else {
                    Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT)
                        .show()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        private fun updateChildNameInFirestore(child: Child, newName: String) {
            val db = FirebaseFirestore.getInstance()

            // Ruta corregida para actualizar el nombre
            db.collection("usuarios")
                .document("hijos")
                .collection(child.id) // Colección del hijo con su ID
                .document("details") // Documento "details" donde se encuentra el nombre
                .update("name", newName) // Actualiza el campo "name"
                .addOnSuccessListener {
                    child.name = newName
                    notifyItemChanged(adapterPosition) // Notifica al adaptador para actualizar la vista
                    Toast.makeText(context, "Nombre actualizado exitosamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al actualizar el nombre", Toast.LENGTH_SHORT).show()
                    Log.e("FirestoreUpdate", "Error al actualizar nombre: ${e.message}")
                }
        }


    }
}
