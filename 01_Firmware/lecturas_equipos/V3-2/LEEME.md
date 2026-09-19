# Segundo equipo "V3" - lectura ICSP 19-sep-2026 14:27

- PICkit 3 BUR195068601, IPE 5.50. PIC18F47K42 rev A001.
- **Sin proteccion de codigo** (CONFIG 0x300008 = FF; SLV-002 tenia FE): el firmware original **se ha podido leer y queda respaldado**.
- lectura_V3-2_2026-09-19.hex md5 a1c91038f4989b844a8164b36723e3f9. CONFIG: 8C FF F7 FF 9F FF FF FF FF FF. 37 330 bytes de programa (hasta 0x192a1). EEPROM: 0x10 0x00 0x00 en 0x310000.
- **No es el firmware V3 2020**: difiere en 35 763 bytes de la base d089f962. Sus cadenas son las de la familia V4 (, , , , en 0xff5e-0xffc2).
- **No grabar la V3.6 hasta identificar la placa**: si es la Retro_smd_v1 (V4.1, UART1 en RC4/RC5) y no la SATLUX H-IoT (UART1 en RC0/RC1), la V3.6 dejaria el Bluetooth sin conexion.
