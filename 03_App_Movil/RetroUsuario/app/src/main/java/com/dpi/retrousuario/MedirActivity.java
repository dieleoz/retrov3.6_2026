package com.dpi.retrousuario;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dpi.retrousuario.dominio.CsvMedidas;
import com.dpi.retrousuario.dominio.EstrategiaExportacion;
import com.dpi.retrousuario.dominio.FilaMedida;
import com.dpi.retrousuario.dominio.PermisoUbicacion;
import com.dpi.retrousuario.dominio.SesionMedicion;

import java.io.File;
import java.io.IOException;
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

    private static final int PETICION_PERMISO_EXPORTAR = 100;
    private static final int PETICION_PERMISO_UBICACION = 101;

    private TextView tvResultado;
    private TextView tvContador;
    private Button[] botonesColor;
    private Button btnAjustes;
    private Button btnExportar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medir);
        tvResultado = findViewById(R.id.tvResultado);
        tvContador = findViewById(R.id.tvContador);
        btnAjustes = findViewById(R.id.btnAjustes);
        btnExportar = findViewById(R.id.btnExportar);
        botonesColor = new Button[] {
                findViewById(R.id.btnBlanco), findViewById(R.id.btnAmarillo), findViewById(R.id.btnVerde),
                findViewById(R.id.btnRojo), findViewById(R.id.btnAzul), findViewById(R.id.btnAnaranjado),
                findViewById(R.id.btnMarron),
        };

        enlazarBotonColor(R.id.btnBlanco, "blanco");
        enlazarBotonColor(R.id.btnAmarillo, "amarillo");
        enlazarBotonColor(R.id.btnVerde, "verde");
        enlazarBotonColor(R.id.btnRojo, "rojo");
        enlazarBotonColor(R.id.btnAzul, "azul");
        enlazarBotonColor(R.id.btnAnaranjado, "anaranjado");
        enlazarBotonColor(R.id.btnMarron, "marron"); // M6.

        btnAjustes.setOnClickListener(v -> startActivity(new Intent(this, AjustesActivity.class)));
        btnExportar.setOnClickListener(v -> exportar());

        pedirPermisoUbicacionSiHaceFalta(); // arq C2: al entrar a medir, una sola vez por proceso.
    }

    /**
     * arq C2 (condición sobre 0.3.1): la app nunca pedía {@code ACCESS_FINE_LOCATION} en tiempo de
     * ejecución, pese a declararlo en el manifiesto. RF-USR-15: se pida o no, o se conceda o no, la
     * medida nunca se bloquea por esto — {@link UbicacionGps} cae a {@code sin_posicion} igual con
     * {@link SecurityException} que sin permiso declarado. La decisión de SI toca pedirlo (no
     * concedido, y no pedido ya en este proceso) está en {@link PermisoUbicacion}, probada en la JVM.
     */
    private void pedirPermisoUbicacionSiHaceFalta() {
        boolean concedido = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (PermisoUbicacion.debePedirse(concedido, SesionHolder.permisoUbicacionPedido())) {
            SesionHolder.marcarPermisoUbicacionPedido();
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PETICION_PERMISO_UBICACION);
        }
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

    /**
     * A1 (ALTO): deshabilita los botones de color, Ajustes y Exportar mientras hay una medida o una
     * exportación en curso (primera línea de defensa; el cerrojo de {@link SesionMedicion#medir} es
     * la segunda, en el dominio).
     */
    private void establecerControlesEnCurso(boolean enCurso) {
        boolean habilitados = !enCurso;
        for (Button b : botonesColor) {
            b.setEnabled(habilitados);
        }
        btnAjustes.setEnabled(habilitados);
        btnExportar.setEnabled(habilitados);
    }

    /**
     * Cero tecleo (RF-USR-06): color → Medir, sin más entrada del operador.
     *
     * <p>B-1 (condición QA-8 sobre 32c785d): el hilo de medir captura cualquier excepción y la
     * muestra, en vez de morir en silencio dejando los controles deshabilitados para siempre (un
     * {@link Thread} sin manejador propio no propaga la excepción a ningún sitio visible).</p>
     */
    private void medir(String color) {
        SesionMedicion sesion = SesionHolder.sesion();
        if (sesion == null) {
            tvResultado.setText(R.string.medir_sin_equipo);
            return;
        }
        establecerControlesEnCurso(true);
        tvResultado.setText(R.string.medir_midiendo);
        new Thread(() -> {
            try {
                UbicacionGps.Resultado gps = UbicacionGps.ultimaConocida(getApplicationContext());
                String fechaHora = fechaHoraIsoAhora();
                boolean conPosicion = "con_posicion".equals(gps.gpsEstado);
                String lat = conPosicion ? CsvMedidas.formatearCoordenada(gps.latitud) : "";
                String lon = conPosicion ? CsvMedidas.formatearCoordenada(gps.longitud) : "";
                FilaMedida fila = sesion.medir(color, fechaHora, lat, lon, gps.gpsEstado);
                runOnUiThread(() -> {
                    mostrarResultado(fila);
                    establecerControlesEnCurso(false);
                });
            } catch (Exception e) {
                String mensaje = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> {
                    tvResultado.setText(getString(R.string.medir_error, mensaje));
                    establecerControlesEnCurso(false);
                });
            }
        }, "medir-" + color).start();
    }

    private void mostrarResultado(FilaMedida fila) {
        if (fila == null) {
            tvResultado.setText(R.string.medir_serie_anulada);
        } else {
            // M3: se muestran las lecturas crudas, y un 0 se marca "saturado o negativo" (nunca es una
            // lectura buena de verdad: SerieDisparos/RF-USR-04 sólo pone media=0 y guarda un 0 cuando
            // el equipo satura o da negativo).
            tvResultado.setText(getString(R.string.medir_resultado, fila.color, lecturasCrudasTexto(fila),
                    formatoValor(fila.media), formatoValor(fila.minimo), fila.valido ? "SI" : "NO"));
        }
        actualizarContador();
    }

    private static String lecturasCrudasTexto(FilaMedida fila) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fila.lecturas.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(formatoValor(fila.lecturas.get(i)));
        }
        return sb.toString();
    }

    /** M3: "0 (saturado o negativo)"; cualquier otro valor, tal cual. */
    private static String formatoValor(int v) {
        return v == 0 ? "0 (saturado o negativo)" : String.valueOf(v);
    }

    private void actualizarContador() {
        SesionMedicion sesion = SesionHolder.sesion();
        int n = sesion == null ? 0 : sesion.filas().size();
        tvContador.setText(getString(R.string.medir_filas_guardadas, n));
    }

    /**
     * A4 (ALTO): API 29+ usa {@code MediaStore.Downloads} (almacenamiento con ámbito); API 24-28
     * pide {@code WRITE_EXTERNAL_STORAGE} en tiempo de ejecución y escribe un fichero directo.
     * Cualquier excepción del hilo de exportar se captura y se muestra en pantalla (antes, una
     * excepción aquí tumbaba el proceso entero: un {@link Thread} sin manejador propio no la
     * atrapa).
     */
    private void exportar() {
        SesionMedicion sesion = SesionHolder.sesion();
        if (sesion == null) {
            return;
        }
        if (EstrategiaExportacion.paraApi(Build.VERSION.SDK_INT) == EstrategiaExportacion.Via.ARCHIVO_DIRECTO
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, PETICION_PERMISO_EXPORTAR);
            tvResultado.setText(R.string.exportar_permiso_necesario);
            return;
        }
        establecerControlesEnCurso(true);
        new Thread(() -> {
            try {
                String destinoTexto = exportarSegunApi(sesion);
                // B-4 (condición QA-8): avisa si el Diario descartó alguna línea FILA cortada (B-1);
                // filasCortadasIgnoradas() sólo es fiable tras exportar/filas(), que ya releyó el
                // fichero (SesionMedicion#filas -> Diario#filasGuardadas).
                int cortadas = sesion.filasCortadasIgnoradas().size();
                String mensaje = cortadas == 0 ? getString(R.string.medir_exportado, destinoTexto)
                        : getString(R.string.medir_exportado_con_filas_cortadas, destinoTexto, cortadas);
                runOnUiThread(() -> {
                    tvResultado.setText(mensaje);
                    establecerControlesEnCurso(false);
                });
            } catch (Exception e) {
                String mensaje = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> {
                    tvResultado.setText(getString(R.string.exportar_error, mensaje));
                    establecerControlesEnCurso(false);
                });
            }
        }, "exportar").start();
    }

    private String exportarSegunApi(SesionMedicion sesion) throws IOException {
        if (EstrategiaExportacion.paraApi(Build.VERSION.SDK_INT) == EstrategiaExportacion.Via.MEDIA_STORE) {
            Uri uri = ExportadorAndroid.exportarMediaStore(getApplicationContext(), sesion, new Date());
            return uri.toString();
        }
        File carpeta = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "RetroUsuario");
        File zip = sesion.exportar(carpeta, new Date(), TimeZone.getDefault());
        return zip.getAbsolutePath();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PETICION_PERMISO_EXPORTAR) {
            boolean concedido = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            tvResultado.setText(concedido ? R.string.exportar_permiso_concedido : R.string.exportar_permiso_denegado);
        }
        // arq C2/RF-USR-15: PETICION_PERMISO_UBICACION no lleva reacción propia, concedido o no — se
        // mide igual (con o sin posición), sin bloquear ni avisar; UbicacionGps ya distingue el
        // resultado (gps_estado) fila a fila.
    }

    /** ISO 8601 con zona (RF-USR-06, M-6): "XXX" da el offset con dos puntos, p. ej. "-05:00" (disponible desde API 24). */
    private static String fechaHoraIsoAhora() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT);
        return f.format(new Date());
    }
}
