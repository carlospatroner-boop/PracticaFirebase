package com.tunombre.practicafirebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private TextView txt_temperatura, txt_humedad, txt_presion, txt_velocidad;
    private EditText editSetTemperatura, editSetHumedad;
    private Button btnSetTemperatura, btnSetHumedad, btnIrAMonitoreo, btnRastrearCompanero;

    // Variables de Firebase
    DatabaseReference HumedadRef;
    DatabaseReference presionRef;
    DatabaseReference VelocidadRef;
    DatabaseReference TemperauraRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicialización de Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        HumedadRef = database.getReference("sensores/humedad");
        presionRef = database.getReference("sensores/presion");
        VelocidadRef = database.getReference("sensores/velocidad");
        TemperauraRef = database.getReference("sensores/temperatura");

        // Referencias de UI Monitoreo
        txt_temperatura = findViewById(R.id.valor_Temperatura);
        txt_humedad = findViewById(R.id.valor_Humedad);
        txt_presion = findViewById(R.id.valor_Presion);
        txt_velocidad = findViewById(R.id.valor_Velocidad);

        // Referencias de UI Seteo
        editSetTemperatura = findViewById(R.id.edit_SetTemperatura);
        editSetHumedad = findViewById(R.id.edit_SetHumedad);
        btnSetTemperatura = findViewById(R.id.btn_SetTemperatura);
        btnSetHumedad = findViewById(R.id.btn_SetHumedad);
        btnIrAMonitoreo = findViewById(R.id.btn_IrAMonitoreo);
        btnRastrearCompanero = findViewById(R.id.btn_RastrearCompanero);

        // Listeners para actualizar la UI en tiempo real
        TemperauraRef.addValueEventListener(setListener(txt_temperatura, "°C"));
        HumedadRef.addValueEventListener(setListener(txt_humedad, "%"));
        presionRef.addValueEventListener(setListener(txt_presion, " atm"));
        VelocidadRef.addValueEventListener(setListener(txt_velocidad, " km/h"));

        // Eventos Click para Seteo
        btnSetTemperatura.setOnClickListener(v -> {
            String valor = editSetTemperatura.getText().toString();
            if (!valor.isEmpty()) {
                TemperauraRef.setValue(valor);
                Toast.makeText(MainActivity.this, "Temperatura actualizada", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetHumedad.setOnClickListener(v -> {
            String valor = editSetHumedad.getText().toString();
            if (!valor.isEmpty()) {
                HumedadRef.setValue(valor);
                Toast.makeText(MainActivity.this, "Humedad actualizada", Toast.LENGTH_SHORT).show();
            }
        });

        // Navegación a Mi Ubicación
        btnIrAMonitoreo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Monitoreo.class);
            startActivity(intent);
        });

        // Navegación a Rastrear Compañero
        btnRastrearCompanero.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VerUbicacion.class);
            startActivity(intent);
        });
    }

    private ValueEventListener setListener(final TextView textView, final String unit) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    textView.setText(snapshot.getValue().toString() + unit);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
    }
}