package utez.edu.mx

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var childAdapter: ChildAdapter
    private lateinit var childrenList: MutableList<Child>
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var navView: NavigationView
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

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
        childrenList = mutableListOf()
        childAdapter = ChildAdapter(this, childrenList)
        recyclerView.adapter = childAdapter

        // Botón para enviar invitaciones
        val btnAddChild = findViewById<Button>(R.id.btnAddChild)
        btnAddChild.setOnClickListener {
            showInvitationDialog()
        }

        fetchAcceptedInvitations()
    }

    /**
     * Mostrar un cuadro de diálogo para ingresar el ID del hijo y enviar la invitación
     */
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

    /**
     * Enviar invitación al hijo ingresado
     */
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
                Toast.makeText(this, "Invitación enviada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al enviar invitación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Obtener todas las invitaciones aceptadas y actualizar la lista
     */
    private fun fetchAcceptedInvitations() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("invitations")
            .whereEqualTo("parentId", userId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                childrenList.clear()
                for (document in result) {
                    val childId = document.getString("childId") ?: continue
                    fetchChildDetails(childId)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar los datos.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Obtener detalles de cada hijo aceptado
     */
    private fun fetchChildDetails(childId: String) {
        db.collection("children").document(childId)
            .get()
            .addOnSuccessListener { document ->
                val child = document.toObject(Child::class.java)
                if (child != null) {
                    childrenList.add(child)
                    childAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener detalles del hijo.", Toast.LENGTH_SHORT).show()
            }
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
