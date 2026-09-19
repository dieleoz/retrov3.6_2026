package com.dpi.retrov36;

import android.bluetooth.BluetoothAdapter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * "Calibrar este equipo" (3.6.11, corte B; RF-APP-34). El operador no elige codigo, grado, metodo ni
 * patron de re-medida: salen de la tabla RF-CAL-37 del APK. Flujo: comprobaciones previas; una
 * tarjeta por codigo con su casilla de conformidad; nombre y nota; "Calibrar" (codigo a codigo:
 * bateria, #S, #G, #E, re-medida con los filtros de P10-C1 y a lo sumo una repeticion, restauracion
 * si falla); persistencia (apagar y encender); "Aceptar y grabar fecha" (con #V# y #G frescos) o
 * "Rechazar". Todo queda en el acta en disco, que se retoma al volver.
 */
public class CalibrarActivity extends Base {

    private BancoCola cola;
    private Campana campana;
    private Acta acta;
    private TextView txtPrevias;
    private LinearLayout tarjetas;
    private EditText edNombre;
    private EditText edNota;
    private Button btnCalibrar;
    private Button btnPersistencia;
    private Button btnAceptar;
    private Button btnRechazar;
    private TextView txtProgreso;
    private TextView txtActa;
    private final Map<Character, CheckBox> casillas = new LinkedHashMap<>();
    private final Map<Character, Asistente.Propuesta> propuestas = new LinkedHashMap<>();
    private volatile boolean ocupado;
    private boolean previasOk;
    /** PIN solo en memoria durante el flujo (la app no lo guarda). */
    private String pin;
    private Anclas.Valor sRep;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        titulo("Comprobaciones previas");
        txtPrevias = texto("");
        titulo("Códigos (tabla RF-CAL-37 del APK)");
        tarjetas = new LinearLayout(this);
        tarjetas.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(tarjetas);
        edNombre = campo("Nombre de quien da la conformidad", InputType.TYPE_CLASS_TEXT);
        edNota = campo("Nota", InputType.TYPE_CLASS_TEXT);
        btnCalibrar = boton("Calibrar", v -> calibrar());
        txtProgreso = texto("");
        txtProgreso.setTypeface(Typeface.MONOSPACE);
        btnPersistencia = boton("Persistencia: apagar y encender el equipo", v -> persistencia());
        btnAceptar = boton("Aceptar y grabar fecha", v -> aceptar());
        btnRechazar = boton("Rechazar", v -> rechazar());
        titulo("Acta");
        txtActa = texto("");
        txtActa.setTypeface(Typeface.MONOSPACE);
        cargar();
    }

    // ------------------------------------------------------------------ carga

    private void cargar() {
        try (InputStream in = getAssets().open(BancoCola.ASSET)) {
            cola = BancoCola.cargar(ImportadorCampana.leer(in));
        } catch (IOException | RuntimeException e) {
            cola = null;
        }
        Sesion s = Sesion.get();
        try {
            if (s.mac != null && !s.mac.isEmpty() && s.serieConocida()) {
                campana = Campanas.abrir(this, s.serie(), s.mac);
                acta = Campanas.actaEnCurso(this);
            }
        } catch (IOException | RuntimeException e) {
            alerta("Campaña", "No se pudo abrir: " + e.getMessage());
        }
        pintar();
    }

    private static final class Previa {
        final boolean ok;
        final String texto;

        Previa(boolean ok, String texto) {
            this.ok = ok;
            this.texto = texto;
        }
    }

    private List<Previa> previas() {
        List<Previa> l = new ArrayList<>();
        Sesion s = Sesion.get();
        boolean con = EnlaceSerie.instancia().estaConectado();
        l.add(new Previa(con, con ? "Conectado con " + s.nombre : "Sin conexión: conecte con el equipo"));
        l.add(new Previa(s.es362(), s.es362() ? "Firmware " + s.firmware()
                : "El firmware no es la 3.6.2 (" + s.firmware() + "): hacen falta #SC y #GN. Pase las pruebas del equipo"));
        l.add(new Previa(s.apto != null, s.apto == null ? "Pruebas del equipo sin hacer: páselas antes"
                : "Pruebas: " + (s.apto ? "APTO" : "NO APTO") + " (" + s.resumenPruebas + ")"));
        l.add(new Previa(cola != null, cola != null ? "Cola del banco md5 " + cola.md5 : "Cola del banco no admitida"));
        l.add(new Previa(campana != null, campana != null ? "Campaña de " + campana.equipo + " (" + campana.mac + ")"
                : "Sin campaña de este equipo"));
        // RF-APP-35: identidad por #GN#.
        String gn = s.serieEquipo;
        boolean idOk = campana != null && gn != null && !Calibracion.NONE.equals(gn) && gn.equals(campana.equipo);
        l.add(new Previa(idOk, gn == null ? "Serie del equipo (#GN#) sin leer: pase las pruebas"
                : Calibracion.NONE.equals(gn) ? "El equipo no tiene serie (#GN# = NONE): dé de alta la serie en Avanzado"
                : idOk ? "Serie #GN# " + gn + " = campaña"
                : "La serie #GN# (" + gn + ") no coincide con la campaña (" + (campana == null ? "-" : campana.equipo)
                + "): no se calibra. Corrija la serie en Avanzado (doble entrada)"));
        sRep = campana == null ? null : Anclas.sRep(cola, campana);
        boolean srOk = sRep != null && !Double.isNaN(sRep.valor);
        l.add(new Previa(srOk, sRep == null ? "s_rep: sin campaña" : sRep.texto + (srOk ? "" : " — mida la A5 del inicio del banco")));
        if (campana != null && campana.bateriaBloqueaEscrituras()) {
            l.add(new Previa(false, "Batería: la última lectura bloquea las escrituras (n = 0 o sin respuesta). Cambie la batería"));
        }
        if (acta != null && acta.invalidada()) {
            l.add(new Previa(false, "El acta en curso está invalidada: recházela"));
        }
        return l;
    }

    private void pintar() {
        List<Previa> pv = previas();
        StringBuilder sb = new StringBuilder();
        previasOk = true;
        for (Previa p : pv) {
            sb.append(p.ok ? "OK  " : "NO  ").append(p.texto).append('\n');
            previasOk &= p.ok;
        }
        txtPrevias.setText(sb.toString());
        txtPrevias.setBackgroundColor(previasOk ? VERDE : ROJO);
        pintarTarjetas();
        boolean hayActa = acta != null && !acta.cerrada();
        btnCalibrar.setEnabled(previasOk && !ocupado && (algunaMarcada() || (hayActa && pendientes() > 0)));
        btnCalibrar.setText(hayActa && pendientes() > 0 && !algunaMarcada()
                ? "Continuar la calibración a medias de " + acta.equipo : "Calibrar");
        btnPersistencia.setEnabled(hayActa && !ocupado && acta.motivoNoAceptable(false) == null);
        // Tras reconectar (persistencia) la version vuelve a "sin detectar": sin 3.6.2 detectada no se acepta,
        // porque sin ella no se sabria si hay que grabar #SC.
        btnAceptar.setEnabled(previasOk && hayActa && !ocupado && acta.motivoNoAceptable(true) == null);
        btnRechazar.setEnabled(hayActa && !ocupado);
        txtActa.setText(acta == null ? "Sin acta en curso." : acta.texto()
                + (acta.cerrada() ? "" : "\nPara aceptar: " + (acta.motivoNoAceptable(true) == null ? "listo"
                : acta.motivoNoAceptable(true))));
    }

    private boolean algunaMarcada() {
        for (CheckBox c : casillas.values()) {
            if (c.isChecked() && c.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    private int pendientes() {
        int n = 0;
        if (acta != null) {
            for (Acta.Codigo c : acta.codigos()) {
                if (!c.resuelto()) {
                    n++;
                }
            }
            if (acta.escribiendo() != 0) {
                n++;
            }
        }
        return n;
    }

    /** Patrones de AJUSTE y RE-MEDIDA del codigo k en la cola. */
    private Set<String> patronesAjuste(char k) {
        Set<String> s = new HashSet<>();
        if (cola != null) {
            for (BancoCola.Paso p : cola.pasos) {
                if ("PATRON".equals(p.tipo) && String.valueOf(k).equals(p.codigo)
                        && ("AJUSTE".equals(p.uso) || "RE-MEDIDA".equals(p.uso))) {
                    s.add(p.patron);
                }
            }
        }
        return s;
    }

    private List<Medida> medidasDe(char k) {
        Set<String> ps = patronesAjuste(k);
        List<Medida> l = new ArrayList<>();
        for (Medida m : campana.medidasElegidas()) {
            if (ps.contains(m.patron.nombre)) {
                l.add(m);
            }
        }
        return l;
    }

    private void pintarTarjetas() {
        Map<Character, Boolean> marcadas = new LinkedHashMap<>();
        for (Map.Entry<Character, CheckBox> e : casillas.entrySet()) {
            marcadas.put(e.getKey(), e.getValue().isChecked());
        }
        tarjetas.removeAllViews();
        casillas.clear();
        propuestas.clear();
        if (campana == null || cola == null) {
            return;
        }
        Map<Integer, String> est = campana.pasos();
        for (TablaCalibracion.Fila f : TablaCalibracion.tabla().values()) {
            TextView t = new TextView(this);
            t.setPadding(dp(6), dp(8), dp(6), dp(2));
            t.setTypeface(Typeface.MONOSPACE);
            StringBuilder sb = new StringBuilder(f.texto()).append('\n');
            boolean activa = false;
            if (!f.escribible()) {
                sb.append("  No se escribe.");
            } else if (!cola.calibrable(String.valueOf(f.codigo), est)) {
                sb.append("  No calibrable: faltan patrones de AJUSTE o RE-MEDIDA del banco (medidos o saltados).");
            } else {
                Asistente.Propuesta p = propuesta(f, sb);
                if (p != null) {
                    propuestas.put(f.codigo, p);
                    activa = p.escribible();
                }
            }
            if (acta != null && acta.codigo(f.codigo) != null) {
                Acta.Codigo c = acta.codigo(f.codigo);
                sb.append("\n  En el acta: ").append(c.conforme() ? "CONFORME" : c.restaurado != null ? "RESTAURADO"
                        : "escrito, re-medida pendiente").append(" (").append(c.intentos.size()).append(" intentos)");
            }
            t.setText(sb.toString());
            t.setBackgroundColor(activa ? GRIS : AMARILLO);
            tarjetas.addView(t);
            if (f.escribible()) {
                CheckBox cb = new CheckBox(this);
                cb.setText("Conforme: escribir el código " + f.codigo
                        + (f.dispensa.isEmpty() ? "" : " (con dispensa " + f.dispensa + ")"));
                cb.setEnabled(activa && !ocupado);
                cb.setChecked(activa && Boolean.TRUE.equals(marcadas.get(f.codigo)));
                cb.setOnCheckedChangeListener((v, c) -> pintar());
                tarjetas.addView(cb);
                casillas.put(f.codigo, cb);
            }
        }
    }

    /** Propuesta del metodo de la tabla (sin elegir nada el operador). */
    private Asistente.Propuesta propuesta(TablaCalibracion.Fila f, StringBuilder sb) {
        char k = f.codigo;
        List<Medida> med = medidasDe(k);
        Anclas.Valor osc = Anclas.oscuro(cola, campana, k);
        if (!Double.isNaN(osc.valor)) {
            Asistente.X_OSCURO = osc.valor;          // B13 con el oscuro medido, no 575 (hallazgo 11)
        }
        Asistente.S_REP_REL = sRep == null ? Double.NaN : sRep.valor;
        List<Patron> cat;
        try {
            cat = Sesion.get().patrones(this);
        } catch (IOException e) {
            sb.append("  Sin catálogo.");
            return null;
        }
        Asistente.Propuesta p = null;
        if (f.metodo == TablaCalibracion.Metodo.GRADO1 || f.metodo == TablaCalibracion.Metodo.GRADO1_O_ANCLADA) {
            p = Asistente.proponer(k, 1, med, Sesion.get().ecuacionVigente(k), cat);
            if (f.metodo == TablaCalibracion.Metodo.GRADO1_O_ANCLADA && p.ajuste != null
                    && Asistente.criterioFirmwareS(p.ajuste.ecuacion) != null) {
                sb.append("  La recta libre no pasa #S: se usa la anclada.\n");
                p = null;
            }
        }
        if (p == null) {
            if (Double.isNaN(osc.valor)) {
                sb.append("  Recta anclada no calculable: ").append(osc.texto);
                return null;
            }
            Asistente.X_ANCLA = osc.valor;
            Asistente.R_ANCLA = 0;
            Asistente.ORIGEN_ANCLA = osc.texto;
            p = Asistente.proponer(k, Asistente.GRADO_ANCLADA, med, Sesion.get().ecuacionVigente(k), cat);
        }
        sb.append("  ").append(p.metodo).append(", ").append(p.puntos.size()).append(" patrones");
        if (p.ajuste != null) {
            Ecuacion e = p.ajuste.ecuacion;
            sb.append(String.format(Locale.US, "\n  c1 %s  c0 %s  error máx %.1f\n  %s", Tramas.coeficiente(e.c1),
                    Tramas.coeficiente(e.c0), p.ajuste.errorMaximo, osc.texto));
        }
        for (String s : p.incumplimientos) {
            sb.append("\n  Incumple (").append(f.dispensa.isEmpty() ? "SIN dispensa en la tabla" : "dispensa " + f.dispensa)
                    .append("): ").append(s);
        }
        for (String s : p.bloqueos) {
            sb.append("\n  NO SE PUEDE ESCRIBIR: ").append(s);
        }
        return p;
    }

    // ---------------------------------------------------------------- calibrar

    private void calibrar() {
        final List<Character> sel = new ArrayList<>();
        for (Map.Entry<Character, CheckBox> e : casillas.entrySet()) {
            if (e.getValue().isChecked() && e.getValue().isEnabled()) {
                sel.add(e.getKey());
            }
        }
        for (char k : sel) {
            TablaCalibracion.Fila f = TablaCalibracion.fila(k);
            Asistente.Propuesta p = propuestas.get(k);
            if (p != null && !p.incumplimientos.isEmpty() && f.dispensa.isEmpty()) {
                alerta("Código " + k, "Incumple " + p.incumplimientos + " y la tabla no le da dispensa: no se escribe.");
                return;
            }
        }
        final String nombre = edNombre.getText().toString().trim();
        final String nota = edNota.getText().toString().trim();
        if (!sel.isEmpty() && (nombre.isEmpty() || nota.isEmpty())) {
            alerta("Conformidad", "Escriba el nombre y la nota.");
            return;
        }
        final EditText e = campoPin();
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("PIN del equipo").setView(e)
                .setPositiveButton("Calibrar", (d, w) -> {
                    pin = e.getText().toString().trim();
                    ocupado = true;
                    pantallaEncendida(true);
                    pintar();
                    Cliente.instancia().ejecutar(() -> flujo(sel, nombre, nota));
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void progreso(String t) {
        Registro.nota("calibrar: " + t);
        enUi(() -> txtProgreso.setText(t));
    }

    private void flujo(List<Character> sel, String nombre, String nota) {
        String fin;
        try {
            fin = flujoInterno(sel, nombre, nota);
        } catch (IOException | InterruptedException | RuntimeException e) {
            fin = "Interrumpido: " + EnlaceSerie.descripcion(e) + ". El acta queda en disco; al volver se retoma.";
        }
        final String f = fin;
        enUi(() -> {
            ocupado = false;
            pantallaEncendida(false);
            txtProgreso.setText(f);
            pintar();
        });
    }

    private String flujoInterno(List<Character> sel, String nombre, String nota) throws IOException, InterruptedException {
        Sesion s = Sesion.get();
        if (!OpsEquipo.login(pin)) {
            return "PIN rechazado: no se ha escrito nada.";
        }
        if (acta == null || acta.cerrada()) {
            acta = new Acta(campana.equipo, campana.mac, s.firmware(), 5, 4, 1, Sesion.ahoraIso());
            acta.tabla = TablaCalibracion.VERSION;
            Campanas.adjuntarActa(this, acta);
            acta.dato("md5 de la cola", cola.md5);
            acta.dato("s_rep", sRep.texto);
            acta.dato("app", "RTV " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        }
        if (!sel.isEmpty()) {
            StringBuilder cods = new StringBuilder();
            for (char k : sel) {
                cods.append(k).append(' ');
            }
            acta.conformidad(nombre + ": códigos " + cods.toString().trim() + ". " + nota);
        }
        progreso("Batería antes de la calibración...");
        Bateria.Lectura b = OpsEquipo.bateria(campana, acta);
        if (b.bloqueaEscrituras) {
            return b.texto + " No se escribe nada.";
        }
        if (acta.escribiendo() != 0) {
            progreso(resolverCorte());
        }
        // Codigos a medias del acta (escritos y sin resolver), y despues los seleccionados.
        List<Character> orden = new ArrayList<>();
        for (Acta.Codigo c : acta.codigos()) {
            if (!c.resuelto() && !orden.contains(c.k)) {
                orden.add(c.k);
            }
        }
        for (char k : sel) {
            if (!orden.contains(k)) {
                orden.add(k);
            }
        }
        for (char k : orden) {
            Acta.Codigo c = acta.codigo(k);
            if (c == null || c.resuelto()) {
                if (c != null && c.restaurado != null) {
                    continue;
                }
                String r = escribir(k);
                if (r != null) {
                    return r;
                }
            }
            String r = remedida(k);
            if (r != null) {
                return r;
            }
        }
        return "Códigos escritos y verificados. Ahora: persistencia (apagar y encender) y después aceptar.";
    }

    /** RF-APP-37: resuelve un corte entre #S y la relectura. */
    private String resolverCorte() throws IOException, InterruptedException {
        char k = acta.escribiendo();
        Ecuacion g = OpsEquipo.leerG(k);
        String t;
        if (g != null && g.igualFloat32(acta.escribiendoEnviada(), Ecuacion.ULP_S)) {
            String porE = OpsEquipo.comprobarPorE(k, acta.escribiendoEnviada());
            if (porE == null) {
                acta.escrito(k, OpsEquipo.tramaG(k, g), g, "", "reanudado tras un corte durante #S", "");
                t = "Corte durante #S," + k + ": el #S entró (#G y #E coinciden); sigue la re-medida.";
            } else {
                t = "Corte durante #S," + k + ": " + porE + ". " + OpsEquipo.restaurar(k, acta.escribiendoAnterior());
                acta.sinEscribir(k, t);
            }
        } else if (g != null && g.igualFloat32(acta.escribiendoAnterior(), Ecuacion.ULP_G)) {
            t = "Corte durante #S," + k + ": el #S no entró; el código sigue como estaba.";
            acta.sinEscribir(k, t);
        } else {
            t = "Corte durante #S," + k + ": la curva no es ni la enviada ni la anterior. "
                    + OpsEquipo.restaurar(k, acta.escribiendoAnterior());
            acta.sinEscribir(k, t);
        }
        OpsEquipo.leerV();
        return t;
    }

    /** #S del codigo k con la propuesta de la tabla. null si fue bien; si no, el motivo para parar. */
    private String escribir(char k) throws IOException, InterruptedException {
        String no = acta.motivoNoEscribir(k);
        if (no != null) {
            return no;
        }
        Asistente.Propuesta p = propuestas.get(k);
        if (p == null || !p.escribible()) {
            return "El código " + k + " no tiene una propuesta escribible.";
        }
        progreso("Código " + k + ": batería antes de #S...");
        Bateria.Lectura b = OpsEquipo.bateria(campana, acta);
        if (b.bloqueaEscrituras) {
            return b.texto + " No se escribe el código " + k + ".";
        }
        Tramas.TramaS ts = Tramas.tramaS(k, p.ajuste.ecuacion);
        if (ts == null || Asistente.criterioFirmwareS(ts.enviada) != null) {
            return "El código " + k + " no pasa el criterio de #S.";
        }
        Ecuacion anterior = OpsEquipo.leerG(k);
        if (anterior == null) {
            return "No se pudo leer el estado anterior del código " + k + " (#G): no se escribe.";
        }
        progreso("Código " + k + ": #S...");
        acta.escribiendo(k, anterior, ts.enviada);
        Cliente.Respuesta r = Cliente.instancia().pedir(ts.texto, Tramas.Tipo.ADMIN, 5000);
        Ecuacion e = OpsEquipo.leerG(k);
        boolean igual = e != null && e.igualFloat32(ts.enviada, Ecuacion.ULP_S);
        String porE = igual ? OpsEquipo.comprobarPorE(k, ts.enviada) : "la relectura #G no coincide";
        String v = OpsEquipo.leerV();
        acta.dato("#V# posterior", v);
        if (Tramas.esOk(r.trama) && igual && porE == null) {
            TablaCalibracion.Fila f = TablaCalibracion.fila(k);
            String osc = String.format(Locale.US, "Oscuro (x = %.1f): R %.1f (fábrica %.1f)", Asistente.X_OSCURO,
                    e.evaluar(Asistente.X_OSCURO), Fabrica.ecuacion(k).evaluar(Asistente.X_OSCURO));
            if (p.metodo.contains("anclada")) {
                acta.dato("oscuro", Asistente.ORIGEN_ANCLA);
            }
            acta.escrito(k, OpsEquipo.tramaG(k, e), e, osc, p.metodo + "; tabla " + f.texto(),
                    p.incumplimientos.isEmpty() ? "" : "Dispensa " + f.dispensa + " aceptada en la conformidad: "
                            + String.join("; ", p.incumplimientos));
            return null;
        }
        String t = "#S," + k + " -> " + r.describir() + "; " + (porE == null ? "" : porE) + ". "
                + OpsEquipo.restaurar(k, anterior);
        acta.sinEscribir(k, t);
        return "Escritura del código " + k + " fallida: " + t;
    }

    /**
     * Re-medida (RF-CAL-39 con P10-C1): K = 5 x M = 4, 'e' y el codigo alternados. Colocaciones no
     * validas se registran y se repiten. Una repeticion como maximo; si falla, restaurar.
     * null si quedo conforme; si no, el motivo para parar.
     */
    private String remedida(char k) throws IOException, InterruptedException {
        TablaCalibracion.Fila f = TablaCalibracion.fila(k);
        Patron pat = campana.patron(f.remedida);
        Campana.Serie banco = pat == null ? null : campana.elegida(pat.nombre);
        if (banco == null) {
            return "Falta la serie del banco de " + f.remedida + " (re-medida del " + k + ").";
        }
        double xBanco = banco.media();
        int kBanco = Math.max(1, banco.colocaciones().size());
        while (true) {
            Acta.Codigo c = acta.codigo(k);
            if (c.conforme()) {
                return null;
            }
            if (!c.puedeRepetir()) {
                progreso("Código " + k + ": dos re-medidas no conformes: se restaura.");
                String t = OpsEquipo.restaurar(k, c.anterior);
                acta.restaurado(k, t);
                return "Código " + k + " restaurado tras dos re-medidas no conformes (PA-12): " + t
                        + ". La secuencia se detiene.";
            }
            int r = preguntar("Re-medida del código " + k + (c.validos() == 1 ? " (repetición)" : ""),
                    "Coloque " + pat + " y pulse OK. " + Remedida3611.K + " colocaciones × " + Remedida3611.M
                            + " pares ('e' y código " + k + ").", "OK", "Parar aquí", null);
            if (r != 0) {
                return "Parado por el operador en la re-medida del código " + k + ". El acta queda a medias.";
            }
            List<double[]> xs = new ArrayList<>();
            List<double[]> rs = new ArrayList<>();
            while (xs.size() < Remedida3611.K) {
                progreso("Código " + k + ": colocación " + (xs.size() + 1) + " de " + Remedida3611.K + "...");
                Cliente.Respuesta a = Cliente.instancia().pedir("e", Tramas.Tipo.MEDIDA, Cliente.TIMEOUT_MEDIDA_MS);
                Integer va = a.valida() ? Tramas.valorMedida(a.trama) : null;
                Registro.nota("re-medida: asentamiento, descartado: " + a.describir());
                double[] xe = new double[Remedida3611.M];
                double[] rk = new double[Remedida3611.M];
                String falla = null;
                for (int i = 0; i < Remedida3611.M && falla == null; i++) {
                    Cliente.Respuesta re = Cliente.instancia().pedir("e", Tramas.Tipo.MEDIDA, Cliente.TIMEOUT_MEDIDA_MS);
                    Cliente.Respuesta rc = Cliente.instancia().pedir(String.valueOf(k), Tramas.Tipo.MEDIDA,
                            Cliente.TIMEOUT_MEDIDA_MS);
                    Integer vx = re.valida() ? Tramas.valorMedida(re.trama) : null;
                    Integer vr = rc.valida() ? Tramas.valorMedida(rc.trama) : null;
                    if (vx == null || vr == null) {
                        falla = "par " + (i + 1) + " sin respuesta: " + re.describir() + " / " + rc.describir();
                    } else {
                        xe[i] = vx;
                        rk[i] = vr;
                    }
                }
                if (falla == null) {
                    falla = Remedida3611.colocacion(pat.nombre, va == null ? Double.NaN : va, xBanco, xe, rk, c.leida);
                }
                if (falla != null) {
                    acta.intento(k, Sesion.ahoraIso(), "NO_VALIDA", falla);
                    int q = preguntar("Colocación no válida", falla + "\nVuelva a colocar " + pat.nombre + " y pulse OK.",
                            "OK", "Parar aquí", null);
                    if (q != 0) {
                        return "Parado por el operador. El acta queda a medias.";
                    }
                    continue;
                }
                xs.add(xe);
                rs.add(rk);
                if (xs.size() < Remedida3611.K) {
                    preguntar("Levante y apoye (" + (xs.size() + 1) + " de " + Remedida3611.K + ")",
                            "Levante el equipo y vuelva a apoyarlo sobre " + pat.nombre + ".", "OK", null, null);
                }
            }
            Remedida3611.Resultado res = Remedida3611.evaluar(pat.nombre, pat.valor, xs, rs, xBanco, kBanco,
                    sRep.valor, sRep.texto);
            acta.intento(k, Sesion.ahoraIso(), res.estado, res.texto);
            if ("NO_EVALUABLE".equals(res.estado)) {
                return res.texto;
            }
            if (!"CONFORME".equals(res.estado) && acta.codigo(k).puedeRepetir()) {
                preguntar("Re-medida NO CONFORME", res.texto + "\nQueda una repetición.", "OK", null, null);
            }
        }
    }

    // ------------------------------------------------------------ persistencia

    private void persistencia() {
        // Al apagar, el equipo pierde el PIN: se pide antes para volver a entrar tras reconectar.
        final EditText e = campoPin();
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("PIN del equipo").setView(e)
                .setPositiveButton("Seguir", (d, w) -> {
                    pin = e.getText().toString().trim();
                    persistenciaConPin();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void persistenciaConPin() {
        ocupado = true;
        pintar();
        Cliente.instancia().ejecutar(() -> {
            String fin;
            try {
                fin = persistenciaInterna();
            } catch (IOException | InterruptedException | RuntimeException e) {
                fin = "Persistencia interrumpida: " + EnlaceSerie.descripcion(e);
            }
            final String f = fin;
            enUi(() -> {
                ocupado = false;
                txtProgreso.setText(f);
                pintar();
            });
        });
    }

    private String persistenciaInterna() throws IOException, InterruptedException {
        preguntar("Persistencia", "Apague el equipo, espere 5 s y enciéndalo. Pulse OK cuando esté encendido "
                + "(la app vuelve a conectar sola).", "OK", null, null);
        Sesion s = Sesion.get();
        EnlaceSerie en = EnlaceSerie.instancia();
        long limite = SystemClock.elapsedRealtime() + 45000;
        while (!en.estaConectado() && SystemClock.elapsedRealtime() < limite) {
            if (!en.estaConectando()) {
                BluetoothAdapter ad = EnlaceSerie.adaptador();
                if (ad != null) {
                    en.conectar(this, ad.getRemoteDevice(s.mac));
                }
            }
            Thread.sleep(1000);
        }
        if (!en.estaConectado()) {
            return "No se pudo reconectar con " + s.mac + ": repita la persistencia.";
        }
        Thread.sleep(2500);   // margen tras el arranque del equipo: provisional, sin medir
        if (!OpsEquipo.login(pin)) {
            return "Reconectado, pero el PIN fue rechazado: repita la persistencia.";
        }
        StringBuilder t = new StringBuilder();
        String v = OpsEquipo.leerV();
        t.append("#V# ").append(v).append("; ");
        boolean ok = !v.startsWith("sin respuesta");
        for (Acta.Codigo c : acta.certificados()) {
            Ecuacion g = OpsEquipo.leerG(c.k);
            boolean igual = g != null && g.igualFloat32(c.leida, Ecuacion.ULP_G);
            String porE = OpsEquipo.comprobarPorE(c.k, c.leida);
            ok &= igual && porE == null;
            t.append("código ").append(c.k).append(": #G ").append(igual ? "igual" : "DISTINTO")
                    .append(", #E ").append(porE == null ? "coincide" : porE).append("; ");
        }
        acta.persistencia(ok, t.toString());
        acta.dato("#V# posterior", v);
        return (ok ? "Persistencia OK: " : "Persistencia FALLA: ") + t;
    }

    // ------------------------------------------------------------------ cierre

    private void aceptar() {
        final EditText e = campoPin();
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Aceptar el acta")
                .setMessage(acta.texto() + "\nAntes se releen #V# y #G; después se graba la fecha (#SC) una sola vez.")
                .setView(e)
                .setPositiveButton("Aceptar y grabar fecha", (d, w) -> {
                    pin = e.getText().toString().trim();
                    ocupado = true;
                    pintar();
                    Cliente.instancia().ejecutar(() -> {
                        String fin;
                        try {
                            fin = aceptarInterno();
                        } catch (IOException | InterruptedException | RuntimeException ex) {
                            fin = "No se aceptó: " + EnlaceSerie.descripcion(ex);
                        }
                        final String f = fin;
                        enUi(() -> {
                            ocupado = false;
                            txtProgreso.setText(f);
                            pintar();
                        });
                    });
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private String aceptarInterno() throws IOException, InterruptedException {
        Sesion s = Sesion.get();
        if (!s.es362()) {
            return "Firmware sin detectar como 3.6.2 (" + s.firmware() + "): pase las pruebas del equipo y vuelva a aceptar.";
        }
        if (!OpsEquipo.login(pin)) {
            return "PIN rechazado.";
        }
        // P10-C5 / RF-APP-44: #V# y #G frescos, iguales a lo certificado.
        String v = OpsEquipo.leerV();
        StringBuilder t = new StringBuilder("#V# ").append(v).append("; ");
        boolean ok = !v.startsWith("sin respuesta");
        for (Acta.Codigo c : acta.certificados()) {
            Ecuacion g = OpsEquipo.leerG(c.k);
            boolean igual = g != null && g.igualFloat32(c.leida, Ecuacion.ULP_G);
            ok &= igual;
            t.append("código ").append(c.k).append(igual ? " igual a lo certificado; " : " DISTINTO de lo certificado; ");
        }
        acta.verificacionFinal(ok, t.toString());
        acta.dato("#V# posterior", v);
        if (!ok) {
            return "Verificación final FALLA: " + t + " El acta no se puede aceptar.";
        }
        String m = acta.motivoNoAceptable(true);
        if (m != null) {
            return "No aceptable: " + m;
        }
        String hoy = Calibracion.hoy();
        String fecha = null;
        if (s.es362()) {
            Cliente.Respuesta r = Cliente.instancia().pedir("#SC," + hoy + "#", Tramas.Tipo.ADMIN, 5000);
            Cliente.Respuesta g = OpsEquipo.pedir("#GC#");
            String leida = Calibracion.fechaDe(g.trama);
            if (Tramas.esOk(r.trama) && hoy.equals(leida)) {
                fecha = hoy;
                s.fechaCalibracion = hoy;
            } else {
                OpsEquipo.leerV();
                return "#SC no quedó grabada (#SC -> " + r.describir() + ", #GC# -> " + g.describir()
                        + "): el acta NO se cierra. Reintente.";
            }
        }
        acta.aceptar(Sesion.ahoraIso(), fecha, s.es362(), true);
        String v2 = OpsEquipo.leerV();
        java.io.File f = Campanas.cerrarActaEnDisco(this, acta);
        return "Acta ACEPTADA; fecha " + fecha + " (vence " + (fecha == null ? "-" : Calibracion.vencimiento(fecha))
                + "); #V# " + v2 + ". Guardada en " + f.getName() + ".";
    }

    private void rechazar() {
        final EditText e = new EditText(this);
        e.setHint("Motivo");
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Rechazar el acta").setView(e)
                .setMessage("No se grabará fecha. Lo escrito sigue en el equipo: restaure a fábrica en Avanzado si no debe quedar.")
                .setPositiveButton("Rechazar", (d, w) -> {
                    acta.rechazar(Sesion.ahoraIso(), e.getText().toString().trim());
                    try {
                        Campanas.cerrarActaEnDisco(this, acta);
                    } catch (IOException ex) {
                        alerta("Acta", ex.getMessage());
                    }
                    pintar();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    /** Mientras el flujo escribe o re-mide, Atras no sale: el acta y el equipo quedarian a medias sin decirlo. */
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
        if (btnCalibrar != null && !ocupado) {
            pintar();
        }
    }
}
