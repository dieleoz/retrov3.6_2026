package com.dpi.retrov36;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * "RTV Calibra" (RTV 1.0.0-rc6): pantalla UNICA del APK corto, el de Coviandina.
 * Especificacion: 05_Documentacion/SPEC-App-Calibracion-Coviandina.md (RF-APP-54 a RF-APP-62).
 *
 * El flujo que pidio Diego, entero y en este orden:
 *
 *   cargar el ZIP -> calibrar -> nombre -> nota -> serie -> acta -> fin
 *
 * Aqui viven los tres primeros pasos; el nombre, la nota y el acta son de {@link CalibrarActivity}, que NO se
 * toca: es la misma pantalla, el mismo flujo y los mismos numeros que en la app de campo. Esta clase no
 * calcula nada — lo que decide sale de {@link AppCorta}, que es Java puro y esta probado en la JVM.
 *
 * Lo que NO hay aqui, y es deliberado: banco, medida de patrones, botones de pantalla, modo administrador,
 * importacion de CSV antiguos y cierre de campana. Para eso esta la app de campo, que se instala al lado
 * porque esta lleva su propio applicationId (app/build.gradle, buildType "coviandina").
 */
public class CortoActivity extends Base {

    private static final int PIDE_ZIP = 201;
    private static final int PIDE_ACTIVAR_BT = 202;
    private static final int PIDE_PERMISOS = 203;

    private TextView txtFamilia;
    private TextView txtLista;
    private TextView txtResultado;
    private LinearLayout listaDispositivos;
    private Button btnCargar;
    private Button btnCalibrar;
    private Button btnGuardar;
    private Button btnSerie;
    /** true entre el toque del operador y el final de connect(). */
    private boolean esperandoConexion;
    private boolean dialogoSerieAbierto;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        texto("Cargue el ZIP de la campaña y pulse Calibrar. Conecte antes el equipo: las pruebas se hacen solas.");
        txtFamilia = texto("");
        btnCargar = boton("1. Cargar el ZIP de la campaña", v -> cargarZip());
        btnCargar.setTextSize(24);
        btnCalibrar = boton("2. Calibrar", v -> startActivity(new Intent(this, CalibrarActivity.class)));
        btnCalibrar.setTextSize(24);
        btnGuardar = boton("3. Guardar / Compartir", v -> guardarCompartir());
        btnSerie = boton("Serie del equipo", v -> pedirSerie());
        txtResultado = texto("");
        txtResultado.setTypeface(Typeface.MONOSPACE);
        Button act = boton("Actualizar lista", v -> cargarEmparejados());
        Button des = boton("Desconectar", v -> {
            if (Pruebas.get().enCurso()) {
                Pruebas.get().cancelar();
            }
            EnlaceSerie.instancia().desconectar();
        });
        fila(act, des);
        titulo("Equipos emparejados (toque uno para conectar)");
        txtLista = texto("");
        listaDispositivos = new LinearLayout(this);
        listaDispositivos.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(listaDispositivos);
        Campanas.iniciar(this);
        PerfilesApp.iniciar(this);
        pedirPermisos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEmparejados();
        refrescar();
    }

    /** Igual que {@link ConexionActivity#estado}: al conectar, las pruebas del equipo arrancan solas. */
    @Override
    public void estado(boolean conectado, String texto) {
        if (conectado && esperandoConexion) {
            esperandoConexion = false;
            EnlaceSerie en = EnlaceSerie.instancia();
            Sesion.get().reiniciar(en.getNombre(), en.getMac());
            Cliente.instancia().reiniciarCuenta();
            Registro.nota("sesion nueva (app corta): " + Sesion.get().identidad());
            Intent i = new Intent(this, PruebasActivity.class);
            i.putExtra(PruebasActivity.EXTRA_AUTO, true);
            startActivity(i);
        } else if (!conectado && !EnlaceSerie.instancia().estaConectando()) {
            esperandoConexion = false;
        }
        super.estado(conectado, texto);
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (btnCargar == null) {
            return;
        }
        boolean con = EnlaceSerie.instancia().estaConectado();
        Sesion s = Sesion.get();
        Protocolo p = s.protocolo;
        btnCargar.setEnabled(con && s.serieConocida());
        btnSerie.setEnabled(con);
        btnSerie.setText("Serie del equipo: " + (s.serieConocida() ? s.serie() : "sin definir — tóquelo"));
        boolean calibra = p == null || p.calibra();
        btnCalibrar.setEnabled(con && calibra);
        btnCalibrar.setText("2. Calibrar" + (calibra ? "" : "\n(" + p.motivoNoCalibra() + ")"));
        btnGuardar.setEnabled(s.mac != null && !s.mac.isEmpty() && s.serieConocida());
        txtFamilia.setText(familia(p, con));
        txtFamilia.setBackgroundColor(!con ? GRIS : calibra ? VERDE : AMARILLO);
    }

    /**
     * RF-APP-54 bis: el APK acepta el ZIP de cualquier familia y dice en UNA linea que se puede hacer con ese
     * equipo. Las familias que hoy no se calibran no son un fallo de esta app: se dice **que les falta**
     * ({@link Protocolo#queHacerParaCalibrar()}) en vez de negar sin explicar.
     *
     * El texto sale del protocolo, que es quien sabe que puede cada firmware, y es el MISMO que ensena la app
     * de campo en sus previas (FlujoCalibracion.java:379-380): quien use las dos no lee dos frases distintas
     * para la misma situacion.
     */
    private String familia(Protocolo p, boolean conectado) {
        if (!conectado || p == null) {
            return "Equipo sin conectar. Conéctelo para saber de qué familia es.";
        }
        if (p.calibra()) {
            return "Equipo " + p.nombre() + ": se puede calibrar desde aquí.";
        }
        String hacer = p.queHacerParaCalibrar();
        return "Equipo " + p.nombre() + ": desde aquí NO se calibra. " + p.motivoNoCalibra()
                + (hacer.isEmpty() ? "" : " " + hacer);
    }

    // ------------------------------------------------------------- paso 1: el ZIP

    private void cargarZip() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, PIDE_ZIP);
        } catch (ActivityNotFoundException e) {
            aviso("No hay selector de ficheros en este teléfono.");
        }
    }

    @Override
    protected void onActivityResult(int codigo, int resultado, Intent datos) {
        super.onActivityResult(codigo, resultado, datos);
        if (codigo == PIDE_ACTIVAR_BT) {
            cargarEmparejados();
            return;
        }
        if (codigo != PIDE_ZIP || resultado != android.app.Activity.RESULT_OK || datos == null
                || datos.getData() == null) {
            return;
        }
        final Uri u = datos.getData();
        Cliente.instancia().ejecutar(() -> {
            final String res = importar(u);
            Registro.nota("app corta: " + res);
            enUi(() -> {
                txtResultado.setText(res);
                refrescar();
            });
        });
    }

    /**
     * RF-APP-55 a RF-APP-58. La campana de esta app nace VACIA, asi que el ZIP trae su propio banco y sus
     * pasos entran enteros (ImportadorCampana.java:119-135): el desajuste de banco que costo una hora la noche
     * del 19-sep no puede darse aqui. La comprobacion de serie y MAC (ImportadorCampana.java:93-96) y la
     * atomicidad (:98-114) se conservan tal cual: son la regla L-23.
     */
    private String importar(Uri u) {
        Sesion s = Sesion.get();
        if (s.mac == null || s.mac.isEmpty() || !s.serieConocida()) {
            return "Conecte el equipo y fije su serie antes de cargar el ZIP: la campaña es de un equipo concreto.";
        }
        try {
            byte[] datos;
            try (InputStream in = getContentResolver().openInputStream(u)) {
                datos = ImportadorCampana.leer(in);
            }
            String nombre = u.getLastPathSegment() == null ? u.toString() : u.getLastPathSegment();
            String md5 = Resumen.hex(datos, "MD5");
            if (!ImportadorCampana.esZip(datos)) {
                return nombre + ": no es un ZIP. Cargue el ZIP de soporte de la campaña (soporte_…zip).";
            }
            if (ImportadorCampana.esIncremental(datos)) {
                return nombre + ": es el ZIP ligero (incremental) y no trae la campaña entera. "
                        + "Cargue el de soporte (soporte_…zip).";
            }
            String diario = ImportadorCampana.diarioDeZip(datos);
            if (diario == null) {
                return nombre + ": el ZIP no trae el diario de la campaña. Cargue el de soporte (soporte_…zip).";
            }
            Campana c = Campanas.abrir(this, s.serie(), s.mac);
            ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, diario, c.catalogo(),
                    nombre + " md5 " + md5);
            String bp = aplicarBancoPrevio(c);
            BancoCola cola = colaApk(BancoCola.Tipo.de(c.colaTipo()));
            return nombre + " (md5 " + md5 + ")\n" + AppCorta.texto(r, cola, c)
                    + (bp.isEmpty() ? "" : "Banco: " + bp + "\n");
        } catch (IOException | RuntimeException e) {
            return "No se pudo cargar el ZIP: " + EnlaceSerie.descripcion(e);
        }
    }

    // ------------------------------------------------------------ paso serie y cierre

    /**
     * RF-APP-61: la serie se resuelve UNA vez, aqui, y no se vuelve a preguntar. Mismo dialogo y misma via que
     * {@link BancoActivity} y {@link CampanaActivity}: {@link Sesion#aceptarSerieTecleada}. **No graba nada en
     * el equipo**: el alta por "#SN" sigue en el modo administrador de la app de campo.
     */
    private void pedirSerie() {
        if (dialogoSerieAbierto) {
            return;
        }
        dialogoSerieAbierto = true;
        Sesion s = Sesion.get();
        final EditText e = new EditText(this);
        e.setHint("Serie del equipo, p. ej. SLV-002");
        if (s.serieConocida()) {
            e.setText(s.serie());
        }
        new AlertDialog.Builder(this).setTitle("Serie del equipo")
                .setMessage("El equipo responde «" + (s.serieEquipo == null ? "?" : s.serieEquipo)
                        + "» a #GN#. La campaña y el acta van a nombre de la serie que escriba aquí; queda marcada "
                        + "\"declarada, no leída del equipo\" si no la dio el equipo.\n\nPara grabarla en el equipo "
                        + "(#SN) hace falta la app de campo, en Modo administrador.")
                .setView(e).setCancelable(true)
                .setPositiveButton("Aceptar", (d, w) -> {
                    dialogoSerieAbierto = false;
                    if (!Sesion.get().aceptarSerieTecleada(e.getText().toString())) {
                        aviso("Esa serie no vale. Escríbala como la del equipo, p. ej. SLV-002.");
                    } else {
                        Registro.nota("serie tecleada por el operador (app corta): " + Sesion.get().serie());
                    }
                    refrescar();
                })
                .setNegativeButton("Cancelar", (d, w) -> dialogoSerieAbierto = false)
                .setOnCancelListener(d -> dialogoSerieAbierto = false)
                .show();
    }

    /** RF-APP-60: el ZIP ligero y el de soporte, con su copia en Download/RTV/ (= ConexionActivity:173-189). */
    private void guardarCompartir() {
        Campana c = Campanas.abierta();
        Sesion s = Sesion.get();
        String serie = c != null ? c.serieActual() : s.serie();
        String mac = c != null ? c.mac : s.mac;
        try {
            if (c == null) {
                Campanas.abrir(this, serie, mac);
            }
            java.util.List<Campanas.Exportacion> l = new java.util.ArrayList<>();
            l.add(Campanas.exportarConHuellas(this, serie, mac));
            l.add(Campanas.exportarSoporte(this, serie, mac));
            compartirZips(l, "Calibración de " + serie + " (" + mac + "): ZIP ligero y de soporte "
                    + "(copia en Download/RTV/)");
        } catch (IOException | RuntimeException e) {
            alerta("Guardar / Compartir", "No se pudo preparar el ZIP: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------- conexion

    private void pedirPermisos() {
        java.util.List<String> faltan = new java.util.ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            faltan.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (android.os.Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            faltan.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!faltan.isEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(this, faltan.toArray(new String[0]), PIDE_PERMISOS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int codigo, String[] permisos, int[] res) {
        super.onRequestPermissionsResult(codigo, permisos, res);
        cargarEmparejados();
    }

    private void cargarEmparejados() {
        listaDispositivos.removeAllViews();
        BluetoothAdapter bt = EnlaceSerie.adaptador();
        if (bt == null) {
            txtLista.setText("Este teléfono no tiene Bluetooth.");
            return;
        }
        if (!bt.isEnabled()) {
            txtLista.setText("El Bluetooth está apagado. Actívelo y pulse Actualizar lista.");
            try {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), PIDE_ACTIVAR_BT);
            } catch (SecurityException | ActivityNotFoundException e) {
                aviso("No se pudo pedir que se active el Bluetooth: " + e.getClass().getSimpleName());
            }
            return;
        }
        try {
            Set<BluetoothDevice> emparejados = bt.getBondedDevices();
            txtLista.setText(emparejados.isEmpty()
                    ? "No hay equipos emparejados. Empareje el equipo desde los ajustes del teléfono."
                    : "El equipo aparece como COVIANDINA_<serie> (p. ej. COVIANDINA_SLV-002).");
            for (final BluetoothDevice d : emparejados) {
                Button b = new Button(this);
                b.setAllCaps(false);
                b.setText(EnlaceSerie.nombreDe(d) + "\n" + d.getAddress());
                b.setOnClickListener(v -> conectarCon(d));
                listaDispositivos.addView(b);
            }
        } catch (SecurityException e) {
            txtLista.setText("Falta el permiso de Bluetooth: " + e.getMessage());
        }
    }

    private void conectarCon(BluetoothDevice d) {
        if (Pruebas.get().enCurso()) {
            aviso("Hay pruebas en curso. Cancélelas antes de reconectar.");
            return;
        }
        if (EnlaceSerie.instancia().estaConectando()) {
            return;
        }
        esperandoConexion = true;
        EnlaceSerie.instancia().conectar(this, d);
        refrescar();
    }
}
