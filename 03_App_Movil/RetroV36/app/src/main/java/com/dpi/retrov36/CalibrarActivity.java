package com.dpi.retrov36;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Calibrar este equipo" (3.6.13). Solo pantalla: toda la logica esta en {@link FlujoCalibracion}, que
 * las pruebas JVM recorren contra un equipo simulado. Aqui se pintan las previas, las tarjetas de la
 * tabla RF-CAL-37 y el acta, y cada boton llama a una accion del flujo en el hilo de trabajo.
 * El PIN se pide una vez por conexion y el nombre del superadministrador se recuerda (QA-3612-14).
 */
public class CalibrarActivity extends Base {

    private static final String PREFS = "rtv";
    private static final String PREF_NOMBRE = "superadministrador";

    private FlujoCalibracion flujo;
    private final FlujoCalibracion.Contexto ctx = new FlujoCalibracion.Contexto();
    private TextView txtPrevias;
    private LinearLayout tarjetas;
    private EditText edNombre;
    private EditText edNota;
    private Button btnCalibrar;
    private Button btnBateria;
    private Button btnPersistencia;
    private Button btnAceptar;
    private Button btnRechazar;
    private TextView txtProgreso;
    private TextView txtActa;
    private final Map<Character, CheckBox> casillas = new LinkedHashMap<>();
    private volatile boolean ocupado;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        titulo("Comprobaciones previas");
        txtPrevias = texto("");
        titulo("Códigos (tabla RF-CAL-37 del APK)");
        tarjetas = new LinearLayout(this);
        tarjetas.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(tarjetas);
        edNombre = campo("Nombre del superadministrador (se recuerda)", InputType.TYPE_CLASS_TEXT);
        edNombre.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_NOMBRE, ""));
        edNota = campo("Nota de la conformidad", InputType.TYPE_CLASS_TEXT);
        btnCalibrar = boton("Calibrar", v -> accion("Calibrar", this::calibrar, true));
        btnBateria = boton("Leer batería (9)", v -> accion("Batería", () -> flujo.leerBateria(), false));
        fila(btnCalibrar, btnBateria);
        txtProgreso = texto("");
        txtProgreso.setTypeface(Typeface.MONOSPACE);
        btnPersistencia = boton("Persistencia: apagar y encender el equipo", v -> accion("Persistencia",
                () -> flujo.persistencia(), true));
        btnAceptar = boton("Aceptar y grabar fecha", v -> confirmarAceptar());
        btnRechazar = boton("Rechazar", v -> rechazar());
        titulo("Acta");
        txtActa = texto("");
        txtActa.setTypeface(Typeface.MONOSPACE);
        construir();
    }

    // ------------------------------------------------------------ construccion

    private void refrescarContexto() {
        Sesion s = Sesion.get();
        ctx.conectado = EnlaceSerie.instancia().estaConectado();
        ctx.es362 = s.es362();
        ctx.firmware = s.firmware();
        ctx.apto = s.apto;
        ctx.resumenPruebas = s.resumenPruebas;
        ctx.serieGN = s.serieEquipo;
        ctx.nombreBT = s.nombre;
        ctx.mac = s.mac;
        ctx.app = "RTV " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
    }

    private String asset(String n) {
        try (InputStream in = getAssets().open(n)) {
            return new String(ImportadorCampana.leer(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private void construir() {
        refrescarContexto();
        Sesion s = Sesion.get();
        BancoCola cola = null;
        try (InputStream in = getAssets().open(BancoCola.ASSET)) {
            cola = BancoCola.cargar(ImportadorCampana.leer(in));
        } catch (IOException | RuntimeException e) {
            Registro.nota("calibrar: cola no admitida: " + e.getMessage());
        }
        Campana campana = null;
        List<Patron> cat = null;
        try {
            cat = s.patrones(this);
            if (s.mac != null && !s.mac.isEmpty() && s.serieConocida()) {
                campana = Campanas.abrir(this, s.serie(), s.mac);
            }
        } catch (IOException | RuntimeException e) {
            alerta("Campaña", "No se pudo abrir: " + e.getMessage());
        }
        final CalibrarActivity yo = this;
        FlujoCalibracion.AlmacenActa almacen = new FlujoCalibracion.AlmacenActa() {
            @Override
            public Acta enCurso() throws IOException {
                return Sesion.get().mac == null ? null : Campanas.actaEnCurso(yo);
            }

            @Override
            public void adjuntar(Acta a) throws IOException {
                Campanas.adjuntarActa(yo, a);
            }

            @Override
            public void cerrar(Acta a) throws IOException {
                Campanas.cerrarActaEnDisco(yo, a);
            }
        };
        FlujoCalibracion.Reloj reloj = new FlujoCalibracion.Reloj() {
            @Override
            public String ahoraIso() {
                return Sesion.ahoraIso();
            }

            @Override
            public String hoy() {
                return Calibracion.hoy();
            }
        };
        try {
            flujo = new FlujoCalibracion(Cliente.instancia(), operador(), almacen, cola, campana, cat,
                    Decisiones.leer(asset(Decisiones.ASSET)), ctx, reloj);
            flujo.pin(s.pinAdmin);
        } catch (IOException | RuntimeException e) {
            alerta("Acta", "No se pudo leer el acta en curso: " + e.getMessage());
        }
        pintar();
    }

    private FlujoCalibracion.Operador operador() {
        return new FlujoCalibracion.Operador() {
            @Override
            public int preguntar(String titulo, String mensaje, String... op) throws InterruptedException {
                return CalibrarActivity.this.preguntar(titulo, mensaje, op[0], op.length > 1 ? op[1] : null,
                        op.length > 2 ? op[2] : null);
            }

            @Override
            public void progreso(String texto) {
                Registro.nota("calibrar: " + texto);
                enUi(() -> txtProgreso.setText(texto));
            }

            @Override
            public boolean apagarYEncender() throws IOException, InterruptedException {
                if (CalibrarActivity.this.preguntar("Persistencia", "Apague el equipo, espere 5 s y enciéndalo. Pulse OK "
                        + "cuando esté encendido (la app vuelve a conectar sola).", "OK", "Cancelar", null) != 0) {
                    return false;
                }
                Sesion s = Sesion.get();
                EnlaceSerie en = EnlaceSerie.instancia();
                long limite = SystemClock.elapsedRealtime() + 45000;
                while (!en.estaConectado() && SystemClock.elapsedRealtime() < limite) {
                    if (!en.estaConectando()) {
                        BluetoothAdapter ad = EnlaceSerie.adaptador();
                        if (ad != null) {
                            en.conectar(CalibrarActivity.this, ad.getRemoteDevice(s.mac));
                        }
                    }
                    Thread.sleep(1000);
                }
                if (!en.estaConectado()) {
                    return false;
                }
                Thread.sleep(2500);   // margen tras el arranque del equipo: provisional, sin medir
                return true;
            }
        };
    }

    // ------------------------------------------------------------------ pintar

    private Set<Character> seleccion() {
        Set<Character> s = new HashSet<>();
        for (Map.Entry<Character, CheckBox> e : casillas.entrySet()) {
            if (e.getValue().isChecked() && e.getValue().isEnabled()) {
                s.add(e.getKey());
            }
        }
        return s;
    }

    private void pintar() {
        refrescarContexto();
        if (flujo == null) {
            txtPrevias.setText("No se pudo preparar el flujo.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean ok = true;
        for (FlujoCalibracion.Previa p : flujo.previas()) {
            sb.append(p.ok ? "OK  " : "NO  ").append(p.texto).append('\n');
            ok &= p.ok;
        }
        txtPrevias.setText(sb.toString());
        txtPrevias.setBackgroundColor(ok ? VERDE : ROJO);
        Set<Character> antes = seleccion();
        tarjetas.removeAllViews();
        casillas.clear();
        Map<Character, Ecuacion> vig = new LinkedHashMap<>();
        for (char k : Fabrica.CODIGOS) {
            vig.put(k, Sesion.get().ecuacionVigente(k));
        }
        for (FlujoCalibracion.Tarjeta t : flujo.tarjetas(vig)) {
            TextView tv = new TextView(this);
            tv.setPadding(dp(6), dp(8), dp(6), dp(2));
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setText(t.texto);
            tv.setBackgroundColor(t.casilla ? GRIS : AMARILLO);
            tarjetas.addView(tv);
            if (t.casilla) {
                CheckBox cb = new CheckBox(this);
                cb.setText(t.textoCasilla);
                cb.setEnabled(!ocupado);
                cb.setChecked(antes.contains(t.k));
                cb.setOnCheckedChangeListener((v, c) -> pintarBotones());
                tarjetas.addView(cb);
                casillas.put(t.k, cb);
            }
        }
        pintarBotones();
        Acta a = flujo.acta();
        txtActa.setText(a == null ? "Sin acta en curso." : a.texto() + "\nPara aceptar: "
                + (a.motivoNoAceptableSalvoVerificacionFinal() == null ? "listo (la verificación final se hace al pulsar)"
                : a.motivoNoAceptableSalvoVerificacionFinal()));
    }

    private void pintarBotones() {
        boolean libre = !ocupado && flujo != null;
        Set<Character> sel = seleccion();
        btnCalibrar.setEnabled(libre && flujo.puedeCalibrar(sel));
        btnCalibrar.setText(sel.isEmpty() && libre && flujo.pendientes() > 0 ? "Continuar la calibración a medias" : "Calibrar");
        btnBateria.setEnabled(libre && ctx.conectado);
        btnPersistencia.setEnabled(libre && flujo.puedePersistencia());
        btnAceptar.setEnabled(libre && flujo.puedeAceptar());
        btnRechazar.setEnabled(libre && flujo.puedeRechazar());
    }

    // ---------------------------------------------------------------- acciones

    private interface Accion {
        String hacer() throws IOException, InterruptedException;
    }

    private String calibrar() throws IOException, InterruptedException {
        return flujo.calibrar(seleccionAlPulsar, edNombreAlPulsar, edNotaAlPulsar);
    }

    private Set<Character> seleccionAlPulsar = new HashSet<>();
    private String edNombreAlPulsar = "";
    private String edNotaAlPulsar = "";

    /** Ejecuta una accion del flujo en el hilo de trabajo. conPin: pide el PIN si no se tiene. */
    private void accion(String titulo, Accion a, boolean conPin) {
        if (flujo == null) {
            return;
        }
        if (conPin && Sesion.get().pinAdmin == null) {
            final EditText e = campoPin();
            new AlertDialog.Builder(this).setTitle("PIN del equipo (una vez por conexión)").setView(e)
                    .setPositiveButton("Seguir", (d, w) -> {
                        Sesion.get().pinAdmin = e.getText().toString().trim();
                        flujo.pin(Sesion.get().pinAdmin);
                        accion(titulo, a, false);
                    })
                    .setNegativeButton("Cancelar", null).show();
            return;
        }
        seleccionAlPulsar = seleccion();
        edNombreAlPulsar = edNombre.getText().toString().trim();
        edNotaAlPulsar = edNota.getText().toString().trim();
        if (!edNombreAlPulsar.isEmpty()) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_NOMBRE, edNombreAlPulsar).apply();
        }
        ocupado = true;
        pantallaEncendida(true);
        pintarBotones();
        Cliente.instancia().ejecutar(() -> {
            String fin;
            try {
                fin = a.hacer();
            } catch (IOException | InterruptedException | RuntimeException e) {
                fin = titulo + " interrumpido: " + EnlaceSerie.descripcion(e) + ". El acta queda en disco; al volver se retoma.";
            }
            if (fin != null && fin.startsWith("PIN rechazado")) {
                Sesion.get().pinAdmin = null;
            }
            final String f = fin;
            Registro.nota("calibrar (" + titulo + "): " + f);
            enUi(() -> {
                ocupado = false;
                pantallaEncendida(false);
                txtProgreso.setText(f);
                if ("Calibrar".equals(titulo)) {
                    // QA-3612-04: las casillas no siguen marcadas despues del flujo.
                    for (CheckBox c : casillas.values()) {
                        c.setChecked(false);
                    }
                }
                if (!isFinishing() && !isDestroyed()) {
                    pintar();
                }
            });
        });
    }

    private void confirmarAceptar() {
        new AlertDialog.Builder(this).setTitle("Aceptar el acta")
                .setMessage("Se releen #V# y #G (los códigos del acta y los heredados) y la batería; si todo coincide, se "
                        + "graba la fecha de hoy (#SC) una sola vez.")
                .setPositiveButton("Aceptar y grabar fecha", (d, w) -> accion("Aceptar", () -> flujo.aceptar(), true))
                .setNegativeButton("Cancelar", null).show();
    }

    private void rechazar() {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        final EditText e = new EditText(this);
        e.setHint("Motivo");
        caja.addView(e);
        final CheckBox rest = new CheckBox(this);
        rest.setText("Restaurar lo escrito a su curva anterior (verificado con #G)");
        rest.setChecked(true);
        caja.addView(rest);
        new AlertDialog.Builder(this).setTitle("Rechazar el acta").setView(caja)
                .setMessage("No se grabará fecha. Si no restaura, lo escrito sigue en el equipo.")
                .setPositiveButton("Rechazar", (d, w) -> accion("Rechazar",
                        () -> flujo.rechazar(e.getText().toString().trim(), rest.isChecked()), rest.isChecked()))
                .setNegativeButton("Cancelar", null).show();
    }

    /** Mientras el flujo escribe o re-mide, Atras no sale. */
    @Override
    public void onBackPressed() {
        if (ocupado) {
            alerta("Calibración en curso", "Espere a que termine el paso en curso o pulse \"Parar aquí\" en la "
                    + "re-medida. Lo hecho queda en el acta en disco y se retoma al volver.");
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (btnCalibrar != null && !ocupado && flujo != null) {
            pintar();
        }
    }
}
