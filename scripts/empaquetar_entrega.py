#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
empaquetar_entrega.py — Empaquetador formal de entrega según entregar §9.

Construye el paquete .zip de entrega con el firmware (.hex + .md5), las APK
autorizadas de cada audiencia, los cuatro manuales en .docx sellados, la hoja
previa de campo y el LEEME.txt explicativo.

El archivo .zip resultante queda fuera de git (ignorado en .gitignore); en el
repositorio se guarda la constancia de huellas (05_Documentacion/HUELLAS-ENTREGA-V3.6.txt).
"""

import datetime
import hashlib
import os
import subprocess
import sys
import zipfile

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, os.path.join(r"D:\IT\Arquitec_Orquestador\skills\manual\scripts"))
try:
    import coherencia_manual as _coh
except ImportError:
    _coh = None

# Binarios oficiales exigidos por la arquitectura y la auditoría
BINARIOS_EXIGIDOS = {
    "firmware": {
        "origen": os.path.join(RAIZ, "01_Firmware", "RetroVertical_V3.6.X", "hex", "RetroVertical_V3.6.hex"),
        "md5_esperado": "9d5d5e3951c8aa83d1465f16f3733d27",
        "destino_zip": "firmware/RetroVertical_V3.6.hex",
    },
    "firmware_md5": {
        "origen": os.path.join(RAIZ, "01_Firmware", "RetroVertical_V3.6.X", "hex", "RetroVertical_V3.6.hex.md5"),
        "destino_zip": "firmware/RetroVertical_V3.6.hex.md5",
    },
    "apk_usuario": {
        "origen": os.path.join(RAIZ, "03_App_Movil", "RETRO-COVIANDINA-usuario-0.3.7-10.apk"),
        "md5_esperado": "6aecf3ac2251db29f78b74d324b920cc",
        "destino_zip": "apk/RETRO-COVIANDINA-usuario-0.3.7-10.apk",
    },
    "apk_calibrar": {
        "origen": os.path.join(RAIZ, "03_App_Movil", "Cov_3.6.5_calibrar.apk"),
        "md5_esperado": "dc0beaf5e110a72b7cbb288e4011cb68",
        "destino_zip": "apk/Cov_3.6.5_calibrar.apk",
    },
}

MANUALES_DOCX = [
    ("05_Documentacion/MANUAL-Usuario-App-Cliente-V3.6.docx", "manuales/MANUAL-Usuario-App-Cliente-V3.6.docx"),
    ("05_Documentacion/MANUAL-Usuario-App-Calibrar-V3.6.docx", "manuales/MANUAL-Usuario-App-Calibrar-V3.6.docx"),
    ("05_Documentacion/MANUAL-Operacion-Retrorreflectometro-Vertical-V3.6.docx", "manuales/MANUAL-Operacion-Retrorreflectometro-Vertical-V3.6.docx"),
    ("05_Documentacion/MANUAL-Interno-KnowHow-V3.6.docx", "manuales/MANUAL-Interno-KnowHow-V3.6.docx"),
    ("05_Documentacion/HOJA-PREVIA-CAMPO-V3.6.md", "manuales/HOJA-PREVIA-CAMPO-V3.6.md"),
]


def md5_fichero(ruta):
    h = hashlib.md5()
    with open(ruta, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()


def sha256_fichero(ruta):
    h = hashlib.sha256()
    with open(ruta, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()


def obtener_commit():
    try:
        out = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=RAIZ).decode().strip()
        return out[:7]
    except Exception:
        return "desconocido"


def generar_leeme(commit_id):
    hoy = datetime.date.today().isoformat()
    return f"""================================================================================
PAQUETE DE ENTREGA TECNICA — RETRORREFLECTOMETRO VERTICAL V3.6
DPI Ingenieria & Consultoria
================================================================================
Fecha de emision: {hoy}
Commit base: {commit_id}

1. COMPONENTES DE SOFTWARE Y FIRMWARE ENTREGADOS
--------------------------------------------------------------------------------
A) Firmware del Microcontrolador (PIC18F47K42):
   - Archivo: RetroVertical_V3.6.hex (en carpeta firmware/)
   - Version: Linea V3.6.2 (compilado con Microchip XC8 v2.10)
   - Bits de configuracion: EC FF F7 FF 9F FF FF DF FE FF
   - Huella MD5: 9d5d5e3951c8aa83d1465f16f3733d27
   - Manual asociado: MANUAL-Operacion-Retrorreflectometro-Vertical-V3.6.docx

B) Aplicacion Movil de Campo para Operadores ("Retro Coviandina"):
   - Archivo instalador: RETRO-COVIANDINA-usuario-0.3.7-10.apk (en carpeta apk/)
   - Identificador de paquete: com.dpi.retrousuario.coviandina
   - Version en pantalla: 0.3.7 (codigo de version 10)
   - Etiqueta del lanzador de Android: "Retro Coviandina"
   - Huella MD5: 6aecf3ac2251db29f78b74d324b920cc
   - Audiencia: Operadores de campo, tecnicos viales e interventoria.
   - Manual asociado: MANUAL-Usuario-App-Cliente-V3.6.docx

C) Aplicacion Movil de Calibracion Metrologica ("RTV Calibra"):
   - Archivo instalador: Cov_3.6.5_calibrar.apk (en carpeta apk/)
   - Identificador de paquete: com.dpi.retrov36.calibra
   - Version en pantalla: Cov_3.6.5_calibrar (codigo de version 10010)
   - Etiqueta del lanzador de Android: "RTV Calibra"
   - Huella MD5: dc0beaf5e110a72b7cbb288e4011cb68
   - Audiencia: Personal de metrologia y soporte tecnico de laboratorio DPI.
   - Manual asociado: MANUAL-Usuario-App-Calibrar-V3.6.docx
   - Nota: El renombrado formal a "Retro Coviandina Calibrar" corresponde a la tarea
     A4c del ROADMAP. Este paquete entrega la version probada y operativa en campo.

2. DOCUMENTACION TECNICA EN FORMATO WORD (.DOCX SELLADOS)
--------------------------------------------------------------------------------
Cada documento Word ha sido generado a partir de su fuente Markdown y sellado
con su hash SHA-256 en docProps/core.xml (identificador md-sha256).

1. MANUAL-Usuario-App-Cliente-V3.6.docx
   Manual de usuario de la aplicacion de campo "Retro Coviandina". Cero tecleo,
   asentamiento optico, dialogo Repetir/Saltar, GPS y exportacion ZIP.

2. MANUAL-Usuario-App-Calibrar-V3.6.docx
   Manual de la aplicacion de calibracion "RTV Calibra". Procedimiento en seis pasos,
   ajuste en memoria EEPROM (0x100), verificacion de tolerancias (+-5% canal 8 blanco,
   +-10% canal b amarillo) y emision de actas tecnicas.

3. MANUAL-Operacion-Retrorreflectometro-Vertical-V3.6.docx
   Manual de operacion del instrumento fisico SAT-LUX/V3 K42. Principio optico,
   sensor fotometrico con correccion CIE V(lambda), mantenimiento y recarga de bateria.

4. MANUAL-Interno-KnowHow-V3.6.docx
   Manual de preservacion de conocimiento tecnico para ingenieria DPI. Mapa completo
   de EEPROM, evaluacion polinomica de 32 bits, comandos UART1 y lecciones aprendidas.

5. HOJA-PREVIA-CAMPO-V3.6.md
   Lista de comprobacion obligatoria de una pagina antes de salir a carretera (A-10):
   PIN en mano, aviso de bloqueo permanente a los 5 fallos, verificacion de APK,
   ZIP en almacenamiento interno y regla estricta contra desinstalacion.

3. MATRIZ DE CORRESPONDENCIA AUDIENCIA <-> APLICACION <-> MANUAL
--------------------------------------------------------------------------------
| Audiencia | Aplicacion en telefono | Manual asignado |
| :--- | :--- | :--- |
| Operador en carretera | Retro Coviandina (0.3.7-10) | MANUAL-Usuario-App-Cliente-V3.6.docx |
| Metrologo en laboratorio | RTV Calibra (Cov_3.6.5-10010)| MANUAL-Usuario-App-Calibrar-V3.6.docx |
| Inspector / Mantenimiento | Hardware SAT-LUX/V3 K42 | MANUAL-Operacion-Retrorreflectometro-Vertical-V3.6.docx|
| Ingeniero de desarrollo | Ecosistema completo V3.6 | MANUAL-Interno-KnowHow-V3.6.docx |
================================================================================
"""


def main():
    print("Iniciando empaquetado de entrega segun entregar §9...")
    errores = []

    # 1. Comprobacion de binarios
    for clave, datos in BINARIOS_EXIGIDOS.items():
        origen = datos["origen"]
        if not os.path.isfile(origen):
            errores.append(f"Falta archivo exigido: {origen}")
            continue
        if "md5_esperado" in datos:
            md5_actual = md5_fichero(origen)
            if md5_actual != datos["md5_esperado"]:
                errores.append(f"MD5 invalido para {clave}: actual {md5_actual} != esperado {datos['md5_esperado']}")
            else:
                print(f"  [OK] {clave}: {os.path.basename(origen)} (MD5: {md5_actual})")

    # 2. Comprobacion de manuales y sellos
    for ruta_rel, destino_zip in MANUALES_DOCX:
        origen = os.path.join(RAIZ, ruta_rel)
        if not os.path.isfile(origen):
            errores.append(f"Falta manual exigido: {origen}")
            continue
        if origen.endswith(".docx") and _coh:
            ruta_md = origen[:-5] + ".md"
            estado, detalle = _coh.estado_docx(origen, ruta_md)
            if estado != "AL_DIA":
                errores.append(f"Manual {ruta_rel} no esta AL_DIA: {estado} ({detalle})")
            else:
                print(f"  [OK] Sello docx al dia: {ruta_rel} ({detalle})")
        else:
            print(f"  [OK] Documento adjunto: {ruta_rel}")

    if errores:
        print("\nABORTANDO: Se detectaron inconsistencias bloqueantes:")
        for e in errores:
            print(f"  - {e}")
        return 1

    # 3. Crear directorio de entrega
    dir_entregas = os.path.join(RAIZ, "entregas")
    os.makedirs(dir_entregas, exist_ok=True)

    commit_id = obtener_commit()
    nombre_zip = f"ENTREGA-RTV-V3.6.2-20260923-{commit_id}.zip"
    ruta_zip = os.path.join(dir_entregas, nombre_zip)

    # 4. Construir ZIP
    print(f"\nGenerando archivo comprimido: {nombre_zip}...")
    leeme_contenido = generar_leeme(commit_id)

    with zipfile.ZipFile(ruta_zip, "w", zipfile.ZIP_DEFLATED) as z:
        # Escribir LEEME.txt
        z.writestr("LEEME.txt", leeme_contenido.encode("utf-8"))

        # Escribir binarios
        for clave, datos in BINARIOS_EXIGIDOS.items():
            z.write(datos["origen"], datos["destino_zip"])

        # Escribir manuales
        for ruta_rel, destino_zip in MANUALES_DOCX:
            z.write(os.path.join(RAIZ, ruta_rel), destino_zip)

    # 5. Comprobaciones sobre el ZIP armado
    with zipfile.ZipFile(ruta_zip, "r") as z:
        archivos_en_zip = z.namelist()
        print(f"  Total de elementos en ZIP: {len(archivos_en_zip)}")
        # Cero artefactos de compilacion
        artefactos_prohibidos = [a for a in archivos_en_zip if any(p in a for p in [".pio", "build/", "dist/", "__pycache__", "node_modules"])]
        if artefactos_prohibidos:
            print(f"ERROR: Se encontraron artefactos prohibidos en el ZIP: {artefactos_prohibidos}")
            return 1

    zip_md5 = md5_fichero(ruta_zip)
    zip_sha256 = sha256_fichero(ruta_zip)
    zip_bytes = os.path.getsize(ruta_zip)

    print(f"\nPAQUETE CREADO EXITOSAMENTE:")
    print(f"  Ruta: {ruta_zip}")
    print(f"  Tamano: {zip_bytes} bytes")
    print(f"  MD5: {zip_md5}")
    print(f"  SHA-256: {zip_sha256}")

    # 6. Escribir registro de huellas en el repositorio (versionable en git)
    ruta_huellas = os.path.join(RAIZ, "05_Documentacion", "HUELLAS-ENTREGA-V3.6.txt")
    hoy = datetime.date.today().isoformat()
    with open(ruta_huellas, "w", encoding="utf-8") as f:
        f.write(f"HUELLAS DE ENTREGA — RETRORREFLECTOMETRO VERTICAL V3.6\n")
        f.write(f"Fecha: {hoy}\n")
        f.write(f"Commit base: {commit_id}\n")
        f.write(f"Paquete ZIP: {nombre_zip}\n")
        f.write(f"Tamano ZIP: {zip_bytes} bytes\n")
        f.write(f"MD5 ZIP: {zip_md5}\n")
        f.write(f"SHA-256 ZIP: {zip_sha256}\n\n")
        f.write(f"CONTENIDO DEL PAQUETE Y HUELLAS INDIVIDUALES:\n")
        f.write(f"- Firmware: RetroVertical_V3.6.hex\n")
        f.write(f"  MD5: {BINARIOS_EXIGIDOS['firmware']['md5_esperado']}\n")
        f.write(f"- APK Usuario: RETRO-COVIANDINA-usuario-0.3.7-10.apk\n")
        f.write(f"  MD5: {BINARIOS_EXIGIDOS['apk_usuario']['md5_esperado']}\n")
        f.write(f"- APK Calibracion: Cov_3.6.5_calibrar.apk\n")
        f.write(f"  MD5: {BINARIOS_EXIGIDOS['apk_calibrar']['md5_esperado']}\n")
        for ruta_rel, destino_zip in MANUALES_DOCX:
            f.write(f"- Documento: {destino_zip}\n")
            f.write(f"  SHA-256: {sha256_fichero(os.path.join(RAIZ, ruta_rel))}\n")

    print(f"\nRegistro de huellas guardado en: {ruta_huellas}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
