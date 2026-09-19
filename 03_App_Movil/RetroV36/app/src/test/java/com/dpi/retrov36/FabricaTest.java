package com.dpi.retrov36;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * RF-APP-08: la tabla de fabrica de la app se compara contra el TEXTO de
 * 01_Firmware/base_2020_d089f962/RetroVertical1.X/ecuacionesCalibracion.c,
 * para que no se desvie de lo que hay en el fuente.
 */
public class FabricaTest {

    private static final String FUENTE =
            "../../../01_Firmware/base_2020_d089f962/RetroVertical1.X/ecuacionesCalibracion.c";

    /** Funcion del .c que usa cada codigo (ecuacionesColoresApp, :137-193). */
    private static final String[][] FUNCION = {
            {"1", "blancoIntenso"}, {"2", "amarilloIntenso"}, {"3", "verdeIntenso"},
            {"4", "rojoIntenso"}, {"5", "azulIntenso"}, {"6", "naranjaIntenso"},
            {"7", "blancoOpaco"}, {"8", "amarilloOpaco"}, {"a", "verdeOpaco"},
            {"b", "rojoOpaco"}, {"c", "azulOpaco"}, {"d", "naranjaOpaco"}};

    @Test
    public void lasDoceEcuacionesCoincidenConElFuente() throws Exception {
        File f = new File(FUENTE);
        org.junit.Assert.assertTrue("fuente no disponible: " + f.getAbsolutePath(), f.exists());
        String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.ISO_8859_1);
        Pattern fn = Pattern.compile("void\\s+(\\w+)\\s*\\(void\\)\\s*\\{\\s*reflectivityValue\\s*=\\s*\\(double\\)\\((.*?)\\);\\s*\\}",
                Pattern.DOTALL);
        Map<String, Ecuacion> leidas = new HashMap<>();
        Matcher m = fn.matcher(src);
        while (m.find()) {
            leidas.put(m.group(1), parsear(m.group(2)));
        }
        for (String[] par : FUNCION) {
            Ecuacion delFuente = leidas.get(par[1]);
            assertNotNull(par[1], delFuente);
            Ecuacion app = Fabrica.ecuacion(par[0].charAt(0));
            assertEquals(par[1] + " c3", delFuente.c3, app.c3, 0);
            assertEquals(par[1] + " c2", delFuente.c2, app.c2, 0);
            assertEquals(par[1] + " c1", delFuente.c1, app.c1, 0);
            assertEquals(par[1] + " c0", delFuente.c0, app.c0, 0);
        }
    }

    /** Suma de terminos "k*(double)reflectivityValue*..." y una constante. */
    private static Ecuacion parsear(String expr) {
        String e = expr.replace("(double)", "").replace("reflectivityValue", "X").replaceAll("\\s+", "");
        double[] c = new double[4];
        Matcher t = Pattern.compile("([+-]?)([0-9.]+)((?:\\*X)*)").matcher(e);
        while (t.find()) {
            if (t.group(2).isEmpty()) {
                continue;
            }
            double v = Double.parseDouble(t.group(2)) * ("-".equals(t.group(1)) ? -1 : 1);
            int grado = t.group(3).length() / 2;
            c[grado] += v;
        }
        return new Ecuacion(c[3], c[2], c[1], c[0]);
    }
}
