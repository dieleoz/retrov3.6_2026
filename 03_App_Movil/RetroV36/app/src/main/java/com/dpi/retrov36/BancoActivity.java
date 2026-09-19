package com.dpi.retrov36;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Medir el banco" (3.6.10, corte A; RF-APP-33, RF-CAL-35). Sigue la cola del APK paso a paso:
 * calentamiento, bateria (9), OSCURO, A5, patrones por color y exportar. Cada paso queda en el
 * diario de la campana (evento PASO): al volver, sigue en el primer paso pendiente. Los pasos de
 * control no se saltan; los patrones si, y quedan marcados.
 */
public class BancoActivity extends Base {

    private BancoCola cola;
    private Campana campana;
    private TextView txtAvance;
    private TextView txtPaso;
    private TextView txtResultado;
    private Button btnOk;
    private Button btnSaltar;
    private BancoCola.Paso paso;
    private volatile boolean ocupado;
    /** Ultimo paso saltado que se ofrecio al final (QA-3610-03). */
    private int ultimoOfrecido;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        txtAvance = texto("");
        txtAvance.setTypeface(Typeface.DEFAULT_BOLD);
        txtPaso = texto("");
        txtPaso.setTextSize(24);
        txtPaso.setTypeface(Typeface.DEFAULT_BOLD);
        btnOk = boton("OK", v -> ok());
        btnSaltar = boton("Saltar este patrón (queda marcado)", v -> saltar());
        fila(btnOk, btnSaltar);
        txtResultado = texto("");
        txtResultado.setTypeface(Typeface.MONOSPACE);
        // 3.6.14 (peticion de Diego): rehacer un patron antes de exportar. Nada se borra: la serie queda
        // ANULADA en el diario con su motivo y el paso vuelve a la cola.
        btnBanco = boton("Banco: completo", v -> elegirBanco());
        btnRehacerAnterior = boton("Rehacer el paso anterior", v -> rehacerAnterior());
        btnRehacerPatron = boton("Rehacer patrón…", v -> elegirPasoARehacer());
        fila(btnRehacerAnterior, btnRehacerPatron);
        chkConfirmar = new android.widget.CheckBox(this);
        chkConfirmar.setText("Confirmar cada patrón antes de pasar al siguiente");
        chkConfirmar.setChecked(prefConfirmar());
        chkConfirmar.setOnCheckedChangeListener((v, c) -> getSharedPreferences("rtv", MODE_PRIVATE).edit()
                .putBoolean(PREF_CONFIRMAR, c).apply());
        raiz.addView(chkConfirmar);
        final android.widget.CheckBox chkRapido = new android.widget.CheckBox(this);
        chkRapido.setText("Modo rápido: 1 colocación × 4 disparos (A5 y OSCURO siempre K = 5). Sin marcar: preciso, "
                + "el K × M de la cola");
        chkRapido.setChecked(prefRapido());
        chkRapido.setOnCheckedChangeListener((v, c) -> {
            getSharedPreferences("rtv", MODE_PRIVATE).edit().putBoolean(PREF_RAPIDO, c).apply();
            pintar();
        });
        raiz.addView(chkRapido);
        Button exp = boton("Exportar (ZIP ligero)", v -> exportar(null));
        Button sop = boton("ZIP de soporte", v -> exportarSoporte());
        fila(exp, sop);
        cargar();
    }

    private static final String PREF_CONFIRMAR = "confirmar_patron";
    private static final String PREF_RAPIDO = "banco_rapido";

    /** 3.6.15 (PROTOCOLO-MIN): modo rapido 1 x 4, activo por defecto; "preciso" = el K x M de la cola. */
    private boolean prefRapido() {
        return getSharedPreferences("rtv", MODE_PRIVATE).getBoolean(PREF_RAPIDO, true);
    }
    private Button btnRehacerAnterior;
    private Button btnBanco;
    /** "Quedan N patrones, unos M min" y lo que se dio por hecho al abrir (3.6.16). */
    private String resumenPrevio = "";
    private Button btnRehacerPatron;
    private android.widget.CheckBox chkConfirmar;

    private boolean prefConfirmar() {
        return getSharedPreferences("rtv", MODE_PRIVATE).getBoolean(PREF_CONFIRMAR, false);
    }

    private String describirPaso(int orden) {
        BancoCola.Paso p = cola.paso(orden);
        String sid = campana.seriePaso(orden);
        Campana.Serie s = sid == null || sid.isEmpty() ? null : campana.serie(sid);
        return "Paso " + orden + (p == null ? "" : " (" + p.tipo + " " + (p.patron.isEmpty() ? "" : p.patron) + ")")
                + (s == null ? "" : ", serie " + s.id + " " + s.patron + String.format(Locale.US, ", x = %.1f", s.media()));
    }

    private void rehacerAnterior() {
        Integer o = campana == null ? null : campana.ultimoPasoRehacible();
        if (o != null) {
            comprobarYRehacer(o);
        }
    }

    /** QA-3614 §6: lista filtrable (patron, color, tipo, sesion, paso), la mas reciente arriba. */
    private void elegirPasoARehacer() {
        if (campana == null) {
            return;
        }
        if (campana.pasosRehacibles().isEmpty()) {
            aviso("No hay pasos hechos que rehacer.");
            return;
        }
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        final EditText filtro = new EditText(this);
        filtro.setSingleLine(true);
        filtro.setHint("Filtrar: P37, amarillo, sesión 2…");
        caja.addView(filtro);
        final android.widget.ListView lista = new android.widget.ListView(this);
        caja.addView(lista, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(360)));
        final List<Integer> vis = new ArrayList<>();
        final ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        lista.setAdapter(ad);
        final Runnable refiltrar = () -> {
            vis.clear();
            vis.addAll(RehacerBanco.filtrar(campana, cola, filtro.getText().toString()));
            ad.clear();
            for (int o : vis) {
                ad.add(RehacerBanco.linea(campana, cola, o));
            }
            ad.notifyDataSetChanged();
        };
        refiltrar.run();
        filtro.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence c, int a, int b, int d) {
            }

            @Override
            public void onTextChanged(CharSequence c, int a, int b, int d) {
                refiltrar.run();
            }

            @Override
            public void afterTextChanged(android.text.Editable e) {
            }
        });
        final AlertDialog d = new AlertDialog.Builder(this).setTitle("Rehacer patrón").setView(caja)
                .setNegativeButton("Cancelar", null).create();
        lista.setOnItemClickListener((parent, view, pos, id) -> {
            d.dismiss();
            comprobarYRehacer(vis.get(pos));
        });
        d.show();
    }

    /**
     * QA-3614-03 y -05: no se rehace con un acta en curso, ni un OSCURO o una A5 de una sesion terminada; con
     * un acta ACEPTADA del codigo del paso, se avisa y se pide confirmacion.
     */
    private void comprobarYRehacer(int orden) {
        String no = RehacerBanco.motivoNoRehacer(campana, cola, orden);
        if (no != null) {
            alerta("No se rehace", no);
            return;
        }
        String b;
        try {
            b = FlujoCalibracion.bloqueoRehacer(almacenActas(), cola, orden);
        } catch (IOException e) {
            b = "BLOQUEO: no se pudieron leer las actas: " + e.getMessage();
        }
        if (b != null && b.startsWith("BLOQUEO")) {
            alerta("No se rehace", b.substring("BLOQUEO: ".length()));
            return;
        }
        if (b != null) {
            new AlertDialog.Builder(this).setTitle("Atención").setMessage(b.substring("AVISO: ".length()))
                    .setPositiveButton("Rehacer igualmente", (d, w) -> pedirMotivoYRehacer(orden))
                    .setNegativeButton("Cancelar", null).show();
            return;
        }
        pedirMotivoYRehacer(orden);
    }

    private FlujoCalibracion.AlmacenActa almacenActas() {
        final BancoActivity yo = this;
        return new FlujoCalibracion.AlmacenActa() {
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
            public boolean aceptadoAntes(char k) throws IOException {
                return Campanas.aceptadoAntes(yo, k);
            }
        };
    }

    /** Motivo obligatorio y de una linea; la serie queda ANULADA con el y el paso vuelve a la cola. */
    private void pedirMotivoYRehacer(int orden) {
        final EditText e = new EditText(this);
        e.setSingleLine(true);   // QA-3614-02
        e.setHint("Motivo: papel equivocado: pedía P45, puse P40");
        BancoCola.Paso pp = cola.paso(orden);
        boolean ajuste = pp != null && "PATRON".equals(pp.tipo);
        new AlertDialog.Builder(this).setTitle("Rehacer " + RehacerBanco.corto(campana, cola, orden)).setView(e)
                .setMessage("La serie queda ANULADA en el diario con este motivo (no se borra), sale "
                        + (ajuste ? "del ajuste" : "del ancla y de la s_rep") + " y el paso vuelve a la cola.")
                .setPositiveButton("Rehacer", (d, w) -> {
                    String m = e.getText().toString().trim();
                    if (m.isEmpty()) {
                        aviso("El motivo es obligatorio.");
                        pedirMotivoYRehacer(orden);
                        return;
                    }
                    try {
                        RehacerBanco.rehacer(campana, cola, orden, m, Sesion.ahoraIso());
                        Registro.nota("banco: " + RehacerBanco.linea(campana, cola, orden) + " a rehacer: " + m);
                    } catch (IOException | RuntimeException ex) {
                        alerta("Rehacer", "No se pudo: " + ex.getMessage());
                    }
                    ultimoOfrecido = 0;
                    pintar();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private String motivoRehacer(int orden) {
        String sid = null;
        for (Campana.Serie x : campana.seriesAnuladas()) {
            if (cola.paso(orden) != null && (x.patron.equals(cola.paso(orden).patron)
                    || (Campana.OSCURO.nombre.equals(x.patron) && "OSCURO".equals(cola.paso(orden).tipo)))) {
                sid = x.anulada;
            }
        }
        return sid == null ? "vuelve a la cola" : sid;
    }

    /** La cola de un tipo de banco, o null si no viaja en este APK o su md5 no esta admitido. */
    private BancoCola colaDe(BancoCola.Tipo t) {
        return colaApk(t);
    }

    /** PROTOCOLO-AJUSTE de decisiones.csv para este equipo (null = el provisional, PRECISO). */
    private String protocoloAjuste() {
        return campana == null ? null : decisionesApk().valor("PROTOCOLO-AJUSTE",
                TablaCalibracion.canonico(campana.historialSeries(), campana.mac));
    }

    /** TIPO-I-REPETIR de decisiones.csv para este equipo: esos patrones se miden en preciso, 5 x 4. */
    private java.util.List<String> repetir() {
        return campana == null ? new java.util.ArrayList<String>() : repetirApk(decisionesApk(),
                TablaCalibracion.canonico(campana.historialSeries(), campana.mac));
    }

    /** 3.6.15: selector de banco (completo / representativo / verificacion anual), solo antes del primer paso. */
    private void elegirBanco() {
        if (campana == null) {
            return;
        }
        final boolean conPasos = !campana.pasos().isEmpty();
        final BancoCola.Tipo[] ts = BancoCola.Tipo.values();
        String[] items = new String[ts.length];
        for (int i = 0; i < ts.length; i++) {
            items[i] = ts[i].nombre + (colaDe(ts[i]) == null ? " (no disponible en este APK)" : "");
        }
        new AlertDialog.Builder(this).setTitle("Tipo de banco" + (conPasos ? " (cambia a mitad: lo ya medido se vuelve "
                + "a contar)" : "")).setItems(items, (d, w) -> {
            if (colaDe(ts[w]) == null) {
                alerta("Tipo de banco", ts[w].nombre + " no viaja en este APK todavía.");
                return;
            }
            try {
                campana.elegirCola(ts[w].name(), colaDe(ts[w]).md5);
            } catch (IOException | RuntimeException e) {
                alerta("Tipo de banco", e.getMessage());
            }
            cargar();
        }).setNegativeButton("Cancelar", null).show();
    }

    /** RF-APP-51: ZIP de soporte, aparte (es el que se importa en otro telefono). */
    private void exportarSoporte() {
        if (campana == null) {
            return;
        }
        try {
            Campanas.Exportacion ex = Campanas.exportarSoporte(this, campana.serieActual(), campana.mac);
            txtResultado.setText("ZIP de soporte: " + ex.zip.getName() + "\nmd5 " + ex.md5 + "\nsha256 " + ex.sha256
                    + "\nCopia: " + ex.copia);
            compartirZip(ex, "Soporte de " + campana.serieConHistoria() + " (" + campana.mac + ")");
        } catch (IOException | RuntimeException e) {
            alerta("ZIP de soporte", "No se pudo preparar: " + e.getMessage());
        }
    }

    private void cargar() {
        Sesion s = Sesion.get();
        if (s.mac == null || s.mac.isEmpty() || !s.serieConocida()) {
            txtPaso.setText("Conecte con el equipo (y, si hace falta, dé su serie en Campaña) antes de medir el banco.");
            btnOk.setEnabled(false);
            btnSaltar.setEnabled(false);
            return;
        }
        try {
            campana = Campanas.abrir(this, s.serie(), s.mac);
        } catch (IOException | RuntimeException e) {
            alerta("Campaña", "No se pudo abrir la campaña: " + e.getMessage());
            return;
        }
        if (!campana.colaElegida() && campana.pasos().isEmpty()) {
            // RF-APP-49: completo la primera vez en un equipo; representativo despues (otra campana de esta MAC).
            BancoCola.Tipo def = Campanas.hayOtraCampanaDeEsteEquipo(this) ? BancoCola.Tipo.REPRESENTATIVO
                    : BancoCola.Tipo.COMPLETO;
            BancoCola cd = colaDe(def);
            if (cd != null) {
                try {
                    campana.elegirCola(def.name(), cd.md5);
                } catch (IOException | RuntimeException e) {
                    Registro.nota("banco: no se pudo anotar la cola: " + e.getMessage());
                }
            }
        }
        BancoCola.Tipo tipo = BancoCola.Tipo.de(campana.colaTipo());
        cola = colaDe(tipo);
        if (cola == null) {
            txtPaso.setText("Cola no admitida (" + tipo.nombre + "). No se mide nada.");
            Registro.nota("banco: cola no admitida: " + tipo.asset);
            btnOk.setEnabled(false);
            btnSaltar.setEnabled(false);
            return;
        }
        btnBanco.setText("Banco: " + tipo.nombre);
        if (!campana.colaMd5().isEmpty() && !campana.colaMd5().equals(cola.md5)) {
            // P14-B03: la cola de la campana y la del APK no son la misma version (los pasos conservan su orden).
            Registro.nota("banco: la campaña empezó con la cola md5 " + campana.colaMd5() + " y el APK trae " + cola.md5);
        }
        resumenPrevio = aplicarBancoPrevio(campana);
        Registro.nota("banco: cola " + tipo.name() + " md5 " + cola.md5 + ", " + cola.pasos.size() + " pasos");
        pintar();
    }

    private void pintar() {
        if (cola == null || campana == null) {
            return;
        }
        Map<Integer, String> est = campana.pasos();
        int hechos = 0;
        int saltados = 0;
        int rehacer = 0;
        for (String e : est.values()) {
            if ("HECHO".equals(e)) {
                hechos++;
            } else if ("SALTADO".equals(e)) {
                saltados++;
            } else if ("REHACER".equals(e)) {
                rehacer++;
            }
        }
        paso = cola.siguiente(est, ultimoOfrecido);
        List<BancoCola.Paso> salt = cola.saltados(est);
        txtAvance.setText(String.format(Locale.US, "Equipo %s (%s). Cola md5 %s…\nPasos hechos %d de %d, saltados %d%s.%s",
                campana.equipo, campana.mac, cola.md5.substring(0, 8), hechos, cola.pasos.size(), saltados,
                rehacer == 0 ? "" : ", por rehacer " + rehacer,
                campana.bateriaBloqueaEscrituras() ? "\nBATERÍA: escrituras bloqueadas (n = 0 o sin respuesta)." : "")
                + (salt.isEmpty() ? "" : "\nSaltados: " + nombres(salt))
                + (campana.cerrada() ? "\nCampaña CERRADA: no se mide nada más en ella (abra una nueva en Campaña, Avanzado)." : "")
                + (enCurso != null ? "\n" + enCurso.texto() : "")
                + "\n" + BancoPrevio.textoPendiente(cola, campana.pasos(), prefRapido(), protocoloAjuste(), repetir())
                + (resumenPrevio.isEmpty() ? "" : "\nAl abrir: " + resumenPrevio));
        if (paso == null) {
            txtPaso.setText("Banco completo.");
        } else {
            String reintento = "SALTADO".equals(est.get(paso.orden)) ? " (saltado antes)"
                    : "REHACER".equals(est.get(paso.orden)) ? " (A REHACER: " + motivoRehacer(paso.orden) + ")" : "";
            txtPaso.setText("Sesión " + paso.sesion + " · paso " + paso.orden + reintento + "\n" + paso.instruccion()
                    + (paso.esMedida() ? String.format(Locale.US, "\n%d colocaciones × %d disparos + %d de asentamiento%s",
                    Protocolo.efectivo(paso, prefRapido(), protocoloAjuste(), repetir())[0],
                    Protocolo.efectivo(paso, prefRapido(), protocoloAjuste(), repetir())[1], paso.asentamiento,
                    "OSCURO".equals(paso.tipo) || "A5".equals(paso.tipo) ? " (A5 y OSCURO: siempre K = 5)"
                            : Protocolo.aRepetir(paso, repetir()) ? " (preciso: Diego manda repetirlo, TIPO-I-REPETIR)"
                            : Protocolo.deAjuste(paso) && !"RAPIDO".equals(protocoloAjuste())
                            ? " (preciso: patrón de lo que se escribe, PROTOCOLO-AJUSTE)"
                            : prefRapido() ? " (rápido; con 1 colocación la reproducibilidad no se evalúa)" : " (preciso)") : "")
                    + (paso.esMedida() && !paso.codigo.isEmpty() ? "\nCódigo " + paso.codigo + ", uso " + paso.uso : "")
                    + (paso.nota.isEmpty() ? "" : "\nNota: " + paso.nota)
                    + (paso.ajusteApp.isEmpty() ? "" : "\n" + paso.ajusteApp));
        }
        boolean con = EnlaceSerie.instancia().estaConectado();
        boolean abierta = !campana.cerrada();
        boolean libre = !ocupado && enCurso == null;
        btnOk.setEnabled(abierta && paso != null && libre
                && (con || "CALENTAMIENTO".equals(paso.tipo) || "EXPORTAR".equals(paso.tipo)));
        btnSaltar.setEnabled(abierta && paso != null && libre && paso.saltable());
        Integer ult = campana.ultimoPasoRehacible();
        btnRehacerAnterior.setEnabled(abierta && libre && ult != null);
        // QA-3614 §6: texto corto.
        btnRehacerAnterior.setText(ult == null ? "Rehacer anterior" : "Rehacer " + RehacerBanco.corto(campana, cola, ult));
        btnRehacerPatron.setEnabled(abierta && libre && ult != null);
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (btnOk != null) {
            pintar();
        }
    }

    private static String nombres(List<BancoCola.Paso> l) {
        StringBuilder sb = new StringBuilder();
        for (BancoCola.Paso p : l) {
            sb.append(sb.length() == 0 ? "" : ", ").append(p.patron).append(" (").append(p.orden).append(')');
        }
        return sb.toString();
    }

    private void anotar(BancoCola.Paso p, String estado, String serieId, String nota) {
        try {
            campana.anotarPaso(p.orden, estado, serieId, Sesion.ahoraIso(), nota);
        } catch (IOException | RuntimeException e) {
            alerta("Campaña", "No se pudo guardar el paso: " + e.getMessage());
        }
    }

    private void saltar() {
        if (paso == null || !paso.saltable()) {
            return;
        }
        anotar(paso, "SALTADO", "", "saltado por el operador");
        ultimoOfrecido = paso.orden;
        Registro.nota("banco: paso " + paso.orden + " (" + paso.patron + ") saltado");
        pintar();
    }

    private void ok() {
        final BancoCola.Paso p = paso;
        if (p == null) {
            return;
        }
        switch (p.tipo) {
            case "CALENTAMIENTO":
                anotar(p, "HECHO", "", "calentamiento confirmado por el operador");
                pintar();
                break;
            case "BATERIA":
                bateria(p);
                break;
            case "EXPORTAR":
                exportar(p);
                break;
            default:
                if (p.esMedida()) {
                    medir(p);
                } else {
                    anotar(p, "HECHO", "", "paso sin acción en la app: " + p.tipo);
                    pintar();
                }
        }
    }

    // ------------------------------------------------------------------ bateria

    private void bateria(BancoCola.Paso p) {
        ocupado = true;
        pintar();
        Cliente.instancia().ejecutar(() -> {
            Integer n = null;
            String err = null;
            try {
                Cliente.Respuesta r = Cliente.instancia().pedir("9", Tramas.Tipo.BATERIA, Cliente.TIMEOUT_MEDIDA_MS);
                n = r.valida() ? Bateria.n(r.trama) : null;
            } catch (IOException | InterruptedException | RuntimeException e) {
                err = EnlaceSerie.descripcion(e);
            }
            final Bateria.Lectura l = Bateria.interpretar(n);
            final String ferr = err;
            enUi(() -> {
                ocupado = false;
                if (ferr != null) {
                    alerta("Batería", "Enlace perdido: " + ferr);
                    pintar();
                    return;
                }
                try {
                    campana.anotarBateria(Sesion.ahoraIso(), l.n, l.texto);
                } catch (IOException e) {
                    alerta("Campaña", "No se pudo guardar la batería: " + e.getMessage());
                }
                anotar(p, "HECHO", "", l.texto);
                txtResultado.setText(l.texto);
                if (l.aviso) {
                    alerta("Batería", l.texto);
                }
                pintar();
            });
        });
    }

    // ------------------------------------------------------------------ medida

    /**
     * QA-3610-04: la serie en curso vive fuera de la actividad. Si la actividad se destruye (Atras
     * confirmado, sistema), el hilo no toca una pantalla muerta: deja el estado aqui y la actividad
     * siguiente lo retoma y SIGUE LA MISMA SERIE. Nunca dos series del mismo paso en cola.
     */
    static final class Medicion {
        final BancoCola.Paso p;
        final Campana campana;
        final String nombre;
        Campana.Serie serie;
        final List<double[]> hechas = new ArrayList<>();
        final List<String> notas = new ArrayList<>();
        /** Colocacion en curso (1..K). */
        int k = 1;
        volatile boolean trabajando;
        volatile boolean cancelada;
        /** Lo que espera al operador: AUSENTE, LEVANTAR, FIN o ERROR; null si nada. */
        volatile String pendiente;
        volatile String detalle;
        Veredicto.Resultado veredicto;
        /** El operador confirmo "¿Era Pxx?" (preferencia de la 3.6.14). */
        boolean confirmado;

        /** Protocolo efectivo de este paso (3.6.15, PROTOCOLO-MIN): K colocaciones x M disparos. */
        final int kEf;
        final int mEf;
        final String protocolo;

        Medicion(BancoCola.Paso p, Campana c, boolean rapido, String protocoloAjuste, java.util.List<String> repetir) {
            this.p = p;
            this.campana = c;
            this.nombre = "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron;
            int[] km = Protocolo.efectivo(p, rapido, protocoloAjuste, repetir);
            this.kEf = km[0];
            this.mEf = km[1];
            this.protocolo = Protocolo.texto(km, rapido && km[0] == Protocolo.K_DEFECTO);
        }

        String texto() {
            return "Serie en curso: " + nombre + (serie == null ? "" : " (" + serie.id + ")") + ", paso " + p.orden
                    + ", colocación " + Math.min(k, kEf) + " de " + kEf;
        }
    }

    private static Medicion enCurso;
    private static BancoActivity visible;
    private static final android.os.Handler PRINCIPAL = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean dialogoAbierto;

    @Override
    protected void onResume() {
        super.onResume();
        visible = this;
        Medicion m = enCurso;
        if (m != null) {
            fijarOrientacion(true);
            if (!m.trabajando && m.pendiente != null) {
                atender(m);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (visible == this) {
            visible = null;
        }
        super.onDestroy();
    }

    /** Orientacion fija en vertical mientras hay una serie en curso (QA-3610-04). */
    private void fijarOrientacion(boolean fija) {
        setRequestedOrientation(fija ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onBackPressed() {
        final Medicion m = enCurso;
        if (m == null) {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Serie a medias")
                .setMessage(m.texto() + ".\n\nSi sale, la serie se cierra como NO aceptada (queda en el diario) y el "
                        + "paso se repite entero al volver.")
                .setPositiveButton("Seguir midiendo", null)
                .setNegativeButton("Salir y dejar la serie", (d, w) -> {
                    m.cancelada = true;
                    if (!m.trabajando) {
                        terminar(m, "dejada por el operador (Atrás)");
                    }
                    finish();
                }).show();
    }

    private void medir(BancoCola.Paso p) {
        Sesion s = Sesion.get();
        if (!s.versionMedible()) {
            alerta("No se puede medir", "Pase antes las pruebas del equipo (firmware: " + s.firmware() + ").");
            return;
        }
        if (enCurso != null) {
            aviso(enCurso.texto());
            return;
        }
        Medicion m = new Medicion(p, campana, prefRapido(), protocoloAjuste(), repetir());
        enCurso = m;
        fijarOrientacion(true);
        pantallaEncendida(true);
        pintar();
        lanzar(m, null);
    }

    /** Lanza la colocacion m.k en el hilo de trabajo. notaForzada != null: "medir igualmente". */
    private static void lanzar(Medicion m, String notaForzada) {
        m.pendiente = null;
        m.trabajando = true;
        Cliente.instancia().ejecutar(() -> colocacion(m, notaForzada));
    }

    /** Una colocacion en el hilo de trabajo: asentamiento (con el filtro de patron presente) + M disparos. */
    private static void colocacion(Medicion m, String notaForzada) {
        Sesion s = Sesion.get();
        BancoCola.Paso p = m.p;
        String error = null;
        String ausente = null;
        try {
            if (notaForzada == null) {
                double xa = Double.NaN;
                for (int i = 0; i < Math.max(1, p.asentamiento) && !m.cancelada; i++) {
                    progreso("Colocación " + m.k + " de " + m.kEf + ": asentamiento (se descarta)...");
                    LecturaX.Lectura l = LecturaX.leer(s);
                    Registro.nota("banco: disparo de asentamiento, descartado: " + l.codigo + " -> "
                            + (l.respuesta.valida() ? l.respuesta.trama : l.respuesta.describir()));
                    xa = l.x;
                }
                if (!"OSCURO".equals(p.tipo)) {
                    double xo = m.campana.xOscuro();
                    ausente = Colocacion.patronAusente(m.nombre, xa, p.xEsperada, p.toleranciaX(),
                            Double.isNaN(xo) ? Asistente.X_OSCURO : xo);
                }
            } else {
                m.notas.add("colocación " + m.k + " medida igualmente: " + notaForzada);
                Registro.nota("banco: colocación " + m.k + " medida igualmente tras el rechazo del filtro: " + notaForzada);
            }
            if (ausente == null && !m.cancelada) {
                List<Double> xs = new ArrayList<>();
                for (int i = 0; i < m.mEf && !m.cancelada; i++) {
                    progreso("Midiendo " + m.nombre + ": colocación " + m.k + " de " + m.kEf + ", disparo " + (i + 1)
                            + " de " + m.mEf + "...");
                    LecturaX.Lectura l = LecturaX.leer(s);
                    if (!l.valida()) {
                        Registro.nota("banco: disparo no válido: " + l.error);
                        continue;
                    }
                    if (m.serie == null) {
                        m.serie = m.campana.nuevaSerie(Sesion.ahoraIso(), s.serie(), s.mac, s.firmware(), m.nombre, 0,
                                l.codigo);
                        Registro.nota("banco: paso " + p.orden + " -> serie " + m.serie.id);
                    }
                    m.campana.agregarDisparo(m.serie, m.k, Sesion.ahoraIso(), l.respuesta.trama, l.x);
                    xs.add(l.x);
                }
                if (!m.cancelada) {
                    m.hechas.add(Estadistica.aVector(xs));
                }
            } else if (ausente != null) {
                Registro.nota("banco: " + ausente);
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            error = EnlaceSerie.descripcion(e);
        }
        if (m.cancelada) {
            m.pendiente = "CANCELADA";
        } else if (error != null) {
            m.pendiente = "ERROR";
            m.detalle = error + Cliente.instancia().consejoSiMudo();
        } else if (ausente != null) {
            m.pendiente = "AUSENTE";
            m.detalle = ausente;
        } else if (m.k < m.kEf) {
            m.k++;
            m.pendiente = "LEVANTAR";
        } else {
            m.pendiente = "FIN";
        }
        m.trabajando = false;
        PRINCIPAL.post(() -> {
            if (m.cancelada) {
                terminarEstatico(m, "dejada por el operador (Atrás)");
                return;
            }
            BancoActivity a = visible;
            if (a != null && !a.isFinishing() && !a.isDestroyed()) {
                a.atender(m);
            }
            // si no hay pantalla, el estado queda en enCurso y la siguiente lo retoma (onResume)
        });
    }

    private static void progreso(String t) {
        PRINCIPAL.post(() -> {
            BancoActivity a = visible;
            if (a != null && a.txtResultado != null) {
                a.txtResultado.setText(t);
            }
        });
    }

    /** Cierra la serie a medias como no aceptada y libera el paso. */
    private static void terminarEstatico(Medicion m, String nota) {
        if (m.serie != null && "PENDIENTE".equals(m.serie.veredicto)) {
            try {
                m.campana.cerrar(m.serie, "REPETIR", false, "banco: " + nota);
            } catch (IOException | RuntimeException e) {
                Registro.nota("no se pudo cerrar la serie: " + e.getMessage());
            }
        }
        if (enCurso == m) {
            enCurso = null;
        }
        BancoActivity a = visible;
        if (a != null && !a.isFinishing() && !a.isDestroyed()) {
            a.fijarOrientacion(false);
            a.pantallaEncendida(false);
            a.pintar();
        }
    }

    private void terminar(Medicion m, String nota) {
        terminarEstatico(m, nota);
    }

    /** Lo que espera al operador, en la pantalla visible. Se puede llamar otra vez al volver (onResume). */
    private void atender(Medicion m) {
        if (m != enCurso || dialogoAbierto || m.pendiente == null) {
            return;
        }
        switch (m.pendiente) {
            case "ERROR":
                alerta("Medida interrumpida", m.detalle + "\nAl volver se repite el paso entero.");
                terminar(m, "interrumpida: " + m.detalle);
                return;
            case "AUSENTE":
                rechazo(m);
                return;
            case "LEVANTAR":
                dialogo(new AlertDialog.Builder(this).setTitle("Levante y apoye (" + m.k + " de " + m.kEf + ")")
                        .setMessage("Levante el equipo y vuelva a apoyarlo sobre " + m.nombre + ". Pulse OK cuando esté apoyado.")
                        .setPositiveButton("OK", (d, w) -> {
                            dialogoAbierto = false;
                            lanzar(m, null);
                        }));
                return;
            case "FIN":
                veredicto(m);
                return;
            default:
        }
    }

    private void dialogo(AlertDialog.Builder b) {
        dialogoAbierto = true;
        b.setCancelable(false).show();
    }

    /**
     * QA-3610-02: el rechazo del filtro siempre tiene salida. "Repetir colocación" (otro asentamiento),
     * "Medir igualmente" (nota obligatoria, queda en la serie) o "Dejar el paso" (un patron queda
     * SALTADO y se ofrece al final; un control se repite).
     */
    private void rechazo(Medicion m) {
        final EditText nota = new EditText(this);
        nota.setSingleLine(true);   // QA-3614-02: ningun texto libre con salto de linea
        nota.setHint("Nota (obligatoria para medir igualmente)");
        dialogo(new AlertDialog.Builder(this).setTitle("¿Está el patrón?").setMessage(m.detalle).setView(nota)
                .setPositiveButton("Repetir colocación", (d, w) -> {
                    dialogoAbierto = false;
                    lanzar(m, null);
                })
                .setNeutralButton("Medir igualmente", (d, w) -> {
                    dialogoAbierto = false;
                    String n = nota.getText().toString().trim();
                    if (n.isEmpty()) {
                        aviso("La nota es obligatoria para medir igualmente.");
                        rechazo(m);
                        return;
                    }
                    lanzar(m, n);
                })
                .setNegativeButton("Dejar el paso", (d, w) -> {
                    dialogoAbierto = false;
                    terminar(m, "dejada: patrón no presente");
                    if (m.p.saltable()) {
                        anotar(m.p, "SALTADO", m.serie == null ? "" : m.serie.id, "dejado tras el rechazo del filtro");
                        ultimoOfrecido = m.p.orden;
                    }
                    pintar();
                }));
    }

    private void cerrarIncompleta(Campana.Serie s, String nota) {
        if (s != null) {
            try {
                campana.cerrar(s, "REPETIR", false, "banco: " + nota);
            } catch (IOException e) {
                Registro.nota("no se pudo cerrar la serie: " + e.getMessage());
            }
        }
    }

    /** Fin de la serie: veredicto. La serie sigue "en curso" hasta que el operador decide (no se duplica). */
    private void veredicto(Medicion m) {
        BancoCola.Paso p = m.p;
        Campana.Serie s = m.serie;
        if (s == null) {
            alerta("Sin lecturas", "Ningún disparo dio lectura. Se repite el paso.");
            terminar(m, "sin lecturas");
            return;
        }
        String cab = "Paso " + p.orden + ", " + s.id + " " + s.patron + ", código " + p.codigo + ", uso " + p.uso;
        String notas = m.notas.isEmpty() ? "" : "; " + String.join("; ", m.notas);
        if ("A5".equals(p.tipo)) {
            cerrarSerie(s, A5.VEREDICTO, false, "banco paso " + p.orden + " (" + p.bloque + ")" + notas);
            anotar(p, "HECHO", s.id, "A5");
            txtResultado.setText(cab + "\n" + A5.evaluar(campana).texto);
            liberar(m);
            return;
        }
        Patron pat = campana.patron(s.patron);
        if (m.veredicto == null) {
            List<double[]> grupos = m.hechas;
            Veredicto.Resultado v = Veredicto.evaluarColocaciones(grupos, pat, campana.medias(pat.nombre),
                    Veredicto.TOL_ORDEN, Veredicto.REPRO_MAX);
            try {
                int base = 0;
                int idx = 0;
                for (double[] g : grupos) {
                    for (int i = 0; i < g.length; i++) {
                        if (v.descartar[base + i]) {
                            campana.descartar(s, idx + 1, v.motivos[base + i]);
                        }
                        idx++;
                    }
                    base += g.length;
                }
            } catch (IOException | RuntimeException e) {
                Registro.nota("banco: no se pudo anotar un descarte: " + e.getMessage());
            }
            m.veredicto = v;
        }
        final Veredicto.Resultado v = m.veredicto;
        final String nota = "banco paso " + p.orden + ", código " + p.codigo + ", uso " + p.uso + ", protocolo "
                + m.protocolo + notas;
        if ("OK".equals(v.veredicto) && prefConfirmar() && "PATRON".equals(p.tipo) && !m.confirmado) {
            // 3.6.14: confirmacion opcional antes de aceptar un OK ("¿Era P45?").
            dialogo(new AlertDialog.Builder(this).setTitle("¿Era " + p.patron + "?")
                    .setMessage(cab + "\n" + v.texto + "\n¿La serie se midió sobre " + p.patron + " (" + p.color + " "
                            + p.tipoLamina + ", cert. " + Campana.fmt(p.certificado) + ")?")
                    .setPositiveButton("Sí", (x, w) -> {
                        dialogoAbierto = false;
                        m.confirmado = true;
                        veredicto(m);
                    })
                    .setNegativeButton("No, rehacer", (x, w) -> {
                        dialogoAbierto = false;
                        final EditText e = new EditText(this);
                        e.setSingleLine(true);
                        e.setHint("Motivo: papel equivocado: pedía " + p.patron + ", puse P40");
                        dialogo(new AlertDialog.Builder(this).setTitle("Rehacer " + p.patron).setView(e)
                                .setPositiveButton("Rehacer", (x2, w2) -> {
                                    dialogoAbierto = false;
                                    String mot = e.getText().toString().trim();
                                    if (mot.isEmpty()) {
                                        aviso("El motivo es obligatorio.");
                                        veredicto(m);
                                        return;
                                    }
                                    // QA-3614-06: la serie NO se acepta y el paso no se da por hecho; se anula en
                                    // seguida. Si la app muere entre medias, la serie no esta aceptada y el paso se repite.
                                    cerrarSerie(s, "OK", false, nota + "; el operador dice que no era " + p.patron);
                                    try {
                                        campana.anular(s, mot, Sesion.ahoraIso());
                                    } catch (IOException | RuntimeException ex) {
                                        alerta("Rehacer", ex.getMessage());
                                    }
                                    txtResultado.setText(cab + "\nANULADA (" + mot + "): el paso se repite.");
                                    liberar(m);
                                })
                                .setNegativeButton("Volver", (x2, w2) -> {
                                    dialogoAbierto = false;
                                    veredicto(m);
                                }));
                    }));
            return;
        }
        if ("OK".equals(v.veredicto)) {
            cerrarSerie(s, "OK", true, nota);
            elegirDelBanco(s);
            anotar(p, "HECHO", s.id, "OK");
            txtResultado.setText(cab + "\n" + v.texto + "Aceptada. Siguiente paso.");
            liberar(m);
            return;
        }
        txtResultado.setText(cab + "\n" + v.texto);
        AlertDialog.Builder d = new AlertDialog.Builder(this).setTitle(s.patron + ": " + v.veredicto)
                .setMessage(cab + "\n" + v.texto)
                .setPositiveButton("Repetir", (x, w) -> {
                    dialogoAbierto = false;
                    cerrarSerie(s, v.veredicto, false, nota + "; repetida");
                    liberar(m);
                    medir(p);
                })
                .setNeutralButton("Aceptar con nota", (x, w) -> {
                    dialogoAbierto = false;
                    aceptarConNota(m, v.veredicto, nota);
                });
        if (p.saltable()) {
            d.setNegativeButton("Saltar", (x, w) -> {
                dialogoAbierto = false;
                cerrarSerie(s, v.veredicto, false, nota + "; saltada");
                anotar(p, "SALTADO", s.id, v.veredicto);
                ultimoOfrecido = p.orden;
                liberar(m);
            });
        }
        dialogo(d);
    }

    private void liberar(Medicion m) {
        if (enCurso == m) {
            enCurso = null;
        }
        fijarOrientacion(false);
        pantallaEncendida(false);
        pintar();
    }

    /**
     * QA-3610-10: la serie aceptada en el banco pasa a ser la elegida de su patron, aunque un ELIGE
     * anterior (p. ej. del 19-sep, 1 x 9) fijara otra. La mas reciente del banco manda.
     */
    private void elegirDelBanco(Campana.Serie s) {
        try {
            campana.elegir(s);
        } catch (IOException | RuntimeException e) {
            Registro.nota("banco: no se pudo elegir " + s.id + ": " + e.getMessage());
        }
    }

    private void aceptarConNota(Medicion m, String ver, String nota) {
        final Campana.Serie s = m.serie;
        final EditText e = new EditText(this);
        e.setSingleLine(true);   // QA-3614-02: ningun texto libre con salto de linea
        e.setHint("Nota obligatoria");
        dialogo(new AlertDialog.Builder(this).setTitle("Aceptar " + s.id).setView(e)
                .setPositiveButton("Aceptar", (d, w) -> {
                    dialogoAbierto = false;
                    String n = e.getText().toString().trim();
                    if (n.isEmpty()) {
                        aviso("La nota es obligatoria.");
                        aceptarConNota(m, ver, nota);
                        return;
                    }
                    cerrarSerie(s, ver, true, nota + "; " + n);
                    elegirDelBanco(s);
                    anotar(m.p, "HECHO", s.id, ver + " aceptada con nota");
                    liberar(m);
                })
                .setNegativeButton("Volver", (d, w) -> {
                    dialogoAbierto = false;
                    veredicto(m);
                }));
    }

    private void cerrarSerie(Campana.Serie s, String ver, boolean aceptada, String nota) {
        try {
            campana.cerrar(s, ver, aceptada, nota);
        } catch (IOException e) {
            alerta("Campaña", "No se pudo guardar el veredicto: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- exportar

    private void exportar(BancoCola.Paso p) {
        if (campana == null) {
            return;
        }
        Campanas.Exportacion ex;
        try {
            ex = Campanas.exportarConHuellas(this, campana.equipo, campana.mac);
        } catch (IOException | RuntimeException e) {
            alerta("Exportar", "No se pudo preparar el ZIP: " + e.getMessage());
            pintar();
            return;
        }
        if (p != null) {
            anotar(p, "HECHO", "", "exportación de la sesión " + p.sesion + ": " + ex.zip.getName());
        }
        if (ex.copia == null || ex.copia.contains("SIN COPIA")) {
            alerta("Copia en Download", "No se pudo dejar la copia en Download/RTV/: " + ex.copia
                    + ". Comparta el ZIP ahora para no depender del teléfono.");
        }
        txtResultado.setText("ZIP: " + ex.zip.getName() + "\nmd5 " + ex.md5 + "\nsha256 " + ex.sha256 + "\nCopia: " + ex.copia);
        compartirZip(ex, "Banco de " + campana.equipo + " (" + campana.mac + "), " + Sesion.get().firmware());
        pintar();
    }
}
