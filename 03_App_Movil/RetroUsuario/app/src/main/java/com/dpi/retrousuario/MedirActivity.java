package com.dpi.retrousuario;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dpi.retrousuario.dominio.CsvMedidas;
import com.dpi.retrousuario.dominio.EstadoMedida;
import com.dpi.retrousuario.dominio.EstrategiaExportacion;
import com.dpi.retrousuario.dominio.FilaMedida;
import com.dpi.retrousuario.dominio.PermisoUbicacion;
import com.dpi.retrousuario.dominio.PreguntaOperador;
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
 *
 * <p><b>FABLE-USR, revisor Fable opción A sobre 0.3.5 (SPEC-App-Usuario-V3.6.md:617-625):</b> hasta la 0.3.5 esta Activity era
 * la DUEÑA de la operación "medir" — el hilo de fondo capturaba {@code this} (una cola local
 * alimentada por un diálogo sobre {@code this}, y {@code runOnUiThread} sobre {@code this} para pintar
 * el resultado): un giro de pantalla a mitad de la pregunta "Repetir o Saltar" perdía el diálogo del
 * sistema y dejaba el hilo de fondo bloqueado para siempre, y la Activity nueva nacía con los botones
 * habilitados aunque hubiera una medida en curso (su {@code onResume} no sabía nada de ella). Ahora la
 * dueña es {@link EstadoMedida} (dominio puro, {@code SesionHolder.medida()}, mismo patrón que
 * {@link com.dpi.retrousuario.dominio.EstadoDeteccion} para la detección): el hilo de medir sólo
 * referencia {@link SesionHolder#medida()} y {@link #getApplicationContext()}, nunca {@code this}; esta
 * Activity sólo PINTA lo que {@link EstadoMedida} ya sabe, en {@link #restaurarInterfaz()} — llamado en
 * {@code onResume()} y, mientras la Activity está viva, cuando {@link EstadoMedida} avisa (vía
 * {@link Handler#post}, nunca directamente desde el hilo de fondo).</p>
 */
public final class MedirActivity extends AppCompatActivity {

    private static final int PETICION_PERMISO_EXPORTAR = 100;
    private static final int PETICION_PERMISO_UBICACION = 101;

    private TextView tvResultado;
    private TextView tvContador;
    private Button[] botonesColor;
    private Button btnAjustes;
    private Button btnExportar;

    /** FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625): salta del hilo de fondo (donde vive el oyente de {@link EstadoMedida}) al
     *  hilo principal antes de tocar cualquier vista — mismo patrón que {@code MainActivity}. */
    private final Handler handlerPrincipal = new Handler(Looper.getMainLooper());
    /** Instancia estable: {@link EstadoMedida#quitarOyente} compara por referencia. */
    private final com.dpi.retrousuario.dominio.Oyente oyenteMedida = () -> handlerPrincipal.post(this::restaurarInterfaz);
    /** Un solo diálogo vivo a la vez (FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625)): se cierra en {@link #onPause()} para que un
     *  giro de pantalla no deje dos diálogos del sistema apilados (uno de la Activity vieja, otro de
     *  la nueva) — la pregunta sigue pendiente en {@link EstadoMedida}, la Activity nueva la re-muestra
     *  en su propio {@link #onResume()}. */
    private AlertDialog dialogoPregunta;

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

    /** FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625): el oyente se registra en {@code onResume()} (retirado en {@link #onPause()},
     *  mismo patrón que {@code MainActivity}/{@code EstadoDeteccion}) — así una medida que termina, o
     *  que queda esperando una pregunta, mientras esta Activity está en segundo plano se repinta en
     *  cuanto vuelve a primer plano, sin depender de que el hilo de fondo siga vivo para avisar. */
    @Override
    protected void onResume() {
        super.onResume();
        SesionHolder.medida().registrarOyente(oyenteMedida);
        restaurarInterfaz();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SesionHolder.medida().quitarOyente(oyenteMedida);
        if (dialogoPregunta != null) {
            dialogoPregunta.dismiss();
            dialogoPregunta = null;
        }
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
     * Cero tecleo (RF-USR-06): color → Medir, sin más entrada del operador. FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625): el hilo
     * de fondo sólo referencia {@link SesionHolder#medida()} y {@link #getApplicationContext()} — ni
     * {@code this} ni ninguna vista; {@link EstadoMedida#ejecutar} captura cualquier excepción de
     * {@code sesion.medir()} (B-1 de la 0.3.0) y la publica, {@link #restaurarInterfaz()} la pinta.
     */
    private void medir(String color) {
        SesionMedicion sesion = SesionHolder.sesion();
        if (sesion == null) {
            tvResultado.setText(R.string.medir_sin_equipo);
            return;
        }
        tvResultado.setText(R.string.medir_midiendo);
        establecerControlesEnCurso(true);
        Context contexto = getApplicationContext();
        new Thread(() -> {
            UbicacionGps.Resultado gps = UbicacionGps.ultimaConocida(contexto);
            String fechaHora = fechaHoraIsoAhora();
            boolean conPosicion = "con_posicion".equals(gps.gpsEstado);
            String lat = conPosicion ? CsvMedidas.formatearCoordenada(gps.latitud) : "";
            String lon = conPosicion ? CsvMedidas.formatearCoordenada(gps.longitud) : "";
            SesionHolder.medida().ejecutar(sesion, color, fechaHora, lat, lon, gps.gpsEstado);
        }, "medir-" + color).start();
    }

    /**
     * FABLE-USR (SPEC-App-Usuario-V3.6.md:617-625): pinta lo que {@link EstadoMedida} ya sabe — controles deshabilitados mientras
     * MIDIENDO o PREGUNTANDO, el diálogo "Repetir o Saltar" re-mostrado si hay una pregunta pendiente
     * (tras un giro, o si esta Activity vuelve de segundo plano con una pregunta que dejó pendiente),
     * y el resultado (o el error) de una medida terminada, una sola vez. Atrás desde esta pantalla NO
     * interrumpe la medida: no hay ningún camino aquí que llame a {@link EstadoMedida#abandonar()} (lo
     * hace {@code SesionHolder.limpiar()}, al cambiar de equipo o salir de verdad).
     */
    private void restaurarInterfaz() {
        EstadoMedida medida = SesionHolder.medida();
        EstadoMedida.Fase fase = medida.fase();
        establecerControlesEnCurso(fase != EstadoMedida.Fase.LIBRE);

        EstadoMedida.TipoPregunta pregunta = medida.preguntaPendiente();
        if (pregunta != null) {
            mostrarDialogoPregunta(pregunta);
            actualizarContador();
            return;
        }
        if (dialogoPregunta != null) {
            dialogoPregunta.dismiss();
            dialogoPregunta = null;
        }

        EstadoMedida.Resultado resultado = medida.recogerResultadoPendiente();
        if (resultado != null) {
            mostrarResultado(resultado.fila());
            actualizarContador();
            return;
        }
        String error = medida.recogerErrorPendiente();
        if (error != null) {
            tvResultado.setText(getString(R.string.medir_error, error));
        }
        actualizarContador();
    }

    /** Un solo diálogo vivo (ver el Javadoc de {@link #dialogoPregunta}): si ya hay uno mostrado para
     *  esta Activity, no crea otro encima. */
    private void mostrarDialogoPregunta(EstadoMedida.TipoPregunta tipo) {
        if (dialogoPregunta != null) {
            return;
        }
        int mensajeResId = tipo == EstadoMedida.TipoPregunta.CERO
                ? R.string.medir_pregunta_cero : R.string.medir_pregunta_disparo_anulado;
        dialogoPregunta = new AlertDialog.Builder(this)
                .setMessage(mensajeResId)
                .setCancelable(false)
                .setPositiveButton(R.string.medir_repetir, (d, w) -> {
                    dialogoPregunta = null;
                    SesionHolder.medida().responder(PreguntaOperador.Decision.REPETIR);
                })
                .setNegativeButton(R.string.medir_saltar, (d, w) -> {
                    dialogoPregunta = null;
                    SesionHolder.medida().responder(PreguntaOperador.Decision.SALTAR);
                })
                .show();
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
