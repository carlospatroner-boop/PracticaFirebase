package com.tunombre.practicafirebase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Monitoreo extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap mapa;
    Marker marker = null;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    DatabaseReference coordinatesRef;
    private TextView txtDireccion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_monitoreo);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtDireccion = findViewById(R.id.txtDireccion);
        FloatingActionButton btnTipoMapa = findViewById(R.id.btnTipoMapa);

        // Funcionalidad  1: Cambiar tipo de mapa
        btnTipoMapa.setOnClickListener(v -> {
            if (mapa != null) {
                int tipoActual = mapa.getMapType();
                if (tipoActual == GoogleMap.MAP_TYPE_NORMAL) mapa.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                else if (tipoActual == GoogleMap.MAP_TYPE_SATELLITE) mapa.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                else mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        coordinatesRef = mDatabase.child("Coordenadas");

        coordinatesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.child("latitud").getValue() != null && snapshot.child("longitud").getValue() != null) {
                    double lat = snapshot.child("latitud").getValue(Double.class);
                    double lng = snapshot.child("longitud").getValue(Double.class);
                    actualizarMarcadorMapa(lat, lng);
                    obtenerDireccion(lat, lng); // Funcionalidad Sorpresa 2
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
        setupLocationUpdates();
    }

    // Funcionalidad  2: Geocodificación Inversa
    private void obtenerDireccion(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0);
                txtDireccion.setText(address);
            }
        } catch (IOException e) {
            txtDireccion.setText("Dirección no disponible");
        }
    }

    private void grabarNuevaPosicionGPS(Location location) {
        TextView txtLatitud = findViewById(R.id.txtLatitud);
        TextView txtLongitud = findViewById(R.id.txtLongitud);
        txtLatitud.setText(String.format(Locale.US, "%.5f", location.getLatitude()));
        txtLongitud.setText(String.format(Locale.US, "%.5f", location.getLongitude()));
        coordinatesRef.child("latitud").setValue(location.getLatitude());
        coordinatesRef.child("longitud").setValue(location.getLongitude());
    }

    private void actualizarMarcadorMapa(double lat, double lng) {
        if (mapa == null) return;
        LatLng latLng = new LatLng(lat, lng);
        if (marker == null) marker = mapa.addMarker(new MarkerOptions().position(latLng).title("Tu Pos"));
        else marker.setPosition(latLng);
        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    @SuppressLint("MissingPermission")
    private void setupLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(3000); // Actualización cada 3 segundos
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) grabarNuevaPosicionGPS(locationResult.getLastLocation());
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) setupLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) { mapa = googleMap; }
}