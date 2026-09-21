package com.dpi.retrousuario;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * RF-USR-04, pantalla 5 (SPEC §1): único campo, {@code lecturasPorColor} (por defecto 3). Se llega
 * desde un menú, **nunca desde la pantalla de medir** (T-USR-28): {@link MedirActivity} no tiene
 * ningún control de este parámetro, sólo un botón que abre esta pantalla.
 */
public final class AjustesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        EditText etLecturas = findViewById(R.id.etLecturasPorColor);
        etLecturas.setText(String.valueOf(SesionHolder.parametros().lecturasPorColor()));

        Button btnGuardar = findViewById(R.id.btnGuardarAjustes);
        btnGuardar.setOnClickListener(v -> guardar(etLecturas));
    }

    private void guardar(EditText etLecturas) {
        String texto = etLecturas.getText().toString().trim();
        int valor;
        try {
            valor = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.ajustes_valor_invalido, Toast.LENGTH_SHORT).show();
            return;
        }
        if (valor < 1) {
            Toast.makeText(this, R.string.ajustes_valor_invalido, Toast.LENGTH_SHORT).show();
            return;
        }
        SesionHolder.parametros().lecturasPorColor(valor);
        SesionHolder.guardarLecturasPorColor(getApplicationContext(), valor); // B3: sobrevive a la muerte del proceso.
        Toast.makeText(this, R.string.ajustes_guardado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
