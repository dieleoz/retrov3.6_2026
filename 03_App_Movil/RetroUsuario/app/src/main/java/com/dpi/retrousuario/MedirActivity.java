package com.dpi.retrousuario;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dpi.retrousuario.dominio.CsvMedidas;
import com.dpi.retrousuario.dominio.FilaMedida;
import com.dpi.retrousuario.dominio.SesionMedicion;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * RF-USR-04, RF-USR-05, RF-USR-06, pantalla 4 (SPEC §1): color → Medir, cada disparo se guarda solo, **cero
 * tecleo**. Sin selector de disparos (RF-USR-04, T-USR-28): el único control de
 * {@code lecturasPorColor} está en {@link AjustesActivity}, a la que se llega por
 * {@link R.id#btnAjustes}, nunca desde aquí.
 */
public final class MedirActivity extends AppCompatActivity {

    private TextView tvResultado;
    private TextView tvContador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medir);
        tvResultado = findViewById(R.id.tvResultado);
        tvContador = findViewById(R.id.tvContador);

        enlazarBotonColor(R.id.btnBlanco, "blanco");
        enlazarBotonColor(R.id.btnAmarillo, "amarillo");
        enlazarBotonColor(R.id.btnVerde, "verde");
        enlazarBotonColor(R.id.btnRojo, "rojo");
        enlazarBotonColor(R.id.btnAzul, "azul");
        enlazarBotonColor(R.id.btnAnaranjado, "anaranjado");

        ((Button) findViewById(R.id.btnAjustes)).setOnClickListener(
                v -> startActivity(new Intent(this, AjustesActivity.class)));
        ((Button) findViewById(R.id.btnExportar)).setOnClickListener(v -> exportar());
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarContador();
    }

    private void enlazarBotonColor(int idBoton, String color) {
        Button b = findViewById(idBoton);
        b.setOnClickListener(v -> medir(color));
    }

    /** Cero tecleo (RF-USR-06): color → Medir, sin más entrada del operador. */
    private void medir(String color) {
        SesionMedicion sesion = SesionHolder.sesion();
        if (sesion == null) {
            tvResultado.setText(R.string.medir_sin_equipo);
            return;
        }
        tvResultado.setText(R.string.medir_midiendo);
        new Thread(() -> {
            double[] gps = UbicacionGps.ultimaConocida(getApplicationContext());
            String fechaHora = fechaHoraIsoAhora();
            String lat = gps != null ? CsvMedidas.formatearCoordenada(gps[0]) : "";
            String lon = gps != null ? CsvMedidas.formatearCoordenada(gps[1]) : "";
            String gpsEstado = gps != null ? "con_posicion" : "sin_posicion";
            FilaMedida fila = sesion.medir(color, fechaHora, lat, lon, gpsEstado);
            runOnUiThread(() -> mostrarResultado(fila));
        }, "medir-" + color).start();
    }

    private void mostrarResultado(FilaMedida fila) {
        if (fila == null) {
            tvResultado.setText(R.string.medir_serie_anulada);
        } else {
            tvResultado.setText(getString(R.string.medir_resultado, fila.color, fila.media, fila.minimo,
                    fila.valido ? "SI" : "NO"));
        }
        actualizarContador();
    }

    private void actualizarContador() {
        SesionMedicion sesion = SesionHolder.sesion();
        int n = sesion == null ? 0 : sesion.filas().size();
        tvContador.setText(getString(R.string.medir_filas_guardadas, n));
    }

    private void exportar() {
        SesionMedicion sesion = SesionHolder.sesion();
        if (sesion == null) {
            return;
        }
        new Thread(() -> {
            File carpeta = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "RetroUsuario");
            File zip = sesion.exportar(carpeta, new Date(), TimeZone.getDefault());
            runOnUiThread(() -> tvResultado.setText(getString(R.string.medir_exportado, zip.getAbsolutePath())));
        }, "exportar").start();
    }

    /** ISO 8601 con zona (RF-USR-06, M-6): "XXX" da el offset con dos puntos, p. ej. "-05:00" (disponible desde API 24). */
    private static String fechaHoraIsoAhora() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT);
        return f.format(new Date());
    }
}
