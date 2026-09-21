package com.dpi.retrousuario;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dpi.retrousuario.dominio.Canal;
import com.dpi.retrousuario.dominio.CanalRegistrado;
import com.dpi.retrousuario.dominio.DetectorEquipo;
import com.dpi.retrousuario.dominio.Diario;
import com.dpi.retrousuario.dominio.EstadoCalibracion;
import com.dpi.retrousuario.dominio.FechaISO;
import com.dpi.retrousuario.dominio.RegistroTramas;
import com.dpi.retrousuario.dominio.RespuestaV;
import com.dpi.retrousuario.dominio.SesionMedicion;
import com.dpi.retrousuario.dominio.Sonda362;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Pantallas 1 y 2 (SPEC-App-Usuario-V3.6.md §1): aviso antes de conectar, lista de equipos
 * emparejados, detección (RF-USR-01) y estado de calibración (RF-USR-02). Al terminar la sonda crea
 * la {@link SesionMedicion} (con lo ya leído: serie, MAC, calibración) y ofrece "Medir" hacia
 * {@link MedirActivity}. **Sin pantalla 3 propia** ("elegir modo", SPEC §1): en el incremento 1 el
 * único modo entregado es "Medir y exportar" (RF-USR-05), así que no hay nada que elegir todavía; la
 * pantalla se añade cuando exista una segunda opción real ("Señal a señal", incremento 2).
 *
 * Esta Activity sólo pinta y llama al dominio (CLAUDE.md, rules/modularidad.md "Capas"): toda la
 * decisión de RF-USR-01/02 vive en {@code dominio.*}, probado en la JVM.
 */
public final class MainActivity extends AppCompatActivity {

    private TextView tvEstado;
    private ListView lvEquipos;
    private Button btnReintentar;
    private Button btnContinuar;
    /** M7: leído/escrito desde el hilo de interfaz y desde el hilo de detección/sonda. */
    private volatile EnlaceBluetooth enlace;
    private BluetoothDevice equipoActual;
    /** M7: "una sola detección a la vez" — una segunda pulsación mientras hay una en curso se ignora. */
    private volatile boolean detectando = false;

    /** M2: UN registro y UN diario para toda la vida del proceso (fichero, no memoria): así una
     *  reconexión o una Activity recreada no pierde lo acumulado hasta que se exporte. */
    private RegistroTramas log;
    private Diario diario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvEstado = findViewById(R.id.tvEstado);
        lvEquipos = findViewById(R.id.lvEquipos);
        btnReintentar = findViewById(R.id.btnReintentar);
        btnReintentar.setOnClickListener(v -> conectarYDetectar(equipoActual));
        btnContinuar = findViewById(R.id.btnContinuar);
        btnContinuar.setOnClickListener(v -> startActivity(new Intent(this, MedirActivity.class)));

        SesionHolder.cargarAjustesPersistidos(getApplicationContext()); // B3.
        log = new RegistroTramas(new File(getFilesDir(), "tramas.log"));
        diario = new Diario(new File(getFilesDir(), "diario_medidas.txt"));

        mostrarAvisoPrevio();
    }

    /** RF-USR-01: el aviso tiene que estar visible antes de cualquier trama (T-USR-02). */
    private void mostrarAvisoPrevio() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.aviso_previo)
                .setCancelable(false)
                .setPositiveButton(R.string.entendido, (dialog, which) -> poblarListaEquipos())
                .show();
    }

    private void poblarListaEquipos() {
        BluetoothAdapter adaptador = BluetoothAdapter.getDefaultAdapter();
        List<BluetoothDevice> emparejados = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();
        if (adaptador != null) {
            try {
                for (BluetoothDevice d : adaptador.getBondedDevices()) {
                    emparejados.add(d);
                    etiquetas.add(d.getName() + " (" + d.getAddress() + ")");
                }
            } catch (SecurityException e) {
                tvEstado.setText("Sin permiso para leer los equipos emparejados");
                return;
            }
        }
        lvEquipos.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, etiquetas));
        lvEquipos.setOnItemClickListener((parent, view, position, id) -> conectarYDetectar(emparejados.get(position)));
    }

    /**
     * A2 (ALTO): al elegir OTRO equipo, desconecta y anula el enlace/sesión anteriores antes de
     * conectar al nuevo — nunca reutiliza un enlace abierto para un dispositivo distinto. Reintentar
     * sobre el MISMO equipo (botón "Reintentar") sí reutiliza el enlace si sigue abierto: sólo repite
     * la sonda, no la conexión SPP entera.
     */
    private void conectarYDetectar(BluetoothDevice dispositivo) {
        if (dispositivo == null || detectando) {
            return; // M7: una sola detección a la vez; una segunda pulsación mientras hay una en curso se ignora.
        }
        boolean cambioDeEquipo = equipoActual != null && !dispositivo.getAddress().equals(equipoActual.getAddress());
        if (cambioDeEquipo && enlace != null) {
            enlace.desconectar();
            enlace = null;
            SesionHolder.limpiar(); // A2: la sesión y el enlace del equipo anterior dejan de estar disponibles.
        }
        equipoActual = dispositivo;
        detectando = true;
        btnReintentar.setVisibility(View.GONE);
        btnContinuar.setVisibility(View.GONE);
        lvEquipos.setEnabled(false);
        tvEstado.setText(R.string.conectando);
        new Thread(() -> {
            try {
                EnlaceBluetooth enlaceLocal = enlace;
                if (enlaceLocal == null) {
                    enlaceLocal = EnlaceBluetooth.conectar(dispositivo);
                    enlace = enlaceLocal;
                }
                Canal canalRegistrado = new CanalRegistrado(enlaceLocal, log, enlaceLocal); // M2.
                DetectorEquipo.ResultadoDeteccion deteccion =
                        DetectorEquipo.detectar(canalRegistrado, SesionHolder.parametros());
                runOnUiThread(() -> mostrarResultadoDeteccion(deteccion, canalRegistrado));
            } catch (Exception e) {
                detectando = false;
                runOnUiThread(() -> {
                    tvEstado.setText("No se pudo conectar: " + e.getMessage());
                    lvEquipos.setEnabled(true);
                });
            }
        }, "deteccion-usuario").start();
    }

    private void mostrarResultadoDeteccion(DetectorEquipo.ResultadoDeteccion deteccion, Canal canalRegistrado) {
        if (deteccion.resultado() == DetectorEquipo.Resultado.NO_COMPATIBLE) {
            detectando = false;
            tvEstado.setText(R.string.equipo_no_compatible);
            lvEquipos.setEnabled(true); // A2: tras "no compatible", elegir otro sin reiniciar.
            return;
        }
        new Thread(() -> {
            Sonda362.ResultadoSonda sonda = Sonda362.sondear(canalRegistrado, SesionHolder.parametros());
            detectando = false;
            runOnUiThread(() -> mostrarResultadoSonda(deteccion.respuestaV(), sonda));
        }, "sonda-362-usuario").start();
    }

    private void mostrarResultadoSonda(RespuestaV v, Sonda362.ResultadoSonda sonda) {
        lvEquipos.setEnabled(true);
        switch (sonda.resultado()) {
            case ACTUALIZAR_FIRMWARE:
                tvEstado.setText(R.string.actualice_firmware);
                return;
            case SIN_RESPUESTA_REINTENTAR:
                tvEstado.setText(R.string.sin_respuesta_reintente);
                btnReintentar.setVisibility(View.VISIBLE);
                return;
            default:
                mostrarEstadoCalibracion(v, sonda);
        }
    }

    private void mostrarEstadoCalibracion(RespuestaV v, Sonda362.ResultadoSonda sonda) {
        FechaISO hoy = hoyDelDispositivo();
        String fechaGC = sonda.fechaRegistrada() ? sonda.fechaCalibracion() : "NONE";
        EstadoCalibracion estado = EstadoCalibracion.calcular(v.estadoAjuste(), fechaGC, hoy);
        String serie = sonda.serieLeida() ? sonda.serie() : "SIN SERIE";
        String texto = "Serie: " + serie + "\n" + (estado.texto().isEmpty() ? "Calibración vigente" : estado.texto());
        tvEstado.setText(texto);

        // RF-USR-04, RF-USR-05, RF-USR-06: la sesion de medir se crea aqui, una vez, con lo que ya sondeo esta pantalla.
        SesionMedicion sesion = new SesionMedicion(enlace, SesionHolder.parametros(), log, diario);
        // A2: la MAC sale del dispositivo REALMENTE conectado (el socket), no sólo del último elegido en la lista.
        String mac = enlace != null ? enlace.macConectada() : "";
        sesion.registrarEquipo(mac, v.crudo(), sonda.serie(), sonda.serieLeida(), // D-1/M1: texto crudo de "#V#", sin reconstruir.
                v.estadoAjuste(), fechaGC, sonda.fechaRegistrada(), hoy);
        SesionHolder.establecer(sesion, enlace);
        btnContinuar.setVisibility(View.VISIBLE);
    }

    private static FechaISO hoyDelDispositivo() {
        Calendar c = Calendar.getInstance();
        return FechaISO.de(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * M7 (revisión P16 de RetroUsuario 0.2.0): si la sesión sigue viva ({@link SesionHolder} ya tiene
     * ESTE enlace, porque {@link MedirActivity} lo está usando), no se cierra aquí — cerrarlo
     * rompería el socket Bluetooth de una sesión que sigue en curso (p. ej. al recrear esta Activity
     * por un giro de pantalla). Sólo se cierra un enlace huérfano: uno que nunca llegó a entregarse a
     * una sesión, o que ya fue reemplazado (A2, cambio de equipo).
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        EnlaceBluetooth actual = enlace;
        if (actual != null && actual != SesionHolder.enlace()) {
            actual.desconectar();
        }
    }
}
