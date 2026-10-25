package com.sitecVendor.am8xControl.modbus;

/**
 * Calcola gli indirizzi Modbus per i point di ciascun dispositivo AM-8x00.
 *
 * Due registri per device:
 *   1. Qualificatore (Stato / Event State) — dice come sta il sensore (1-6: Normale, Preallarme, Guasto, Escluso, Test, Allarme)
 *   2. Analogico (Misura / Loop Value) — valore fisico misurato (%, ppm, °C, ecc.)
 *
 * Formule:
 *   Sensori : state   = (loop * 5000) + 2000 + pos
 *             analog  = state + 1000
 *   Moduli  : state   = (loop * 5000) + (modulePos * 10) + channel
 *             analog  = state + 1000
 *
 * Esempi (loop=1):
 *   Sensore L1 P1  → state=7001, analog=8001
 *   M720 L1 P1 ch1 → state=5011, analog=6011
 */
public final class Am8xModbusAddressing {

    private Am8xModbusAddressing() {}

    // Sensori
    public static int sensorState(int loop, int pos) {
        return loop * 5000 + 2000 + pos;
    }
    public static int sensorAnalog(int loop, int pos) {
        return sensorState(loop, pos) + 1000;
    }

    // Moduli
    public static int moduleState(int loop, int modulePos, int channel) {
        return loop * 5000 + modulePos * 10 + channel;
    }
    public static int moduleAnalog(int loop, int modulePos, int channel) {
        return moduleState(loop, modulePos, channel) + 1000;
    }

}
