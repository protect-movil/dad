package utez.edu.mx

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var childAdapter: ChildAdapter
    private lateinit var childrenList: MutableList<Child>
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var navView: NavigationView
    private lateinit var db: FirebaseFirestore
    private lateinit var tvEmptyMessage: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        progressBar = findViewById(R.id.progressBar)

        // Encuentra el TextView en el diseño
        val tvParentId = findViewById<TextView>(R.id.tvParentId)

        // Obtén el UID del usuario actual y actualiza el TextView
        val parentId = auth.currentUser?.uid
        if (parentId != null) {
            tvParentId.text = "UID: $parentId"
        } else {
            tvParentId.text = "No hay usuario autenticado"
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        recyclerView = findViewById(R.id.rvChildrenList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)

        childrenList = mutableListOf()
        childAdapter = ChildAdapter(this, childrenList)
        recyclerView.adapter = childAdapter

        // Configura el botón de enviar invitación
        val btnAddChild = findViewById<Button>(R.id.btnAddChild)
        btnAddChild.setOnClickListener {
            showInvitationDialog()
        }

        // Configura el botón de actualizar
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        btnRefresh.setOnClickListener {
            refreshChildrenList()
        }

        // Escuchar invitaciones aceptadas
        listenForChildrenUpdates()
        setupRefreshButton()
        //fetchChildDetails()
    }

    private fun showInvitationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_send_invitation, null)
        val childIdInput = dialogView.findViewById<EditText>(R.id.etChildId)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Enviar Invitación")
            .setView(dialogView)
            .setPositiveButton("Enviar") { _, _ ->
                val childId = childIdInput.text.toString().trim()
                if (childId.isNotEmpty()) {
                    sendInvitationToChild(childId)
                } else {
                    Toast.makeText(this, "Por favor ingresa un ID válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun sendInvitationToChild(childId: String) {
        val parentId = auth.currentUser?.uid ?: return

        val invitation = hashMapOf(
            "childId" to childId,
            "parentId" to parentId,
            "status" to "pending"
        )

        db.collection("invitations")
            .add(invitation)
            .addOnSuccessListener {
                Log.d("Firestore", "Invitación enviada correctamente al hijo con ID: $childId")
                Toast.makeText(this, "Invitación enviada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al enviar invitación: ${e.message}")
                Toast.makeText(this, "Error al enviar invitación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRefreshButton() {
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        btnRefresh.setOnClickListener {
            refreshChildrenList()
        }
    }

    private fun listenForChildrenUpdates() {
        db.collection("usuarios").document("hijos")
            .collection("hijos")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("Firestore", "Error al escuchar cambios en hijos: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    childrenList.clear() // Limpia la lista para reflejar los datos actuales
                    for (document in snapshots.documents) {
                        val child = document.toObject(Child::class.java)
                        if (child != null) {
                            child.id = document.id // Asigna el ID del documento al objeto
                            childrenList.add(child)
                        }
                    }
                    childAdapter.notifyDataSetChanged() // Notifica al adaptador que hay cambios
                    updateEmptyMessageVisibility()
                    Log.d("Firestore", "Lista de hijos actualizada: ${childrenList.size} hijos")
                }
            }
    }

    private fun refreshChildrenList() {
        val parentId = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        Log.d("Firestore", "Iniciando consulta de hijos para el parentId: $parentId")

        // Consulta las invitaciones aceptadas
        db.collection("invitations")
            .whereEqualTo("parentId", parentId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val childIds = querySnapshot.documents.mapNotNull { it.getString("childId") }
                Log.d("Firestore", "Número de invitaciones aceptadas: ${querySnapshot.size()}")
                Log.d("Firestore", "childIds obtenidos: $childIds")

                if (childIds.isEmpty()) {
                    Log.d("Firestore", "No se encontraron hijos aceptados para este padre.")
                    childrenList.clear()
                    childAdapter.notifyDataSetChanged()
                    updateEmptyMessageVisibility()
                    progressBar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                // Limpia la lista actual antes de agregar nuevos datos
                childrenList.clear()

                // Itera sobre los childIds y consulta en "usuarios/hijos/{childId}/details"
                childIds.forEach { childId ->
                    db.collection("usuarios")
                        .document("hijos")
                        .collection(childId)
                        .document("details") // Accede al subdocumento "details"
                        .get()
                        .addOnSuccessListener { detailsSnapshot ->
                            if (detailsSnapshot.exists()) {
                                val name = detailsSnapshot.getString("name")
                                if (name != null) {
                                    val child = Child(
                                        id = childId,
                                        name = name
                                    )
                                    childrenList.add(child)
                                    Log.d("Firestore", "Hijo agregado: ${child.name}, ID: ${child.id}")
                                }
                            } else {
                                Log.d("Firestore", "No se encontró el subdocumento 'details' para el hijo con ID: $childId")
                            }

                            // Notifica al adaptador cuando se haya procesado el último hijo
                            if (childIds.last() == childId) {
                                Log.d("Adapter", "Tamaño final de la lista de hijos: ${childrenList.size}")
                                childAdapter.notifyDataSetChanged()
                                updateEmptyMessageVisibility()
                                progressBar.visibility = View.GONE
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error al obtener detalles del hijo: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al cargar invitaciones: ${e.message}")
                progressBar.visibility = View.GONE
            }
    }




    private fun updateEmptyMessageVisibility() {
        tvEmptyMessage.visibility = if (childrenList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
