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
        // 3.6.13 (QA-3612-08, P11-M4): Avanzado ya no escribe curvas. Se escriben solo en "Calibrar este
        // equipo", con la tabla RF-CAL-37, el oscuro de la sesion, D-20 y el acta.
        btnEscribir = boton("Escribir: sólo en Calibrar este equipo",
                v -> startActivity(new android.content.Intent(this, CalibrarActivity.class)));
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
        btnRechazar = boton("Rechazar acta: en Calibrar este equipo (restaura lo escrito)",
                v -> startActivity(new android.content.Intent(this, CalibrarActivity.class)));
        fila(btnAceptar, btnRechazar);
        txtActa = texto("");
        txtActa.setTypeface(Typeface.MONOSPACE);

        titulo("Serie y fecha de calibración (firmware 3.6.2)");
        txtCal = texto("");
        edSerie = campo("Serie del equipo (1-12 caracteres, sin # ni ,)", InputType.TYPE_CLASS_TEXT);
        btnSerie = boton("Alta / Cambiar serie (#SN, verificada con #GN)", v -> grabarSerie());
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
        boolean v36 = s.administra() && s.protocolo.calibra();
        boolean ab = abierto();
        panel.setVisibility(ab ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(v36 && !ab && !ocupado && EnlaceSerie.instancia().estaConectado());
        if (!v36) {
            txtAcceso.setText("Modo administrador desactivado: el firmware detectado es " + s.firmware()
                    + (s.protocolo != null && !s.protocolo.calibra() ? ". " + s.protocolo.motivoNoCalibra() : "")
                    + ". Sólo el firmware V3.6 (y la V4.6, cuando exista) guarda la calibración en EEPROM y acepta órdenes #...#. "
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
        btnEscribir.setEnabled(!ocupado);
        boolean v362 = s.es362();
        txtCal.setText(s.datosCalibracion() + (v362 ? "" : "\nGrabar serie y restaurar temperatura exigen la 3.6.2."));
        btnSerie.setEnabled(ab && !ocupado && v362);
        Acta acta = s.acta;
        txtActa.setText(acta == null ? "Sin acta en curso (el acta se lleva en Calibrar este equipo)." : acta.texto()
                + (acta.cerrada() ? "" : "\nPara aceptar (en Calibrar este equipo): "
                + (acta.motivoNoAceptableSalvoVerificacionFinal() == null ? "falta sólo la verificación final"
                : acta.motivoNoAceptableSalvoVerificacionFinal())));
        Acta.Codigo ult = acta == null || acta.codigos().isEmpty() ? null : acta.codigos().get(acta.codigos().size() - 1);
        btnRemedida.setEnabled(ab && !ocupado && ult != null && !acta.cerrada());
        btnAceptar.setEnabled(!ocupado && acta != null && !acta.cerrada());
        btnRechazar.setEnabled(!ocupado && acta != null && !acta.cerrada());
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
        if (!s.administra() || !s.protocolo.calibra()) {
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

    // 3.6.14 (QA-3613-09, P12 §5.3): Avanzado ya no escribe curvas ni lleva actas. Se borro el codigo muerto
    // de escritura, re-medida y aceptacion/rechazo de actas: todo eso vive en "Calibrar este equipo".

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
        repite.setSingleLine(true);
        repite.setHint("Repita la serie");
        caja.addView(repite);
        final EditText operador = new EditText(this);
        operador.setSingleLine(true);
        operador.setHint("Quién cambia la serie (queda en el diario)");
        final android.widget.CheckBox distinta = new android.widget.CheckBox(this);
        final boolean difiere = ses.nombre == null || !ses.nombre.contains(serie);
        distinta.setText("La serie no coincide con el nombre Bluetooth (" + ses.nombre + "): es correcta");
        if (difiere) {
            caja.addView(distinta);
        }
        // P14-S03: con una campana de este equipo abierta, siempre por "Cambiar serie" (RENOMBRA), nunca por "Alta".
        Campana abiertaC = Campanas.abierta();
        final boolean yaTiene = (ses.serieEquipo != null && !Calibracion.NONE.equals(ses.serieEquipo))
                || (abiertaC != null && abiertaC.mac.equalsIgnoreCase(ses.mac == null ? "" : ses.mac));
        if (yaTiene) {
            caja.addView(operador);
        }
        new AlertDialog.Builder(this).setTitle(yaTiene ? "Cambiar serie" : "Alta de la serie")
                .setMessage((yaTiene ? "Serie actual: " + ses.serieEquipo + ". Nueva: " + serie + ". La campaña, el banco, las "
                        + "actas y las decisiones de " + ses.serieEquipo + " siguen valiendo para este equipo (misma MAC); "
                        + "el cambio queda en el diario (RENOMBRA).\n\n" : "")
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
                    if (yaTiene) {
                        final String quien = operador.getText().toString().trim();
                        final String rep2 = repite.getText().toString().trim();
                        op("Cambiar serie", true, () -> {
                            Sesion s2 = Sesion.get();
                            Campana c = Campanas.abrir(this, s2.serie(), s2.mac);
                            String t = FlujoCalibracion.renombrarSerie(Cliente.instancia(), c, null, serie, rep2, quien,
                                    Sesion.ahoraIso(), Campanas.actaEnCurso(this) != null);
                            if (t.startsWith("Serie cambiada") || t.startsWith("El equipo ya tiene la serie")) {
                                s2.serieEquipo = serie;
                                Campanas.abrir(this, serie, s2.mac);   // misma campana, por el RENOMBRA
                            }
                            return t;
                        });
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
                    // P10-C5 y P11 §3 caso 3: con un acta abierta (en disco, aunque se haya reconectado) cualquier
                    // #F la invalida. Mismo codigo que prueban las pruebas JVM.
                    final AdminActivity yo = this;
                    String res = FlujoCalibracion.fabricaDesdeAvanzado(Cliente.instancia(), new FlujoCalibracion.AlmacenActa() {
                        @Override
                        public Acta enCurso() throws IOException {
                            return Campanas.actaEnCurso(yo);
                        }

                        @Override
                        public void adjuntar(Acta a) {
                        }

                        @Override
                        public void cerrar(Acta a) {
                        }

                        @Override
                        public boolean aceptadoAntes(char c) {
                            return false;
                        }
                    }, null, todos, k);
                    leerTodo("despues de " + trama);
                    return res;
                }))
                .setNegativeButton("Cancelar", null).show();
    }
}
