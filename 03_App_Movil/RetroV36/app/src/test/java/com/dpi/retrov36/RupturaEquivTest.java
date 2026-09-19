/*
 * Prueba de ruptura del revisor P15 (05_Documentacion/REVISION-P15-QA-3.6.16.md, ef7ea10), llevada al arbol en la
 * 3.6.17. Original en el scratchpad de la revision: mio/src/MioEquivTest.java. Imprime lo observado (SALIDA) y, donde la 3.6.17 corrige
 * el defecto, lo comprueba con assert.
 */
package com.dpi.retrov36;
import org.junit.Test;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.Files; import java.util.*;
public class RupturaEquivTest {
    static final String MAC = "00:21:13:05:19:3B";
    static String asset(String n) throws Exception { return new String(Files.readAllBytes(new File("src/main/assets/" + n).toPath()), StandardCharsets.UTF_8); }
    static Campana.Serie medir(Campana c, String patron, double x, int k, int m) throws Exception {
        Campana.Serie s = c.nuevaSerie("2026-09-20T09:00:00-0500", "SLV-002", MAC, "V3.6", patron, 0, 'e');
        for (int j = 1; j <= k; j++) for (int i = 0; i < m; i++) c.agregarDisparo(s, j, "t", "::" + Math.round(x), x);
        c.cerrar(s, "OK", true, ""); c.elegir(s); return s;
    }
    @Test public void verificacionYAjusteDelMismoGrupo() throws Exception {
        List<Patron> cat; try (InputStreamReader r = new InputStreamReader(new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) { cat = Patron.leer(r); }
        BancoCola q = BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/cola_banco_representativo_v2.csv").toPath()));
        Campana c = new Campana(cat, "SLV-002", MAC); c.escribirEn(new StringWriter()); c.elegirCola("REPRESENTATIVO", q.md5);
        Decisiones d = Decisiones.leer(asset("decisiones.csv")); BancoPrevio.Grupos g = BancoPrevio.Grupos.leer(asset("grupos_patrones_equivalentes.csv"));
        // el operador mide P81 (orden 82) a 5x4 y P18 (orden 84) a 5x4, como pide la app; sale y vuelve a entrar
        Campana.Serie s81 = medir(c, "P81", 846, 5, 4); c.anotarPaso(82, "HECHO", s81.id, "t", "");
        Campana.Serie s18 = medir(c, "P18", 813, 5, 4); c.anotarPaso(84, "HECHO", s18.id, "t", "");
        BancoPrevio.Resultado r = BancoPrevio.aplicar(c, q, g, d, "SLV-002", "t");
        System.out.println("hechos=" + r.hechos + " " + r.texto);
        System.out.println("orden 83 (P127 AJUSTE del 5) = " + c.pasos().get(83) + " serie " + c.seriePaso(83));
        System.out.println("orden 85 (P19 VERIFICACION del 5) = " + c.pasos().get(85) + " serie " + c.seriePaso(85));
        System.out.println("patronesAjuste(5) = " + FlujoCalibracion.patronesAjuste(q, '5', Arrays.asList("P81")));
        System.out.println("elegida P127 = " + c.elegida("P127"));
    }
}
