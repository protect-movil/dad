package utez.edu.mx

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import utez.edu.mx.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private val db = FirebaseFirestore.getInstance()
    private var locationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Obtén el childId del Intent
        val childId = intent.getStringExtra("childId")
        if (childId != null) {
            listenToChildLocation(childId)
        } else {
            Log.e("MapsActivity", "No se recibió un childId válido")
        }
    }

    private fun listenToChildLocation(childId: String) {
        // Configura un listener en Firestore para escuchar cambios de ubicación
        locationListener = db.collection("usuarios")
            .document("hijos")
            .collection(childId)
            .document("details")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Error al escuchar cambios de ubicación: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val latitude = snapshot.getDouble("latitude")
                    val longitude = snapshot.getDouble("longitude")
                    val name = snapshot.getString("name") ?: "Sin nombre"

                    if (latitude != null && longitude != null) {
                        val location = LatLng(latitude, longitude)
                        Log.d("Firestore", "Ubicación actualizada: $latitude, $longitude para $name")

                        // Actualiza el marcador en el mapa
                        updateMap(location, name)
                    } else {
                        Log.e("Firestore", "Coordenadas no encontradas en Firestore para $childId")
                    }
                } else {
                    Log.e("Firestore", "No se encontró el documento 'details' para $childId")
                }
            }
    }

    private fun updateMap(location: LatLng, name: String) {
        mMap.clear() // Limpia el mapa antes de agregar el nuevo marcador
        mMap.addMarker(MarkerOptions().position(location).title(name))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    override fun onStop() {
        super.onStop()
        locationListener?.remove() // Detén el listener cuando la actividad se detiene
    }
}
