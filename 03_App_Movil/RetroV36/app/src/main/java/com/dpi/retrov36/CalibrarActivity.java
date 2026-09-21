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
    private Button btnTodo;
    private Button btnBateria;
    private Button btnPersistencia;
    private Button btnAceptar;
    private Button btnRechazar;
    private Button btnCerrar;
    /** Ultimos ZIP exportados en esta accion: se comparten juntos al final (un solo selector). */
    /** 3.6.17 (F-05): los ZIP ligeros de todas las actas aceptadas en esta accion (son incrementales). */
    private final java.util.List<Campanas.Exportacion> ligeros = new java.util.ArrayList<>();
    /** Actas aceptadas en esta accion: van al informe de calibracion (3.6.17). */
    private final java.util.List<Acta> aceptadasAccion = new java.util.ArrayList<>();
    private volatile Campanas.Exportacion ultimoSoporte;
    private Button btnLiberar;
    private volatile String errorZip;
    private TextView txtProgreso;
    private TextView txtActa;
    private final Map<Character, CheckBox> casillas = new LinkedHashMap<>();
    private volatile boolean ocupado;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        titulo("Comprobaciones previas");
        txtPrevias = texto("");
        // RF-COV-17: la app de calibrar no ensena codigos ni tarjetas por codigo (el operador ni los ve ni
        // los elige); tarjetas se queda sin usar (pintar() no la rellena en CORTO) y sin vista propia.
        tarjetas = new LinearLayout(this);
        tarjetas.setOrientation(LinearLayout.VERTICAL);
        if (!BuildConfig.CORTO) {
            titulo("Códigos (tabla RF-CAL-37 del APK)");
            raiz.addView(tarjetas);
        }
        edNombre = campo(BuildConfig.CORTO ? "Nombre de quien calibra (se recuerda)"
                : "Nombre del superadministrador (se recuerda)", InputType.TYPE_CLASS_TEXT);
        edNombre.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_NOMBRE, ""));
        edNota = campo("Nota de la conformidad", InputType.TYPE_CLASS_TEXT);
        edNombre.setSingleLine(true);
        edNota.setSingleLine(true);
        if (BuildConfig.CORTO) {
            // RF-COV-17: sin casillas por código y sin "Calibrar todo"/"Continuar un código": un solo botón que
            // calibra sola toda la sesión, en el orden de TablaCalibracion, con el nombre tecleado una vez. La
            // nota de conformidad no la pide esta app (RF-COV-17 solo menciona el nombre).
            edNota.setVisibility(android.view.View.GONE);
            btnCalibrar = boton("Calibrar", v -> accion("Calibrar", this::calibrarAutomatico, true));
            btnBateria = boton("Leer batería (9)", v -> accion("Batería", () -> flujo.leerBateria(), false));
            fila(btnCalibrar, btnBateria);
        } else {
            // 3.6.15: una sola sesion. "Calibrar todo" escribe, re-mide, hace la persistencia y acepta el acta de
            // cada codigo marcado, en orden (8, b, 5, 3, 4, 6), y sigue solo con el siguiente.
            btnTodo = boton("Calibrar todo (8 → b → 5 …)", v -> accion("Calibrar", this::calibrarTodo, true));
            btnCalibrar = boton("Continuar / un código", v -> accion("Calibrar", this::calibrar, true));
            btnBateria = boton("Leer batería (9)", v -> accion("Batería", () -> flujo.leerBateria(), false));
            fila(btnCalibrar, btnBateria);
        }
        txtProgreso = texto("");
        txtProgreso.setTypeface(Typeface.MONOSPACE);
        btnPersistencia = boton("Persistencia: apagar y encender el equipo", v -> accion("Persistencia",
                () -> flujo.persistencia(), true));
        btnAceptar = boton("Aceptar y grabar fecha", v -> confirmarAceptar());
        btnRechazar = boton("Rechazar", v -> rechazar());
        btnCerrar = boton("Cerrar sin restaurar (PIN de administrador)", v -> cerrarSinRestaurar());
        btnLiberar = boton("Liberar tras el cierre sin restaurar (PIN de administrador)", v -> liberar());
        if (BuildConfig.CORTO) {
            // RF-COV-17: "Calibrar" ya escribe, re-mide, hace la persistencia y acepta sola; estos dos botones
            // quedan sin uso en el camino normal. Se dejan Rechazar/Cerrar sin restaurar/Liberar, que son la
            // valvula de seguridad para una sesion que quedo a medias (rechazo pendiente, cierre sin restaurar).
            btnPersistencia.setVisibility(android.view.View.GONE);
            btnAceptar.setVisibility(android.view.View.GONE);
        }
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
        ctx.protocolo = s.protocolo != null ? s.protocolo : new ProtocoloV36();
        ctx.firmware = s.firmware();
        ctx.apto = s.apto;
        ctx.resumenPruebas = s.resumenPruebas;
        ctx.serieGN = s.serieEquipo;
        // RF-COV-06: la marca "serie declarada, no leída del equipo" llega al acta. Hasta la rc5 se quedaba en
        // el texto del correo (Sesion.java:300) y el acta no la llevaba.
        ctx.marcaSerie = s.marcaSerie();
        ctx.nombreBT = s.nombre;
        ctx.mac = s.mac;
        ctx.app = "RTV " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ", sucede a 3.6.16)";
    }

    private void construir() {
        refrescarContexto();
        Sesion s = Sesion.get();
        BancoCola cola = null;
        String tipoCola = "COMPLETO";
        try {
            if (s.mac != null && !s.mac.isEmpty() && s.serieConocida()) {
                tipoCola = Campanas.abrir(this, s.serie(), s.mac).colaTipo();
            }
        } catch (IOException | RuntimeException e) {
            // se ve abajo al abrir la campana
        }
        try (InputStream in = getAssets().open(BancoCola.Tipo.de(tipoCola).asset)) {
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

            @Override
            public boolean aceptadoAntes(char k) throws IOException {
                return Campanas.aceptadoAntes(yo, k);
            }

            @Override
            public List<Acta> aceptadas() throws IOException {
                return Campanas.aceptadas(yo);
            }

            @Override
            public Acta ultimaCerrada() throws IOException {
                return Campanas.ultimaCerrada(yo);
            }

            @Override
            public void anadirAUltimaCerrada(String linea) throws IOException {
                Campanas.anadirAUltimaCerrada(yo, linea);
            }

            /** P14-B02: el ZIP de soporte, antes de cerrar el acta; su SHA-256 va al acta. */
            @Override
            public String soporteSha256(Acta a) {
                try {
                    Sesion s = Sesion.get();
                    ultimoSoporte = Campanas.exportarSoporte(yo, s.serie(), s.mac);
                    return ultimoSoporte.sha256;
                } catch (IOException | RuntimeException e) {
                    Registro.nota("no se pudo exportar el ZIP de soporte antes de aceptar: " + e.getMessage());
                    errorZip = e.getMessage();
                    return null;
                }
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
            // RF-COV-12 (H-2): BuildConfig.CORTO solo se lee aqui, al construir el flujo (Base.java es donde vive
            // la Activity real); FlujoCalibracion.java no importa android y recibe el booleano por parametro,
            // para poder probarse en la JVM sin depender del variant compilado (FlujoCalibracion.java:206-211).
            flujo = new FlujoCalibracion(Cliente.instancia(), operador(), almacen, cola, campana, cat,
                    Decisiones.leer(asset(Decisiones.ASSET)), ctx, reloj, BuildConfig.CORTO);
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
            public void actaAceptada(Acta a) {
                // P14-04 / QA-3615: cada acta aceptada deja su ZIP en disco (y la copia en Download/RTV/) en el
                // momento; se comparten una sola vez al acabar la accion.
                synchronized (aceptadasAccion) {
                    aceptadasAccion.add(a);
                }
                exportarSinCompartir();
            }

            @Override
            public void progreso(String texto) {
                Registro.nota("calibrar: " + texto);
                enUi(() -> txtProgreso.setText(texto));
            }

            @Override
            public boolean apagarYEncender() throws IOException, InterruptedException {
                // P12 §5.1: hay que VER caer el enlace. Un vigia mira el enlace mientras el dialogo esta abierto.
                EnlaceSerie en = EnlaceSerie.instancia();
                final boolean[] cayo = {false};
                Thread vigia = new Thread(() -> {
                    long lim = SystemClock.elapsedRealtime() + 120000;
                    while (SystemClock.elapsedRealtime() < lim && !Thread.currentThread().isInterrupted()) {
                        if (!en.estaConectado()) {
                            cayo[0] = true;
                            return;
                        }
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                });
                vigia.start();
                if (CalibrarActivity.this.preguntar("Apagar y encender", "Apague el equipo (la app verá caer la conexión), "
                        + "espere 5 s y enciéndalo. Pulse OK cuando esté encendido: la app vuelve a conectar sola.", "OK",
                        "Cancelar", null) != 0) {
                    vigia.interrupt();
                    return false;
                }
                vigia.join(1000);
                vigia.interrupt();
                if (!cayo[0] && en.estaConectado()) {
                    return false;   // no se vio caer el enlace: no se apago de verdad
                }
                Sesion s = Sesion.get();
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
        if (!BuildConfig.CORTO) {
            // RF-COV-17: la app de calibrar no ensena tarjetas por codigo ni casillas (el operador no elige).
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
        }
        pintarBotones();
        Acta a = flujo.acta();
        txtActa.setText(a == null ? "Sin acta en curso." : a.texto() + "\nPara aceptar: "
                + (flujo.motivoNoAceptar() == null ? "listo (la verificación final se hace al pulsar)"
                : flujo.motivoNoAceptar()));
    }

    private void pintarBotones() {
        boolean libre = !ocupado && flujo != null;
        boolean pend = flujo != null && flujo.rechazoPendiente();
        if (BuildConfig.CORTO) {
            // RF-COV-17: sin casillas, "Calibrar" se habilita con las previas en verde y sin rechazo pendiente;
            // la propia calibrarAutomatico() decide, código a código, qué hay para hacer.
            btnCalibrar.setEnabled(libre && !pend && flujo.motivoPrevias() == null);
        } else {
            Set<Character> sel = seleccion();
            btnCalibrar.setEnabled(libre && flujo.puedeCalibrar(sel));
            btnCalibrar.setText(sel.isEmpty() && libre && flujo.pendientes() > 0 ? "Continuar la calibración a medias" : "Continuar / un código");
            btnTodo.setEnabled(libre && flujo.motivoPrevias() == null && (!sel.isEmpty() || flujo.pendientes() > 0));
        }
        btnBateria.setEnabled(libre && ctx.conectado);
        btnPersistencia.setEnabled(libre && flujo.puedePersistencia());
        btnAceptar.setEnabled(libre && flujo.puedeAceptar());
        btnRechazar.setEnabled(libre && flujo.puedeRechazar());
        btnCerrar.setEnabled(libre && pend);
        btnCerrar.setVisibility(pend ? android.view.View.VISIBLE : android.view.View.GONE);
        boolean bloq = flujo != null && flujo.bloqueoSinRestaurar() != null;
        btnLiberar.setEnabled(libre && bloq);
        btnLiberar.setVisibility(bloq ? android.view.View.VISIBLE : android.view.View.GONE);
        if (pend) {
            // P14-05/06: con un rechazo pendiente no se escribe nada ni se continua.
            if (btnTodo != null) {
                btnTodo.setEnabled(false);
            }
            btnCalibrar.setEnabled(false);
            btnPersistencia.setEnabled(false);
            btnAceptar.setEnabled(false);
        }
    }

    // ---------------------------------------------------------------- acciones

    private interface Accion {
        String hacer() throws IOException, InterruptedException;
    }

    private String calibrarTodo() throws IOException, InterruptedException {
        return flujo.calibrarTodo(seleccionAlPulsar, edNombreAlPulsar, edNotaAlPulsar);
    }

    private String calibrar() throws IOException, InterruptedException {
        return flujo.calibrar(seleccionAlPulsar, edNombreAlPulsar, edNotaAlPulsar);
    }

    /** RF-COV-17: el único botón de la app de calibrar. */
    private String calibrarAutomatico() throws IOException, InterruptedException {
        return flujo.calibrarAutomatico(edNombreAlPulsar);
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
                        String pin = e.getText().toString().trim();
                        Sesion.get().pinAdmin = pin.isEmpty() ? null : pin;
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
            if (flujo.necesitaPin()) {
                Sesion.get().pinAdmin = null;   // QA-3613-02: cualquier fallo de #L vuelve a pedir el PIN
            }
            if (fin != null && "Aceptar".equals(titulo) && fin.startsWith("Acta ACEPTADA") && ligeros.isEmpty()) {
                exportarSinCompartir();
            }
            if (!ligeros.isEmpty() || errorZip != null) {
                String err = compartirAlFinal();
                if (err != null) {
                    fin = fin + "\nATENCIÓN: el ZIP no se pudo exportar (" + err + "). Pulse ZIP de soporte en el Banco o en Campaña.";
                }
            }
            final String f = fin;
            Registro.nota("calibrar (" + titulo + "): " + f);
            enUi(() -> {
                ocupado = false;
                pantallaEncendida(false);
                txtProgreso.setText(f);
                // 3.6.17 (principio de Diego): si faltan muestras, se dice cuales y se ofrece ir a tomarlas.
                // RF-COV-11 (H-1): en la app de calibrar NO HAY Banco (no se instala): el aviso dice qué hacer
                // sin ofrecer ese camino, que en esta app no existe (CalibrarActivity.java:441-446 en 9435f69).
                if (f.contains("no se escribe") || f.contains("falta") || f.contains("Falta")) {
                    if (BuildConfig.CORTO) {
                        alerta("No se puede calibrar", f);
                    } else {
                        new AlertDialog.Builder(this).setTitle("Faltan muestras").setMessage(f)
                                .setPositiveButton("Tomar muestras", (d, w) -> startActivity(
                                        new android.content.Intent(this, BancoActivity.class)))
                                .setNegativeButton("Cerrar", null).show();
                    }
                }
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
        e.setSingleLine(true);
        e.setHint("Motivo");
        caja.addView(e);
        new AlertDialog.Builder(this).setTitle("Rechazar el acta").setView(caja)
                .setMessage("No se grabará fecha. Todo lo escrito (también un #S sin resolver) vuelve a su curva "
                        + "anterior, verificado con #G (P12 §6.8).")
                .setPositiveButton("Rechazar", (d, w) -> accion("Rechazar",
                        () -> flujo.rechazar(e.getText().toString().trim()), true))
                .setNegativeButton("Cancelar", null).show();
    }

    /**
     * P12 §6 / RF-APP-51: al aceptar cada acta se exporta el ZIP ligero (incremental, con su copia en Download/RTV/),
     * sin compartir todavia; el de soporte ya salio antes de cerrar el acta (P14-B02). 3.6.17 (F-05): se guardan
     * todos los ligeros de la accion, porque cada uno lleva solo lo nuevo.
     */
    private void exportarSinCompartir() {
        try {
            Sesion s = Sesion.get();
            Campanas.Exportacion ex = Campanas.exportarConHuellas(this, s.serie(), s.mac);
            synchronized (ligeros) {
                ligeros.add(ex);
            }
            if (ultimoSoporte == null) {
                ultimoSoporte = Campanas.exportarSoporte(this, s.serie(), s.mac);
            }
            if (ex.copia != null && ex.copia.contains("SIN COPIA")) {
                errorZip = ex.copia;
            }
        } catch (IOException | RuntimeException e) {
            Registro.nota("no se pudo exportar el ZIP al aceptar: " + e.getMessage());
            errorZip = e.getMessage();
        }
    }

    /** QA-3615: un solo selector de compartir por accion, con los dos ZIP. null si todo se exporto. */
    private String compartirAlFinal() {
        final java.util.List<Campanas.Exportacion> todos = new java.util.ArrayList<>();
        synchronized (ligeros) {
            todos.addAll(ligeros);
            ligeros.clear();
        }
        if (ultimoSoporte != null) {
            todos.add(ultimoSoporte);
        }
        String err = errorZip;
        ultimoSoporte = null;
        errorZip = null;
        final java.util.List<java.io.File> fs = new java.util.ArrayList<>();
        final String serie = Sesion.get().serie();
        // 3.6.17: el informe de calibracion (texto; el PDF, en la siguiente) a Download/RTV/, con los ZIP.
        java.util.List<Acta> actas;
        synchronized (aceptadasAccion) {
            actas = new java.util.ArrayList<>(aceptadasAccion);
            aceptadasAccion.clear();
        }
        if (!actas.isEmpty()) {
            try {
                String hoy = Calibracion.hoy();
                java.io.File inf = new java.io.File(new java.io.File(getFilesDir(), "campanas"),
                        ExportadorFinal.nombreInforme(serie, hoy));
                try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(inf),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    w.write(ExportadorFinal.informe(actas, ctx.app, hoy));
                }
                String copia = Campanas.copiarADescargas(this, inf, "text/plain");
                Registro.nota("informe de calibración: " + inf.getName() + " -> " + copia);
                fs.add(inf);
            } catch (IOException | RuntimeException e) {
                err = (err == null ? "" : err + "; ") + "informe: " + e.getMessage();
            }
        }
        for (Campanas.Exportacion ex : todos) {
            fs.add(ex.zip);
        }
        if (!fs.isEmpty()) {
            enUi(() -> compartirFicheros(fs, "Calibración de " + serie + ": informe y ZIP (copia en Download/RTV/)"));
        }
        return err;
    }

    /**
     * RTV 1.0.0-rc3 (decision FIRMA-ACTA): campo del nombre de quien firma. El acta lo cita tal cual, asi que
     * es el de la persona que esta delante, no el de quien calibro ni un literal.
     */
    private EditText campoNombre() {
        EditText n = new EditText(this);
        n.setSingleLine(true);
        n.setHint("Nombre de quien firma");
        n.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        return n;
    }

    /** F-03: se libera el equipo tras un cierre SIN RESTAURAR, con el PIN de administrador. */
    private void liberar() {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        final EditText clave = campoPin();
        clave.setHint("PIN de administrador del equipo");
        clave.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        caja.addView(clave);
        final EditText nombre = campoNombre();
        caja.addView(nombre);
        final EditText mot = new EditText(this);
        mot.setSingleLine(true);
        mot.setHint("Motivo (qué se comprobó)");
        caja.addView(mot);
        new AlertDialog.Builder(this).setTitle("Liberar tras el cierre sin restaurar").setView(caja)
                .setMessage("El equipo quedó con códigos en estado desconocido. Lo libera quien tenga el PIN de "
                        + "administrador, y el diario del acta anterior citará su nombre.")
                .setPositiveButton("Liberar", (d, w) -> accion("Liberar",
                        () -> flujo.liberarTrasCierre(clave.getText().toString(), mot.getText().toString().trim(),
                                nombre.getText().toString()), false))
                .setNegativeButton("Cancelar", null).show();
    }

    /** "Cerrar sin restaurar": solo con un rechazo pendiente y el PIN de administrador (P14-06/-10). */
    private void cerrarSinRestaurar() {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        // RTV 1.0.0-rc3: no vale un nombre tecleado como autorizacion; autoriza el PIN (comprobado con #L). El
        // nombre es aparte y va al acta: autorizar y firmar son dos cosas distintas.
        final EditText firma = campoPin();
        firma.setHint("PIN de administrador del equipo");
        firma.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        caja.addView(firma);
        final EditText nombre = campoNombre();
        caja.addView(nombre);
        final EditText mot = new EditText(this);
        mot.setSingleLine(true);
        mot.setHint("Motivo");
        caja.addView(mot);
        new AlertDialog.Builder(this).setTitle("Cerrar sin restaurar").setView(caja)
                .setMessage("El acta queda RECHAZADA y lo que no se pudo restaurar se queda en el equipo tal cual, "
                        + "anotado en el acta. Hace falta el PIN de administrador, y el acta citará el nombre que "
                        + "escriba.")
                .setPositiveButton("Cerrar", (d, w) -> accion("Cerrar sin restaurar",
                        () -> flujo.cerrarSinRestaurar(firma.getText().toString(), mot.getText().toString().trim(),
                                nombre.getText().toString()),
                        false))
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
