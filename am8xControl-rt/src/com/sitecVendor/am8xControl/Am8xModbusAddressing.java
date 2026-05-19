package com.sitecVendor.am8xControl;

/**
 * Calcola gli indirizzi Modbus per i point di ciascun dispositivo AM-8x00.
 *
 * Formule da modbus_coversion.md:
 *   Sensori : alarm        = (loop * 5000) + 2000 + n_sensore
 *             cameraValue  = (loop * 5000) + 3000 + n_sensore
 *   Moduli  : alarm        = (loop * 5000) + (n_modulo * 10) + submodule
 */
public final class Am8xModbusAddressing {

    private Am8xModbusAddressing() {}

    // ---------------------------------------------------------------
    // Sensori diretti su loop (SENS OTTICO, TH, ecc.)
    // ---------------------------------------------------------------

    /** Registro allarme sensore: (loop * 5000) + 2000 + pos */
    public static int sensorAlarm(int loop, int pos) {
        return loop * 5000 + 2000 + pos;
    }

    /** Valore camera / stato secondario sensore: (loop * 5000) + 3000 + pos */
    public static int sensorFault(int loop, int pos) {
        return loop * 5000 + 3000 + pos;
    }

    // ---------------------------------------------------------------
    // Sub-moduli di un M720 (MON3, IN3, OUT3, ecc.)
    //   modulePos = positionOnLoop del modulo M720 padre
    //   channel   = numero del sub-modulo (positionOnLoop del child entry)
    // ---------------------------------------------------------------

    /** Registro allarme sub-modulo: (loop * 5000) + (modulePos * 10) + channel */
    public static int moduleAlarm(int loop, int modulePos, int channel) {
        return loop * 5000 + modulePos * 10 + channel;
    }

    public static int moduleFault(int loop, int modulePos, int channel) {
        return loop * 5000 + modulePos * 10 + channel + 1000;
    }
}
