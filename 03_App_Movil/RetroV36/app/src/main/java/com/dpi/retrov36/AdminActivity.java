package com.dpi.retrov36;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modo administrador (solo V3.6): PIN, lectura de coeficientes, asistente de
 * calibracion, restaurar fabrica y cambio de PIN. Las funciones de escritura
 * solo se muestran con el modo abierto (RF-APP-19).
 */
public class AdminActivity extends Base {

    /** El firmware cierra el modo a los 10 min sin tramas '#'; la app, un poco antes. */
    private static final long CADUCIDAD_MS = 9 * 60 * 1000 + 30 * 1000;
    private static final int MAX_FALLOS_PIN = 5;

    private TextView txtAcceso;
    private Button btnEntrar;
    private LinearLayout panel;
    private TextView txtCoef;
    private Spinner spAjuste;
    private RadioGroup rgGrado;
    private TextView txtAjuste;
    private Button btnEscribir;
    private Spinner spFabrica;
    private EditText edPinActual;
    private EditText edPinNuevo;
    private TextView txtOp;
    private volatile boolean ocupado;
    private Asistente.Propuesta propuesta;
    private TextView txtCal;
    private TextView txtActa;
    private EditText edXosc;
    private EditText edOscMargen;
    private EditText edOscMin;
    private TextView txtSrep;
    private Spinner spRemedida;
    private Button btnRemedida;
    private Button btnAceptar;
    private Button btnRechazar;
    private List<Patron> patronesRemedida = new ArrayList<>();
    private EditText edSerie;
    private Button btnSerie;
    private Button btnFT;

    private interface Tarea {
        String correr() throws IOException, InterruptedException;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        txtAcceso = texto("");
        btnEntrar = boton("Entrar en modo administrador (PIN)", v -> entrar());
        txtOp = texto("");
        txtOp.setTypeface(Typeface.MONOSPACE);

        // Todo lo demas cuelga de 'panel', visible solo con el modo abierto.
        LinearLayout exterior = raiz;
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        exterior.addView(panel);
        raiz = panel;

        titulo("Coeficientes del equipo");
        boton("Leer coeficientes (#G x12, #GT#) y guardar copia", v -> leerCoeficientes("lectura manual"));
        txtCoef = texto("");
        txtCoef.setTypeface(Typeface.MONOSPACE);

        titulo("Asistente de calibración");
        List<Patron> catalogo = catalogo();
        texto(catalogo == null ? "No se pudo leer el catálogo de patrones." : Asistente.cobertura(catalogo));
        texto("Usa las medias por patrón de la sesión de medida (pantalla Medida de patrones), de los patrones "
                + "del color y la clase del código (tipo I = opacas 7, 8, a-d). Hacen falta al menos grado + 2 "
                + "niveles certificados distintos y un rango certificado amplio: con rango estrecho no se ajusta.");
        spAjuste = new Spinner(this);
        List<String> aj = new ArrayList<>();
        for (char k : Fabrica.CODIGOS) {
            aj.add(catalogo == null ? k + "  " + Fabrica.nombre(k) : Asistente.cobertura(k, catalogo).texto());
        }
        spAjuste.setAdapter(adaptador(aj));
        panel.addView(spAjuste);
        rgGrado = new RadioGroup(this);
        rgGrado.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton g1 = new RadioButton(this);
        g1.setText("Grado 1");
        g1.setId(View.generateViewId());
        RadioButton g2 = new RadioButton(this);
        g2.setText("Grado 2");
        g2.setId(View.generateViewId());
        RadioButton g3 = new RadioButton(this);
        g3.setText("Recta anclada en oscuro");
        g3.setId(View.generateViewId());
        rgGrado.addView(g1);
        rgGrado.addView(g2);
        rgGrado.addView(g3);
        rgGrado.check(g1.getId());
        panel.addView(rgGrado);
        Button ajustar = boton("Ajustar", v -> ajustar());
        btnEscribir = boton("Escribir en el equipo...", v -> confirmarEscritura());
        fila(ajustar, btnEscribir);
        txtAjuste = texto("");
        txtAjuste.setTypeface(Typeface.MONOSPACE);

        titulo("Acta de calibración");
        texto("Un código cada vez: #S, #G, #E y re-medida de verificación; al final se acepta el acta y sólo "
                + "entonces se graba la fecha de calibración (#SC). El protocolo de disparos queda fijo mientras dura.");
        edXosc = campo("x de oscuro (P9-B13)", InputType.TYPE_CLASS_NUMBER);
        edXosc.setText(String.format(Locale.US, "%.0f", Asistente.X_OSCURO));
        edOscMargen = campo("Margen sobre fábrica en oscuro", InputType.TYPE_CLASS_NUMBER);
        edOscMargen.setText(String.format(Locale.US, "%.0f", Asistente.OSCURO_MARGEN));
        edOscMin = campo("Mínimo en oscuro", InputType.TYPE_CLASS_NUMBER);
        edOscMin.setText(String.format(Locale.US, "%.0f", Asistente.OSCURO_MINIMO));
        fila(edXosc, edOscMargen, edOscMin);
        // P10 hallazgo 9: s_rep no se teclea; sale de la A5 del inicio del banco (Anclas.sRep).
        txtSrep = texto("s_rep: se calcula de la A5 del inicio del banco");
        spRemedida = new Spinner(this);
        panel.addView(spRemedida);
        // 3.6.11: la re-medida (D-20, una repeticion como maximo) y la aceptacion (persistencia, #V# y #G
        // frescos, #SC) se hacen en "Calibrar este equipo", que retoma el acta en disco.
        btnRemedida = boton("Re-medida: en Calibrar este equipo",
                v -> startActivity(new android.content.Intent(this, CalibrarActivity.class)));
        btnAceptar = boton("Aceptar acta: en Calibrar este equipo",
                v -> startActivity(new android.content.Intent(this, CalibrarActivity.class)));
        btnRechazar = boton("Rechazar acta (no se graba fecha)", v -> rechazarActa());
        fila(btnAceptar, btnRechazar);
        txtActa = texto("");
        txtActa.setTypeface(Typeface.MONOSPACE);

        titulo("Serie y fecha de calibración (firmware 3.6.2)");
        txtCal = texto("");
        edSerie = campo("Serie del equipo (1-12 caracteres, sin # ni ,)", InputType.TYPE_CLASS_TEXT);
        btnSerie = boton("Grabar serie (#SN) y verificar (#GN)", v -> grabarSerie());
        btnFT = boton("Restaurar temperatura de fábrica (#FT#)", v -> restaurarTemperatura());

        titulo("Volver a fábrica");
        spFabrica = new Spinner(this);
        List<String> fa = new ArrayList<>();
        for (char k : Fabrica.CODIGOS) {
            fa.add(k + "  " + Fabrica.nombre(k));
        }
        spFabrica.setAdapter(adaptador(fa));
        panel.addView(spFabrica);
        Button uno = boton("Restaurar este (#F,k#)", v -> confirmarFabrica(false));
        Button todos = boton("Restaurar todos (#F,*#)", v -> confirmarFabrica(true));
        fila(uno, todos);

        titulo("Cambiar PIN");
        edPinActual = campo("PIN actual", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        edPinNuevo = campo("PIN nuevo (4 dígitos)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        boton("Cambiar PIN (#P#)", v -> cambiarPin());

        titulo("Salir");
        boton("Cerrar modo administrador (#Q#)", v -> salir());
        raiz = exterior;
        btnEscribir.setEnabled(false);
    }

    private ArrayAdapter<String> adaptador(List<String> l) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, l);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    /** Modo abierto y sin caducar. */
    private boolean abierto() {
        Sesion s = Sesion.get();
        if (s.adminDesbloqueado && SystemClock.elapsedRealtime() - s.ultimaAlmohadillaMs > CADUCIDAD_MS) {
            s.adminDesbloqueado = false;
            Registro.nota("modo admin caducado (10 min sin tramas #)");
        }
        return s.adminDesbloqueado && EnlaceSerie.instancia().estaConectado();
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (panel == null) {
            return;
        }
        Sesion s = Sesion.get();
        boolean v36 = s.version == Sesion.Version.V36;
        boolean ab = abierto();
        panel.setVisibility(ab ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(v36 && !ab && !ocupado && EnlaceSerie.instancia().estaConectado());
        if (!v36) {
            txtAcceso.setText("Modo administrador desactivado: el firmware detectado es " + s.firmware()
                    + ". Sólo el firmware V3.6 guarda la calibración en EEPROM y acepta órdenes #...#. "
                    + "En un V3 2020 las ecuaciones están en el código: calibrar exige recompilar y grabar por ICSP "
                    + "(PROCEDIMIENTO-Calibracion-V3-K42.md §6).");
        } else if (ab) {
            txtAcceso.setText("Modo administrador ABIERTO. Se cierra con #Q#, al salir de esta pantalla, "
                    + "al apagar el equipo o a los 10 min sin órdenes #.");
            txtAcceso.setBackgroundColor(AMARILLO);
        } else {
            txtAcceso.setText("Modo administrador cerrado." + (s.apto == null ? " Pruebas sin hacer."
                    : (s.apto ? "" : " Equipo NO APTO: entrar exige confirmación.")));
            txtAcceso.setBackgroundColor(GRIS);
        }
        btnEscribir.setEnabled(ab && !ocupado && propuesta != null && propuesta.escribible());
        boolean v362 = s.es362();
        txtCal.setText(s.datosCalibracion() + (v362 ? "" : "\nGrabar serie y restaurar temperatura exigen la 3.6.2."));
        btnSerie.setEnabled(ab && !ocupado && v362);
        Acta acta = s.acta;
        txtActa.setText(acta == null ? "Sin acta: se abre con el primer #S." : acta.texto()
                + (acta.cerrada() ? "" : "\nPara aceptar: " + (acta.motivoNoAceptable() == null ? "listo"
                : acta.motivoNoAceptable())));
        Acta.Codigo ult = acta == null || acta.codigos().isEmpty() ? null : acta.codigos().get(acta.codigos().size() - 1);
        btnRemedida.setEnabled(ab && !ocupado && ult != null && !acta.cerrada());
        btnAceptar.setEnabled(!ocupado && acta != null && !acta.cerrada());
        btnRechazar.setEnabled(ab && !ocupado && acta != null && !acta.cerrada());
        if (ult != null) {
            List<String> et = new ArrayList<>();
            patronesRemedida = new ArrayList<>();
            List<Patron> cat = catalogo();
            if (cat != null) {
                for (Patron p : cat) {
                    if (Asistente.deCodigo(p, ult.k)) {
                        patronesRemedida.add(p);
                        et.add(p.toString());
                    }
                }
            }
            if (spRemedida.getCount() != et.size()) {
                spRemedida.setAdapter(adaptador(et));
            }
        }
        btnFT.setEnabled(ab && !ocupado && v362);
    }

    /** Ejecuta una operacion con el equipo en el hilo de trabajo y muestra el resultado. */
    private void op(String etiqueta, boolean requiereAdmin, Tarea t) {
        if (ocupado) {
            return;
        }
        if (requiereAdmin && !abierto()) {
            alerta("Modo administrador", "El modo está cerrado o ha caducado. Vuelva a entrar con el PIN.");
            refrescar();
            return;
        }
        ocupado = true;
        txtOp.setText(etiqueta + "...");
        refrescar();
        Cliente.instancia().ejecutar(() -> {
            String res;
            try {
                res = t.correr();
            } catch (IOException | InterruptedException e) {
                res = "Error: " + EnlaceSerie.descripcion(e);
            } catch (RuntimeException e) {
                res = "Error interno: " + EnlaceSerie.descripcion(e);
            }
            final String fin = etiqueta + ": " + res;
            Registro.nota(fin);
            enUi(() -> {
                ocupado = false;
                txtOp.setText(fin);
                refrescar();
            });
        });
    }

    /** Analiza #OK# / #ERR,motivo#; cierra el modo si el firmware dice BLOQUEADO. */
    private String resultado(Cliente.Respuesta r) {
        if (Tramas.esOk(r.trama)) {
            return "OK";
        }
        String m = Tramas.motivoError(r.trama);
        if ("BLOQUEADO".equals(m)) {
            Sesion.get().adminDesbloqueado = false;
            return "el equipo responde BLOQUEADO: modo admin cerrado o caducado";
        }
        return m != null ? "ERROR " + m : r.describir();
    }

    // ------------------------------------------------------------------ PIN

    private void entrar() {
        Sesion s = Sesion.get();
        if (s.version != Sesion.Version.V36) {
            refrescar();
            return;
        }
        if (s.fallosPin >= MAX_FALLOS_PIN) {
            alerta("PIN bloqueado", "Van " + s.fallosPin + " fallos: el equipo bloquea #L hasta apagarlo. "
                    + "Apague y encienda el equipo, y vuelva a conectar.");
            return;
        }
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), 0);
        final CheckBox confirma = new CheckBox(this);
        boolean noApto = !Boolean.TRUE.equals(s.apto);
        if (noApto) {
            TextView t = new TextView(this);
            t.setText(s.apto == null ? "Las pruebas del equipo no se han hecho."
                    : "Las pruebas dieron NO APTO:\n" + s.resumenPruebas);
            c.addView(t);
            confirma.setText("Entiendo que el equipo no está apto y quiero entrar igualmente");
            c.addView(confirma);
        }
        final EditText pin = campoPin();
        c.addView(pin);
        new AlertDialog.Builder(this)
                .setTitle(noApto ? "Entrar sin APTO (override)" : "PIN del equipo")
                .setView(c)
                .setPositiveButton("Entrar", (d, w) -> {
                    if (noApto && !confirma.isChecked()) {
                        alerta("Override", "Hay que marcar la confirmación para entrar con el equipo no apto.");
                        return;
                    }
                    String p = pin.getText().toString().trim();
                    if (!p.matches("\\d{4}")) {
                        alerta("PIN", "El PIN son 4 dígitos.");
                        return;
                    }
                    if (noApto) {
                        s.overrideAdmin = true;
                        Registro.nota("OVERRIDE: el operador entra en modo admin con el equipo "
                                + (s.apto == null ? "sin pruebas" : "NO APTO"));
                    }
                    login(p);
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void login(String pin) {
        op("Entrar (#L)", false, () -> {
            Cliente.Respuesta r = Cliente.instancia().pedir("#L," + pin + "#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
            Sesion s = Sesion.get();
            if (Tramas.esOk(r.trama)) {
                s.adminDesbloqueado = true;
                s.fallosPin = 0;
                return "modo administrador abierto";
            }
            String m = Tramas.motivoError(r.trama);
            if ("PIN".equals(m)) {
                s.fallosPin++;
                return "PIN incorrecto (fallo " + s.fallosPin + " de " + MAX_FALLOS_PIN
                        + "; al " + MAX_FALLOS_PIN + "º el equipo bloquea #L hasta apagarlo)";
            }
            if ("BLOQUEADO".equals(m)) {
                s.fallosPin = MAX_FALLOS_PIN;
                return "el equipo tiene #L bloqueado por fallos de PIN: apáguelo y enciéndalo";
            }
            return r.describir();
        });
    }

    private void cambiarPin() {
        String a = edPinActual.getText().toString().trim();
        String n = edPinNuevo.getText().toString().trim();
        if (!a.matches("\\d{4}") || !n.matches("\\d{4}")) {
            alerta("PIN", "Los dos PIN son de 4 dígitos.");
            return;
        }
        new AlertDialog.Builder(this).setTitle("Cambiar PIN")
                .setMessage("Se cambiará el PIN del equipo. Anote el nuevo: la app no lo guarda.")
                .setPositiveButton("Cambiar", (d, w) -> op("Cambiar PIN (#P)", true, () -> {
                    Cliente.Respuesta r = Cliente.instancia().pedir("#P," + a + "," + n + "#",
                            Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
                    return resultado(r);
                }))
                .setNegativeButton("Cancelar", null).show();
        edPinActual.setText("");
        edPinNuevo.setText("");
    }

    private void salir() {
        op("Cerrar modo admin (#Q#)", false, () -> {
            Cliente.Respuesta r = Cliente.instancia().pedir("#Q#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
            Sesion.get().adminDesbloqueado = false;
            return resultado(r);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Al salir de la pantalla se cierra el modo (RF-APP-19).
        if (Sesion.get().adminDesbloqueado && EnlaceSerie.instancia().estaConectado()) {
            Sesion.get().adminDesbloqueado = false;
            Cliente.instancia().ejecutar(() -> {
                try {
                    Cliente.instancia().pedir("#Q#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
                } catch (IOException | InterruptedException e) {
                    Registro.nota("#Q# al salir: " + EnlaceSerie.descripcion(e));
                }
            });
        }
    }

    // --------------------------------------------------------- coeficientes

    /** Lee #G x12 y #GT# y guarda copia. Llamar desde el hilo de trabajo. */
    private String leerTodo(String motivo) throws IOException, InterruptedException {
        Sesion s = Sesion.get();
        StringBuilder sb = new StringBuilder();
        int fallos = 0;
        for (int i = 0; i < Fabrica.CODIGOS.length; i++) {
            char k = Fabrica.CODIGOS[i];
            Cliente.Respuesta r = Cliente.instancia().pedir("#G," + k + "#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
            Ecuacion e = r.valida() ? Tramas.parsearG(r.trama, k) : null;
            if (e == null) {
                fallos++;
                sb.append(k).append(": ").append(r.describir()).append('\n');
                continue;
            }
            s.leidas[i] = e;
            sb.append(k).append(' ').append(Tramas.coeficiente(e.c3)).append(' ').append(Tramas.coeficiente(e.c2))
                    .append(' ').append(Tramas.coeficiente(e.c1)).append(' ').append(Tramas.coeficiente(e.c0))
                    .append(e.igualFloat32(Fabrica.ecuacion(k)) ? "  = fábrica" : "  AJUSTADA").append('\n');
        }
        Cliente.Respuesta rt = Cliente.instancia().pedir("#GT#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
        double[] t = rt.valida() ? Tramas.parsearGT(rt.trama) : null;
        if (t != null) {
            s.temperatura = t;
            sb.append("T ").append(Tramas.coeficiente(t[0])).append(' ').append(Tramas.coeficiente(t[1]))
                    .append(' ').append(Tramas.coeficiente(t[2])).append('\n');
        } else {
            fallos++;
            sb.append("#GT#: ").append(rt.describir()).append('\n');
        }
        s.guardarCoeficientes(this, motivo);
        final String tabla = "código c3 c2 c1 c0\n" + sb;
        enUi(() -> txtCoef.setText(tabla));
        return fallos == 0 ? "12 juegos y temperatura leídos; copia guardada" : fallos + " lecturas fallidas";
    }

    private void leerCoeficientes(String motivo) {
        op("Leer coeficientes", false, () -> leerTodo(motivo));
    }

    // -------------------------------------------------------------- ajuste

    private char codigoAjuste() {
        int i = spAjuste.getSelectedItemPosition();
        return Fabrica.CODIGOS[Math.max(0, i)];
    }

    private List<Patron> catalogo() {
        try {
            return Sesion.get().patrones(this);
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    /** Lee los parametros del oscuro y de s_rep de la pantalla. */
    private boolean leerParametros() {
        try {
            Asistente.X_OSCURO = Double.parseDouble(edXosc.getText().toString().trim());
            Asistente.OSCURO_MARGEN = Double.parseDouble(edOscMargen.getText().toString().trim());
            Asistente.OSCURO_MINIMO = Double.parseDouble(edOscMin.getText().toString().trim());
            Asistente.S_REP_REL = Double.NaN;
            Campana c = Campanas.abierta();
            if (c != null) {
                try (java.io.InputStream in = getAssets().open(BancoCola.ASSET)) {
                    Anclas.Valor v = Anclas.sRep(BancoCola.cargar(ImportadorCampana.leer(in)), c);
                    Asistente.S_REP_REL = v.valor;
                    txtSrep.setText(v.texto);
                } catch (IOException | RuntimeException e) {
                    txtSrep.setText("s_rep: cola del banco no admitida (" + e.getMessage() + ")");
                }
            } else {
                txtSrep.setText("s_rep: sin campaña de este equipo, no conocido");
            }
            return true;
        } catch (NumberFormatException e) {
            alerta("Parámetros", "Revise x de oscuro, margen, mínimo y s_rep.");
            return false;
        }
    }

    private void ajustar() {
        if (!leerParametros()) {
            return;
        }
        char k = codigoAjuste();
        int sel = rgGrado.getCheckedRadioButtonId();
        int grado = sel == rgGrado.getChildAt(1).getId() ? 2 : (sel == rgGrado.getChildAt(2).getId()
                ? Asistente.GRADO_ANCLADA : 1);
        if (grado == Asistente.GRADO_ANCLADA) {
            // El ancla sale de la serie OSCURO de la campana de este equipo (superficie negra mate, cert. 0).
            Campana c = Campanas.abierta();
            Campana.Serie so = c != null && c.esDeEsteEquipo(Sesion.get().mac) ? c.serieOscuro() : null;
            if (so == null) {
                alerta("Recta anclada", "Falta la serie OSCURO de la campaña de este equipo (preajuste OSCURO, 5 × 9). "
                        + "Sin ella no hay ancla.");
                return;
            }
            Asistente.X_ANCLA = so.media();
            Asistente.R_ANCLA = 0;
            Asistente.ORIGEN_ANCLA = String.format(Locale.US, "serie OSCURO %s de la campaña, %d colocaciones",
                    so.id, so.colocaciones().size());
        }
        Sesion s = Sesion.get();
        if (s.leidas[Fabrica.indice(k)] == null) {
            alerta("Coeficientes", "Lea antes los coeficientes del equipo: el estado \"como llegó\" se calcula con ellos.");
            return;
        }
        List<Patron> cat = catalogo();
        if (cat == null) {
            alerta("Patrones", "No se pudo leer el catálogo de patrones: sin él no se ajusta.");
            return;
        }
        propuesta = Asistente.proponer(k, grado, s.medidasParaAjuste(), s.ecuacionVigente(k), cat);
        txtAjuste.setText("Puntos: " + s.origenAjuste() + "\n" + propuesta.informe);
        Registro.nota("asistente: " + propuesta.informe);
        refrescar();
    }

    private void confirmarEscritura() {
        final Asistente.Propuesta p = propuesta;
        if (p == null || !p.escribible()) {
            return;
        }
        final char k = p.codigo;
        final Tramas.TramaS ts = Tramas.tramaS(k, p.ajuste.ecuacion);
        if (ts == null) {
            alerta("Trama", "La trama #S no cabe en " + Tramas.MAX_TRAMA + " bytes.");
            return;
        }
        String fw = Asistente.criterioFirmwareS(ts.enviada);
        if (fw != null) {
            // Mismo criterio que #S del firmware 3.6.1: no se envia nada que vaya a dar #ERR sin explicacion.
            alerta("No se escribe", fw);
            return;
        }
        Ecuacion ant = Sesion.get().ecuacionVigente(k);
        Ecuacion nue = ts.enviada;
        StringBuilder m = new StringBuilder();
        m.append("Código ").append(k).append(" (").append(Fabrica.nombre(k)).append(")\n\n");
        m.append("         actual            nuevo\n");
        String[] nom = {"c3", "c2", "c1", "c0"};
        double[] a = ant.comoVector();
        double[] n = nue.comoVector();
        for (int i = 0; i < 4; i++) {
            m.append(nom[i]).append("  ").append(Tramas.coeficiente(a[i])).append("  ")
                    .append(Tramas.coeficiente(n[i])).append('\n');
        }
        m.append("\nR sobre los patrones (actual -> nuevo, certificado):\n");
        for (Asistente.Punto q : p.puntos) {
            m.append(String.format(Locale.US, "%s: %.0f -> %.0f  (%.0f)\n", q.patron.nombre,
                    (double) ant.respuestaFloat32((int) Math.round(q.x)),
                    (double) nue.respuestaFloat32((int) Math.round(q.x)), q.patron.valor));
        }
        m.append("\nTrama: ").append(ts.texto).append("\n\nAntes de escribir se guarda una copia de los 12 juegos y de la temperatura. "
                + "Ajuste contra patrones, no calibración trazable.");
        new AlertDialog.Builder(this).setTitle("Confirmar escritura en EEPROM")
                .setMessage(m.toString())
                .setPositiveButton("Escribir", (d, w) -> conformidadYEscribir(k, ts, p))
                .setNegativeButton("Cancelar", null).show();
    }

    /**
     * Si la curva incumple RF-CAL-14/15/16 (propuestos), la app avisa pero no bloquea: pide la
     * conformidad del superadministrador con una nota, que queda en el acta (3.6.9, decision de
     * Diego para el blanco grado 1).
     */
    private void conformidadYEscribir(char k, Tramas.TramaS ts, Asistente.Propuesta p) {
        if (p.incumplimientos.isEmpty()) {
            escribir(k, ts, p.metodo, "");
            return;
        }
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), 0);
        TextView t = new TextView(this);
        StringBuilder sb = new StringBuilder("La curva incumple criterios propuestos (no bloquean):\n");
        for (String s : p.incumplimientos) {
            sb.append("- ").append(s).append('\n');
        }
        t.setText(sb.toString());
        c.addView(t);
        final CheckBox ok = new CheckBox(this);
        ok.setText("El superadministrador da su conformidad");
        c.addView(ok);
        final EditText nota = new EditText(this);
        nota.setHint("Nota obligatoria (quién y por qué)");
        c.addView(nota);
        new AlertDialog.Builder(this).setTitle("Conformidad del superadministrador").setView(c)
                .setPositiveButton("Escribir", (d, w) -> {
                    String n = nota.getText().toString().trim();
                    if (!ok.isChecked() || n.isEmpty()) {
                        alerta("Conformidad", "Hace falta marcar la conformidad y escribir la nota.");
                        return;
                    }
                    escribir(k, ts, p.metodo, "Conformidad del superadministrador pese a incumplir "
                            + String.join("; ", p.incumplimientos) + ". Nota: " + n);
                })
                .setNegativeButton("Cancelar", null).show();
    }

    /** Relee #G,k#; null si no se pudo. */
    private Ecuacion releer(char k) throws IOException, InterruptedException {
        Cliente.Respuesta g = Cliente.instancia().pedir("#G," + k + "#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
        Ecuacion e = g.valida() ? Tramas.parsearG(g.trama, k) : null;
        if (e != null) {
            Sesion.get().leidas[Fabrica.indice(k)] = e;
        }
        return e;
    }

    /**
     * Condicion C3: si la relectura no coincide con lo escrito, se vuelve al
     * estado anterior: #F,k# si el anterior era el de fabrica (RF-APP-18: nunca
     * se reescribe fabrica con #S); si no, #S con los coeficientes anteriores.
     */
    private String restaurar(char k, Ecuacion anterior) throws IOException, InterruptedException {
        boolean eraFabrica = anterior.igualFloat32(Fabrica.ecuacion(k));
        String trama;
        if (eraFabrica) {
            trama = "#F," + k + "#";
        } else {
            Tramas.TramaS ts = Tramas.tramaS(k, anterior);
            if (ts == null) {
                return "no se pudo formar la trama de restauración";
            }
            trama = ts.texto;
        }
        Cliente.Respuesta r = Cliente.instancia().pedir(trama, Tramas.Tipo.ADMIN, 5000);
        String res = resultado(r);
        Ecuacion e = releer(k);
        // #F repone el valor de ROM (misma impresion: ULP_G). #S con los valores
        // anteriores pasa otra vez por strtod: impresion + strtod + impresion.
        boolean ok = e != null && e.igualFloat32(anterior, eraFabrica ? Ecuacion.ULP_G
                : Ecuacion.ULP_S + Ecuacion.ULP_G);
        return "restauración con " + (eraFabrica ? "#F," + k + "#" : "#S (valores anteriores)") + " -> " + res
                + (ok ? "; la relectura coincide con el estado anterior" : "; la relectura NO coincide con el estado anterior: "
                + (e == null ? "sin relectura" : e.toString()));
    }

    /**
     * Comprobacion por evaluacion tras #S (la que importa de verdad): #E,k,x en
     * x = 500, 1000, 2000, 3000, 4000 tiene que dar lo mismo que la curva que la
     * app queria escribir, emulada en float32, con +/-1 cuenta.
     * @return null si cuadra; si no, el detalle.
     */
    private String comprobarPorE(char k, Ecuacion enviada) throws IOException, InterruptedException {
        StringBuilder mal = new StringBuilder();
        for (int x : Pruebas.X_COMPROBACION) {
            Cliente.Respuesta r = Cliente.instancia().pedir("#E," + k + "," + x + "#", Tramas.Tipo.ADMIN,
                    Cliente.TIMEOUT_ADMIN_MS);
            Integer v = r.valida() ? Tramas.parsearE(r.trama, k) : null;
            int esperado = enviada.respuestaFloat32(x);
            if (v == null) {
                mal.append(" x=").append(x).append(": ").append(r.describir()).append(';');
            } else if (Math.abs(v - esperado) > 1) {
                mal.append(" x=").append(x).append(": #E=").append(v).append(", esperado ").append(esperado).append(';');
            }
        }
        return mal.length() == 0 ? null : "#E no reproduce la curva enviada:" + mal;
    }

    private void escribir(char k, Tramas.TramaS ts, String metodo, String conformidad) {
        op("Escribir código " + k, true, () -> {
            Sesion ses = Sesion.get();
            if (ses.acta == null || ses.acta.cerrada()) {
                Acta en = Campanas.actaEnCurso(this);
                if (en == null) {
                    Campanas.adjuntarActa(this, nuevaActa());
                }
                Registro.nota("acta abierta:\n" + ses.acta.texto());
            }
            String no = ses.acta.motivoNoEscribir(k);
            if (no != null) {
                return "No se escribe: " + no;
            }
            String copia = leerTodo("copia antes de escribir el codigo " + k);
            Registro.nota("copia previa: " + copia);
            Ecuacion anterior = Sesion.get().leidas[Fabrica.indice(k)];
            if (anterior == null) {
                return "no se pudo leer el estado anterior del código " + k + ": no se escribe nada";
            }
            ses.acta.escribiendo(k, anterior, ts.enviada);
            Cliente.Respuesta r = Cliente.instancia().pedir(ts.texto, Tramas.Tipo.ADMIN, 5000);
            String res = resultado(r);
            Ecuacion e = releer(k);
            String estado = e == null ? "la relectura #G falló"
                    : (e.igualFloat32(ts.enviada, Ecuacion.ULP_S) ? "la relectura coincide con lo enviado"
                    : (e.igualFloat32(anterior) ? "la relectura coincide con el estado ANTERIOR"
                    : "la relectura no coincide ni con lo enviado ni con lo anterior: " + e));
            String salida;
            if (!"OK".equals(res)) {
                // Ante #ERR o sin respuesta no se puede afirmar que no haya cambiado nada.
                salida = "#S -> " + res + ". Estado tras releer: " + estado + ".";
                if (e == null || !e.igualFloat32(anterior)) {
                    salida += " Se restaura el estado anterior: " + restaurar(k, anterior);
                }
            } else if (e != null && e.igualFloat32(ts.enviada, Ecuacion.ULP_S)) {
                String porE = comprobarPorE(k, ts.enviada);
                if (porE == null) {
                    Acta acta = Sesion.get().acta;
                    String tramaG = "#G," + k + "," + Tramas.coeficiente(e.c3) + "," + Tramas.coeficiente(e.c2) + ","
                            + Tramas.coeficiente(e.c1) + "," + Tramas.coeficiente(e.c0) + "#";
                    Ecuacion fab = Fabrica.ecuacion(k);
                    String osc = String.format(Locale.US, "Oscuro (x = %.0f): R %.1f (fábrica %.1f), valor que acepta Diego (P9-B13)",
                            Asistente.X_OSCURO, e.evaluar(Asistente.X_OSCURO), fab.evaluar(Asistente.X_OSCURO));
                    acta.escrito(k, tramaG, e, osc, metodo, conformidad);
                    acta.dato("#V# posterior", OpsEquipo.leerV());
                    salida = "#S OK y " + estado + " (" + Ecuacion.ULP_S + " ulp); #E en 5 puntos coincide con la curva "
                            + "enviada (±1). Ahora: re-medida de verificación del código " + k + " (RF-CAL-18). "
                            + "No se escribe otro código antes (P9-B8). La fecha de calibración se graba al aceptar el acta.";
                } else {
                    salida = "#S OK pero " + porE + ". Se restaura el estado anterior: " + restaurar(k, anterior);
                }
            } else {
                salida = "#S OK pero " + estado + ". Se restaura el estado anterior: " + restaurar(k, anterior);
            }
            if (!salida.startsWith("#S OK y") && Sesion.get().acta.escribiendo() == k) {
                Sesion.get().acta.sinEscribir(k, salida);
            }
            Sesion.get().guardarCoeficientes(this, "despues de escribir el codigo " + k);
            final String aviso = salida;
            if (!salida.startsWith("#S OK y")) {
                enUi(() -> alerta("Escritura del código " + k, aviso));
            }
            return salida;
        });
    }

    // ------------------------------------------------------------------ acta (3.6.8)

    /** Acta nueva con el protocolo de la campana de este equipo (P9-B3). */
    private Acta nuevaActa() {
        Sesion s = Sesion.get();
        int[] pr = {1, 9};
        Campana c = Campanas.abierta();
        if (c != null && c.esDeEsteEquipo(s.mac)) {
            pr = c.protocolo();
        }
        return new Acta(s.serie(), s.mac, s.firmware(), pr[0], pr[1], s.disparosAsentamiento, Sesion.ahoraIso());
    }

    /**
     * RF-CAL-18: re-medida del ultimo codigo escrito sobre un patron de su clase, con el
     * protocolo fijo del acta. En cada disparo: 'e' (x) y el codigo (R).
     */
    private void remedida() {
        final Sesion s = Sesion.get();
        final Acta acta = s.acta;
        if (acta == null || acta.codigos().isEmpty() || !leerParametros()) {
            return;
        }
        final Acta.Codigo cod = acta.codigos().get(acta.codigos().size() - 1);
        int pos = spRemedida.getSelectedItemPosition();
        if (pos < 0 || pos >= patronesRemedida.size()) {
            alerta("Re-medida", "Elija un patrón de la clase del código " + cod.k + ".");
            return;
        }
        final Patron p = patronesRemedida.get(pos);
        new AlertDialog.Builder(this).setTitle("Re-medida del código " + cod.k)
                .setMessage("Coloque " + p + ". Protocolo del acta: " + acta.colocaciones + " colocaciones × "
                        + acta.disparos + " disparos, " + acta.asentamiento + " de asentamiento.")
                .setPositiveButton("OK", (d, w) -> {
                    ocupado = true;
                    refrescar();
                    Cliente.instancia().ejecutar(() -> colocacionRemedida(acta, cod, p, 1,
                            new ArrayList<double[]>(), new ArrayList<double[]>()));
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void colocacionRemedida(Acta acta, Acta.Codigo cod, Patron p, int k, List<double[]> rs, List<double[]> xs) {
        Sesion s = Sesion.get();
        String error = null;
        try {
            for (int i = 0; i < acta.asentamiento; i++) {
                Cliente.Respuesta a = Cliente.instancia().pedir("e", Tramas.Tipo.MEDIDA, Cliente.TIMEOUT_MEDIDA_MS);
                Registro.nota("re-medida: disparo de asentamiento, descartado: " + a.describir());
            }
            List<Double> r = new ArrayList<>();
            List<Double> x = new ArrayList<>();
            for (int i = 0; i < acta.disparos; i++) {
                Cliente.Respuesta re = Cliente.instancia().pedir("e", Tramas.Tipo.MEDIDA, Cliente.TIMEOUT_MEDIDA_MS);
                Cliente.Respuesta rk = Cliente.instancia().pedir(String.valueOf(cod.k), Tramas.Tipo.MEDIDA,
                        Cliente.TIMEOUT_MEDIDA_MS);
                Integer vx = re.valida() ? Tramas.valorMedida(re.trama) : null;
                Integer vr = rk.valida() ? Tramas.valorMedida(rk.trama) : null;
                if (vx != null && vx > 0 && vr != null) {
                    x.add((double) vx);
                    r.add((double) vr);
                } else {
                    Registro.nota("re-medida: par no válido: e -> " + re.describir() + ", " + cod.k + " -> " + rk.describir());
                }
            }
            xs.add(Estadistica.aVector(x));
            rs.add(Estadistica.aVector(r));
        } catch (IOException | InterruptedException | RuntimeException e) {
            error = EnlaceSerie.descripcion(e);
        }
        final String ferr = error;
        enUi(() -> {
            if (ferr == null && k < acta.colocaciones) {
                new AlertDialog.Builder(this).setTitle("Colocación " + (k + 1) + " de " + acta.colocaciones)
                        .setMessage("Levante el equipo y vuelva a apoyarlo sobre " + p.nombre + ". Pulse OK cuando esté apoyado.")
                        .setCancelable(false)
                        .setPositiveButton("OK", (d, w) -> Cliente.instancia().ejecutar(
                                () -> colocacionRemedida(acta, cod, p, k + 1, rs, xs)))
                        .show();
                return;
            }
            ocupado = false;
            if (ferr != null) {
                alerta("Re-medida", "Interrumpida: " + ferr + ". No cuenta; repítala.");
            } else {
                Acta.Remedida rm = Acta.evaluarRemedida(p.nombre, rs, xs, cod.leida, Asistente.S_REP_REL);
                acta.remedida(cod.k, rm);
                Registro.nota("re-medida código " + cod.k + ": " + rm.texto);
                alerta("Re-medida del código " + cod.k, rm.texto);
            }
            refrescar();
        });
    }

    private void aceptarActa() {
        final Acta acta = Sesion.get().acta;
        if (acta == null || acta.motivoNoAceptable() != null) {
            return;
        }
        new AlertDialog.Builder(this).setTitle("Aceptar el acta")
                .setMessage(acta.texto() + "\nAl aceptar se graba la fecha de calibración de hoy (#SC), una sola vez.")
                .setPositiveButton("Aceptar y grabar fecha", (d, w) -> op("Aceptar acta", true, () -> {
                    String t = grabarFechaHoy();
                    acta.aceptar(Sesion.ahoraIso(), ultimaFechaGrabada);
                    guardarActa(acta);
                    return t;
                }))
                .setNegativeButton("Cancelar", null).show();
    }

    private void rechazarActa() {
        final Acta acta = Sesion.get().acta;
        if (acta == null || acta.cerrada()) {
            return;
        }
        final EditText e = new EditText(this);
        e.setHint("Motivo");
        new AlertDialog.Builder(this).setTitle("Rechazar el acta").setView(e)
                .setMessage("No se grabará fecha de calibración. Lo escrito sigue en el equipo: restaure a fábrica "
                        + "si no debe quedar.")
                .setPositiveButton("Rechazar", (d, w) -> {
                    acta.rechazar(Sesion.ahoraIso(), e.getText().toString().trim());
                    guardarActa(acta);
                    refrescar();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void guardarActa(Acta acta) {
        try {
            java.io.File f = Campanas.guardarActa(this, acta.texto());
            Registro.nota("acta guardada en " + f.getName() + ":\n" + acta.texto());
        } catch (IOException e) {
            Registro.nota("no se pudo guardar el acta: " + e.getMessage() + "\n" + acta.texto());
        }
    }

    // ------------------------------------------------------ serie y fecha (3.6.2)

    /**
     * Tras una calibracion verificada: #SC con la fecha de hoy y relectura #GC.
     * Llamar desde el hilo de trabajo. Devuelve el texto para el operador.
     */
    private volatile String ultimaFechaGrabada;

    private String grabarFechaHoy() throws IOException, InterruptedException {
        ultimaFechaGrabada = null;
        Sesion s = Sesion.get();
        if (!s.es362()) {
            return "Firmware " + s.firmware() + ": no tiene #SC, la fecha de calibración NO se graba.";
        }
        String hoy = Calibracion.hoy();
        Cliente.Respuesta r = Cliente.instancia().pedir("#SC," + hoy + "#", Tramas.Tipo.ADMIN, 5000);
        String res = resultado(r);
        Cliente.Respuesta g = Cliente.instancia().pedir("#GC#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
        String leida = Calibracion.fechaDe(g.trama);
        if (leida != null) {
            s.fechaCalibracion = leida;
        }
        if ("OK".equals(res) && hoy.equals(leida)) {
            ultimaFechaGrabada = hoy;
            return "Fecha de calibración " + hoy + " grabada y verificada con #GC# (vence "
                    + Calibracion.vencimiento(hoy) + ").";
        }
        return "ATENCIÓN: la fecha de calibración no quedó grabada (#SC -> " + res + ", #GC# -> "
                + g.describir() + ").";
    }

    private void grabarSerie() {
        final String serie = edSerie.getText().toString().trim();
        String mal = Calibracion.motivoSerieInvalida(serie);
        if (mal != null) {
            alerta("Serie no válida", mal);
            return;
        }
        // P10-C6: doble entrada solo en el alta; casilla si difiere del nombre Bluetooth; aviso si ya tenia serie.
        Sesion ses = Sesion.get();
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        final EditText repite = new EditText(this);
        repite.setHint("Repita la serie");
        caja.addView(repite);
        final android.widget.CheckBox distinta = new android.widget.CheckBox(this);
        final boolean difiere = ses.nombre == null || !ses.nombre.contains(serie);
        distinta.setText("La serie no coincide con el nombre Bluetooth (" + ses.nombre + "): es correcta");
        if (difiere) {
            caja.addView(distinta);
        }
        boolean yaTiene = ses.serieEquipo != null && !Calibracion.NONE.equals(ses.serieEquipo);
        new AlertDialog.Builder(this).setTitle("Alta de la serie")
                .setMessage((yaTiene ? "ATENCIÓN: el equipo ya tiene serie (" + ses.serieEquipo + "); se sustituye.\n\n" : "")
                        + "Se grabará la serie \"" + serie + "\" en la EEPROM del equipo (actual: "
                        + ses.serieEquipo + ").\n\nTrama: #SN," + serie + "#")
                .setView(caja)
                .setPositiveButton("Grabar", (d, w) -> {
                    if (!serie.equals(repite.getText().toString().trim())) {
                        alerta("Serie", "Las dos entradas no coinciden: no se graba.");
                        return;
                    }
                    if (difiere && !distinta.isChecked()) {
                        alerta("Serie", "Confirme que la serie es correcta aunque difiera del nombre Bluetooth.");
                        return;
                    }
                    op("Grabar serie", true, () -> {
                    Cliente.Respuesta r = Cliente.instancia().pedir("#SN," + serie + "#", Tramas.Tipo.ADMIN, 5000);
                    String res = resultado(r);
                    Cliente.Respuesta g = Cliente.instancia().pedir("#GN#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
                    String leida = g.valida() ? Calibracion.serieDe(g.trama) : null;
                    if (leida != null) {
                        Sesion.get().serieEquipo = leida;
                    }
                    return "#SN -> " + res + "; #GN# -> " + g.describir()
                            + (serie.equals(leida) ? " (verificada)" : " (NO coincide con lo enviado)");
                    });
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void restaurarTemperatura() {
        double[] t = Sesion.get().temperatura;
        new AlertDialog.Builder(this).setTitle("Restaurar temperatura de fábrica")
                .setMessage("El factor de temperatura vuelve a los valores de fábrica (gui.c:42-44). Actual: "
                        + (t == null ? "sin leer" : Tramas.coeficiente(t[0]) + ", " + Tramas.coeficiente(t[1]) + ", "
                        + Tramas.coeficiente(t[2])) + ".\n\nTrama: #FT#")
                .setPositiveButton("Restaurar", (d, w) -> op("Restaurar temperatura (#FT#)", true, () -> {
                    Cliente.Respuesta r = Cliente.instancia().pedir("#FT#", Tramas.Tipo.ADMIN, 5000);
                    String res = resultado(r);
                    Cliente.Respuesta gt = Cliente.instancia().pedir("#GT#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
                    double[] nt = gt.valida() ? Tramas.parsearGT(gt.trama) : null;
                    if (nt != null) {
                        Sesion.get().temperatura = nt;
                    }
                    return "#FT# -> " + res + "; #GT# -> " + gt.describir();
                }))
                .setNegativeButton("Cancelar", null).show();
    }

    // ------------------------------------------------------------- fabrica

    private void confirmarFabrica(boolean todos) {
        int i = Math.max(0, spFabrica.getSelectedItemPosition());
        final char k = Fabrica.CODIGOS[i];
        final String trama = todos ? "#F,*#" : "#F," + k + "#";
        new AlertDialog.Builder(this).setTitle("Volver a fábrica")
                .setMessage((todos ? "Los 12 códigos" : "El código " + k + " (" + Fabrica.nombre(k) + ")")
                        + " volverán a las ecuaciones de fábrica de 2020. Antes se guarda una copia de los actuales.\n\nTrama: " + trama)
                .setPositiveButton("Restaurar", (d, w) -> op("Restaurar " + trama, true, () -> {
                    leerTodo("copia antes de " + trama);
                    Cliente.Respuesta r = Cliente.instancia().pedir(trama, Tramas.Tipo.ADMIN, 5000);
                    String res = resultado(r);
                    if ("OK".equals(res)) {
                        leerTodo("despues de " + trama);
                    }
                    // P10 hallazgo 9: un #F tras la conformidad deja el acta sin valor (lo certificado ya no esta).
                    Acta acta = Sesion.get().acta;
                    if (acta != null && !acta.cerrada() && (todos || acta.codigo(k) != null)) {
                        acta.invalidar(trama + " -> " + res + " después de abrir el acta");
                        res += ". El acta en curso queda INVALIDADA: recházela";
                    }
                    return res;
                }))
                .setNegativeButton("Cancelar", null).show();
    }
}
