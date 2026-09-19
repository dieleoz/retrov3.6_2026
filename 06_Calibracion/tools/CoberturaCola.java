import com.dpi.retrov36.Asistente;
import com.dpi.retrov36.BancoCola;
import com.dpi.retrov36.Fabrica;
import com.dpi.retrov36.Patron;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Cobertura por codigo de una cola del banco, con el codigo de la app (banco_representativo.py).
 *
 * - Carga la cola con BancoCola.cargar: dice si la app la admite (md5 en MD5_PERMITIDOS).
 * - Para cada codigo, los patrones de AJUSTE y RE-MEDIDA de la cola (FlujoCalibracion.patronesAjuste)
 *   pasan por Asistente.cobertura, sin ancla y con el ancla (certificado 0, RF-APP-42, como hace
 *   Asistente.proponer para la recta anclada).
 * La cola se lee aqui con un parser propio, porque BancoCola.cargar rechaza un md5 desconocido.
 */
public final class CoberturaCola {

    public static void main(String[] a) throws Exception {
        byte[] b = Files.readAllBytes(Paths.get(a[0]));
        try {
            BancoCola c = BancoCola.cargar(b);
            System.out.println("BancoCola.cargar: ADMITIDA, " + c.pasos.size() + " pasos, md5 " + c.md5);
        } catch (IllegalArgumentException e) {
            System.out.println("BancoCola.cargar: " + e.getMessage());
        }
        List<String> lineas = Files.readAllLines(Paths.get(a[0]));
        String[] cab = lineas.get(0).split(",", -1);
        int iPaso = idx(cab, "paso"), iPat = idx(cab, "patron"), iCol = idx(cab, "color"), iTipo = idx(cab, "tipo"),
                iCert = idx(cab, "valor_certificado"), iCod = idx(cab, "codigo_equipo"), iUso = idx(cab, "uso");
        for (char k : Fabrica.CODIGOS) {
            // FlujoCalibracion.patronesAjuste: AJUSTE y RE-MEDIDA; si el codigo no tiene ningun AJUSTE,
            // todos sus patrones (VERIFICACION incluidos)
            List<Patron> l = new ArrayList<>();
            List<Patron> todos = new ArrayList<>();
            boolean hayAjuste = false;
            String tipoClase = "";
            for (int i = 1; i < lineas.size(); i++) {
                String[] f = lineas.get(i).split(",", -1);   // las columnas que se usan van antes de la nota
                if (f.length <= iUso || !"PATRON".equals(f[iPaso]) || !String.valueOf(k).equals(f[iCod])) {
                    continue;
                }
                Patron p = new Patron(f[iPat], Double.parseDouble(f[iCert]), f[iTipo], f[iCol]);
                todos.add(p);
                hayAjuste |= "AJUSTE".equals(f[iUso]);
                if ("AJUSTE".equals(f[iUso]) || "RE-MEDIDA".equals(f[iUso])) {
                    l.add(p);
                }
                tipoClase = f[iTipo];
            }
            if (!hayAjuste) {
                l = todos;
            }
            Asistente.Cobertura sin = Asistente.cobertura(k, l);
            String con = "";
            if (!l.isEmpty()) {
                List<Patron> l2 = new ArrayList<>(l);
                l2.add(new Patron("OSCURO", 0, tipoClase, Fabrica.color(k)));
                con = " | con ancla: " + Asistente.cobertura(k, l2).texto();
            }
            System.out.println(sin.texto() + con);
        }
    }

    private static int idx(String[] cab, String n) {
        for (int i = 0; i < cab.length; i++) {
            if (cab[i].equals(n)) {
                return i;
            }
        }
        throw new IllegalArgumentException("falta la columna " + n);
    }
}
