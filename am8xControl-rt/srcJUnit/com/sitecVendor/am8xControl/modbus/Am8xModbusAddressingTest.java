package com.sitecVendor.am8xControl.modbus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Copre le quattro formule di indirizzamento Modbus, pura aritmetica intera
 * senza dipendenze dal runtime Niagara. Un off-by-one qui punta il point
 * sbagliato sul registro sbagliato — potenzialmente lo stato di un allarme
 * incendio letto dal punto di un altro dispositivo.
 *
 * I valori attesi per i casi "worked example" sono presi dal javadoc di
 * {@link Am8xModbusAddressing}, non ricalcolati qui.
 */
class Am8xModbusAddressingTest {

    // ------------------------------------------------------------
    // Worked examples dal javadoc (loop=1)
    // ------------------------------------------------------------

    @Test
    void sensorWorkedExample_L1P1() {
        // Sensore L1 P1 → state=7001, analog=8001
        assertEquals(7001, Am8xModbusAddressing.sensorState(1, 1));
        assertEquals(8001, Am8xModbusAddressing.sensorAnalog(1, 1));
    }

    @Test
    void moduleWorkedExample_M720_L1P1Ch1() {
        // M720 L1 P1 ch1 → state=5011, analog=6011
        assertEquals(5011, Am8xModbusAddressing.moduleState(1, 1, 1));
        assertEquals(6011, Am8xModbusAddressing.moduleAnalog(1, 1, 1));
    }

    // ------------------------------------------------------------
    // Boundary: loop/pos/channel a zero e su un altro loop
    // ------------------------------------------------------------

    @Test
    void sensorState_boundary_zeroPos() {
        // state = loop*5000 + 2000 + pos, con pos=0 resta il termine base del loop
        assertEquals(2000, Am8xModbusAddressing.sensorState(0, 0));
        assertEquals(7000, Am8xModbusAddressing.sensorState(1, 0));
    }

    @Test
    void sensorAnalog_boundary_zeroPos() {
        assertEquals(3000, Am8xModbusAddressing.sensorAnalog(0, 0));
    }

    @Test
    void sensorState_secondLoop() {
        // loop=2, pos=1 → 2*5000 + 2000 + 1 = 12001
        assertEquals(12001, Am8xModbusAddressing.sensorState(2, 1));
        assertEquals(13001, Am8xModbusAddressing.sensorAnalog(2, 1));
    }

    @Test
    void moduleState_boundary_zeroModulePosAndChannel() {
        // state = loop*5000 + modulePos*10 + channel, tutti zero → solo il termine del loop
        assertEquals(5000, Am8xModbusAddressing.moduleState(1, 0, 0));
        assertEquals(6000, Am8xModbusAddressing.moduleAnalog(1, 0, 0));
    }

    @Test
    void moduleState_secondLoopDifferentChannel() {
        // loop=2, modulePos=3, channel=2 → 2*5000 + 3*10 + 2 = 10032
        assertEquals(10032, Am8xModbusAddressing.moduleState(2, 3, 2));
        assertEquals(11032, Am8xModbusAddressing.moduleAnalog(2, 3, 2));
    }
}
