package com.tunombre.practicafirebase;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class VerUbicacion extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mapa;
    private Marker marker = null;
    private DatabaseReference coordinatesRef;
    private TextView txtDireccion, txtLatitud, txtLongitud;
    
    // Variables para guardar la última ubicación recibida antes de que el mapa cargue
    private Double lastLat = null;
    private Double lastLng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ver_ubicacion);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header_ver), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtDireccion = findViewById(R.id.txtDireccionVer);
        txtLatitud = findViewById(R.id.txtLatitudVer);
        txtLongitud = findViewById(R.id.txtLongitudVer);

        // Inicializar Firebase
        coordinatesRef = FirebaseDatabase.getInstance().getReference("Coordenadas");

        // Cargar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_ver);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Escuchar cambios del compañero en tiempo real
        escucharCompanero();
    }

    private void escucharCompanero() {
        coordinatesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.child("latitud").getValue() != null && snapshot.child("longitud").getValue() != null) {
                    lastLat = snapshot.child("latitud").getValue(Double.class);
                    lastLng = snapshot.child("longitud").getValue(Double.class);
                    
                    // Actualizar Coordenadas en la UI
                    txtLatitud.setText(String.format(Locale.US, "Lat: %.5f", lastLat));
                    txtLongitud.setText(String.format(Locale.US, "Lon: %.5f", lastLng));
                    
                    // Intentar actualizar el mapa
                    actualizarMapa(lastLat, lastLng);
                    obtenerDireccion(lastLat, lastLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(VerUbicacion.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarMapa(double lat, double lng) {
        if (mapa == null) return;

        LatLng latLng = new LatLng(lat, lng);
        if (marker == null) {
            marker = mapa.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Compañero")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
        } else {
            marker.setPosition(latLng);
            mapa.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    private void obtenerDireccion(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                txtDireccion.setText("El compañero está en: " + addresses.get(0).getAddressLine(0));
            }
        } catch (IOException e) {
            txtDireccion.setText("Dirección no disponible");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.getUiSettings().setZoomControlsEnabled(true);
        
        // Si ya recibimos datos de Firebase antes de que el mapa estuviera listo,
        // actualizamos el marcador ahora.
        if (lastLat != null && lastLng != null) {
            actualizarMapa(lastLat, lastLng);
        }
    }
}