# Contradicciones abiertas de la RTV

**Ninguna de estas se ha cerrado midiendo contra un equipo físico.** Se dejan escritas, sin elegir, hasta
que alguien las mida. Si alguien las cierra, que sustituya la entrada por el resultado medido y su fecha.

Estado: RTV 1.0.0-rc3, 19-sep-2026.

---

## C-01 · La pausa corta de 150 ms se justifica con algo que en la V4.6 ya no es cierto

**Las dos fuentes.**

`Cliente.java:30-33` justifica así la pausa corta entre dos tramas `#` seguidas:

> *"Entre dos tramas '#' seguidas con un V3.6 ya detectado basta una pausa corta: según el contrato (§3 y
> O-05) una trama '#' nunca dispara medida."*

Pero en la V4.6 **`#X,k#` sí mide**: arma `pedidoX` y el firmware entra en `ST_MEDIR_AP`, con 1000 ms de
calentamiento de luz y 1000 muestras a 1 ms (`V4.6:Calibracion.c:980-981`, `Optical_Capture.c:137,181`,
`Optical_Capture.h:13`). Y la condición de `Cliente.java:127` sólo exige `proto.administra()`, que
`ProtocoloV46` cumple (`ProtocoloV46.java`), así que la app **ya está encadenando `#X` con la pausa corta**.

**Lo que sí está medido** (ejecutando `Ritmo`, no razonándolo: `RitmoTest`):

| Caso | Espera | Coste por disparo |
| :--- | ---: | ---: |
| `#X` → `#X` (hoy) | 0 ms | — |
| `#X` → `#T#` → `#X` | 0 + 142 ms | **+75 ms** (+1,6 % del banco) |
| `#X` → algo sin `#` → `#X` | 600 + 1492 ms | **+1050 ms** |

La pausa corta se cuenta desde el **envío** anterior, y como `#X` tarda ~2 s en contestar, esos 150 ms ya
han pasado: la siguiente petición sale sin esperar. La pausa desde el último byte recibido es **cero** con
la corta (`Ritmo.pausaRxMs`).

**Las dos consecuencias, sin elegir entre ellas:**

- Si 150 ms **valen**, el coste de intercalar temperatura es el de la tabla y no hay problema.
- Si tras un `#X` hiciera falta la pausa larga, el banco representativo de 1410 disparos pasaría de 110 min
  a unos **145 min**, y habría que replantear el ritmo entero, no sólo la temperatura.

**Lo que dice el fuente del firmware, y que inclina pero no cierra:** una `#X` que llega con una medida en
curso responde `#ERR,OCUPADO#` (`V4.6:Calibracion.c:980`, guarda `aplicacionOcupada() || pedidoX`), y una
trama `#` necesita 5 ms de silencio antes de procesarse (`Serial.h:14-15`). Es decir, **el riesgo se vería
como error, no como una cifra mala**. La app ya lo dice con esas palabras desde la rc3
(`LecturaX.java`, rama de `#ERR`).

**Cómo se cierra:** con un V4.6 grabado delante, encadenando `#X` y mirando si aparece `OCUPADO` y si las
x salen iguales que con pausa larga. Va a la lista de lo que hay que mirar el día del banco de Nordeste.

---

## C-02 · El campo de temperatura de circuito de `#T#` es estructuralmente 0

`#T#` devuelve `#T,<Tcirc>,<Topt>#` (`V4.6:Calibracion.c:986,988`). El primer campo es `ap.fTempCircuit`, y
**el único sitio que lo asigna** (`V4.6:Aplicacion.c:216`) está dentro de `ST_TEMPCIRC_AP`, un estado al
que **no llega ningún `cambiarEstado()`**. Verificado dos veces. Es miembro de un global, así que vale 0,0
siempre.

No se elige: la app **registra lo que venga** y anota el motivo junto al valor
(`Tramas.T_CIRCUITO_ESTRUCTURAL`). **No se ajusta con esa columna** mientras no se cierre.

**Cómo se cierra:** o el firmware asigna `fTempCircuit`, o se retira el campo del contrato.

---

## C-03 · El contrato de `#X` y de `#T` tiene dos versiones vivas a la vez

| | Candidato del firmware | Borrador del contrato |
| :--- | :--- | :--- |
| `#X,<k>#` | `#X,<k>,<x>#` (`V4.6:Calibracion.c:774-781`) | `#X,<k>,<x>,<TO>,<a>#` (RF-FW-B13, `PROTOCOLO-V4.6-BORRADOR.md:215,251`) |
| `#T#` | `#T,<Tcirc>,<Topt>#` (`:986,988`) | `#T,<Tcirc>,<TO>,<estado>#` (RF-FW-B13) |

Y el propio borrador se contradice consigo mismo: `:203` dice que `#T#` no está implementada, y el código
la implementa en `Calibracion.c:983-990`. **Manda el código.**

No se elige: la app **acepta las dos formas** (`Tramas.parsearXCompleta`, `Tramas.parsearT`) y usa la
temperatura embebida en `#X` cuando venga, que es la vía definitiva y cuesta cero.

**Cómo se cierra:** cuando el firmware implemente RF-FW-B13, o cuando el borrador se corrija.

---

## C-04 · Un sensor óptico muerto se parece a un día fresco

Cuando la lectura cruda pasa de 70 °C, el firmware **fuerza `TO = 0`**
(`V4.6:Temp_Optica.c:63-66`). Un 0 en esa columna es indistinguible de una temperatura real baja si no se
mira nada más, y **un ajuste hecho con el sensor averiado saldría plano y se leería como "aquí no hay
deriva"**.

No se elige, se rodea: la rc3 añade la columna `T_estado` (`OK` / `DESC` / `SIN`) y, con `DESC`, **deja la
columna de TO vacía en vez de escribir ese 0** (`Tramas.parsearT`, `Campana.T_ESTADO_DESC`). El candidato
todavía no manda el tercer campo, así que hoy esa marca sólo llega si el firmware implementa RF-FW-B13.

**Cómo se cierra:** el día que el firmware mande el estado. Mientras tanto, **un 0 en TO con el candidato
actual hay que mirarlo con el equipo delante antes de darlo por bueno.**

---

## C-05 · El umbral de recorrido de TO para ajustar F(TO) y S(TO) no está medido

`V_corregida = F(TO)·V + S(TO)` (RF-FW-B10/B11). Si todas las medidas se tomaron a la misma TO, el sistema
es degenerado: infinitas parejas (F, S) explican los datos igual de bien y una regresión devuelve una
cualquiera **con toda la apariencia de un buen ajuste**.

`AjusteTemperatura.RECORRIDO_TO_MINIMO` vale **NaN a propósito**, y con NaN la guarda **nunca autoriza el
ajuste**: deja los coeficientes de fábrica y lo dice. El número lo está midiendo quien tiene los datos
reales delante.

**Cómo se cierra:** escribiendo el umbral con el documento que lo sostenga. La guarda ya está y no hay que
tocar nada más.
