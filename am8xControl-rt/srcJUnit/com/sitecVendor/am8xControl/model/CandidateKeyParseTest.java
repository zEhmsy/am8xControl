package com.sitecVendor.am8xControl.model;

import com.sitecVendor.am8xControl.model.CandidateKey.Kind;
import com.sitecVendor.am8xControl.parser.Am8xDeviceDescriptor;
import com.sitecVendor.am8xControl.parser.Am8xSubModuleDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CandidateKeyParseTest {

    @Test
    void parsesSensorSlot() {
        CandidateKey k = CandidateKey.parse("CENTRALE_1", "L01S002").get();
        assertEquals(Kind.SENSOR, k.getKind());
        assertEquals(1, k.getLoop());
        assertEquals(2, k.getPos());
        assertEquals("CENTRALE_1", k.getPanel());
    }

    @Test
    void parsesModuleSlot() {
        CandidateKey k = CandidateKey.parse("P", "L02M017").get();
        assertEquals(Kind.MODULE, k.getKind());
        assertEquals(2, k.getLoop());
        assertEquals(17, k.getPos());
        assertEquals(-1, k.getChannel());
    }

    @Test
    void parsesSubModuleSlot() {
        CandidateKey k = CandidateKey.parse("P", "L01M003_4").get();
        assertEquals(Kind.SUB_MODULE, k.getKind());
        assertEquals(3, k.getParentModulePos());
        assertEquals(4, k.getChannel());
    }

    @Test
    void roundTripsEverySlotNameItProduces() {
        CandidateKey[] originals = {
            CandidateKey.parse("P", "L01S002").get(),
            CandidateKey.parse("P", "L09M123").get(),
            CandidateKey.parse("P", "L03M007_2").get()
        };
        for (CandidateKey original : originals) {
            CandidateKey reparsed = CandidateKey.parse("P", original.toSlotName()).get();
            assertEquals(original, reparsed, "round-trip fallito per " + original.toSlotName());
            assertEquals(original.toSlotName(), reparsed.toSlotName());
            assertEquals(original.getKind(), reparsed.getKind());
        }
    }

    /**
     * Round-trip VERO: le CandidateKey qui sono costruite via forDevice()/
     * forSubModule() a partire da descriptor POJO indipendenti, MAI passando
     * per parse(). La versione precedente costruiva gli originali chiamando
     * parse() stesso, quindi testava solo l'idempotenza di parse(), non che
     * toSlotName()/parse() siano davvero l'una l'inversa dell'altra.
     * Am8xDeviceDescriptor/Am8xSubModuleDescriptor sono POJO puri (nessun
     * import baja), quindi non serve alcun runtime Niagara per costruirli.
     */
    @Test
    void roundTripsFromIndependentlyConstructedDescriptors() {
        // SENSOR: deviceTypeId=0, deviceType non a prefisso "M"
        Am8xDeviceDescriptor sensor = new Am8xDeviceDescriptor(
                "AM8200N", "CENTRALE_1", 1, 2, "S001", "Sensore ingresso", 10, "Zona A", 0);
        CandidateKey sensorKey = CandidateKey.forDevice(sensor);
        assertEquals(Kind.SENSOR, sensorKey.getKind());

        // MODULE: deviceTypeId=1, senza sub-moduli
        Am8xDeviceDescriptor module = new Am8xDeviceDescriptor(
                "AM8200N", "CENTRALE_1", 9, 123, "M720", "Modulo I/O", 20, "Zona B", 1);
        CandidateKey moduleKey = CandidateKey.forDevice(module);
        assertEquals(Kind.MODULE, moduleKey.getKind());

        // SUB_MODULE: modulo padre + un suo canale
        Am8xDeviceDescriptor parent = new Am8xDeviceDescriptor(
                "AM8200N", "CENTRALE_1", 3, 7, "M720", "Modulo canali", 30, "Zona C", 1);
        Am8xSubModuleDescriptor sub = new Am8xSubModuleDescriptor("M720CH", "Canale 2", 2, 31, "Zona C.2");
        CandidateKey subKey = CandidateKey.forSubModule(parent, sub);
        assertEquals(Kind.SUB_MODULE, subKey.getKind());

        CandidateKey[] originals = { sensorKey, moduleKey, subKey };
        for (CandidateKey original : originals) {
            String slotName = original.toSlotName();
            CandidateKey reparsed = CandidateKey.parse("CENTRALE_1", slotName).get();
            assertEquals(original, reparsed, "round-trip fallito per " + slotName);
            assertEquals(original.getKind(), reparsed.getKind(), "kind diverso per " + slotName);
            assertEquals(original.getLoop(), reparsed.getLoop());
            assertEquals(original.getPos(), reparsed.getPos());
            assertEquals(original.getParentModulePos(), reparsed.getParentModulePos());
            assertEquals(original.getChannel(), reparsed.getChannel());
        }
    }

    @Test
    void returnsEmptyOnMalformedNamesInsteadOfThrowing() {
        String[] bad = { "", "L01", "LxxS002", "S002", "L01X002", "L01S002_", "L01M003_x",
                         "Loop01Sensor2", "CENTRALE", "L01S002 ", "L01S002_1" };
        for (String s : bad) {
            Optional<CandidateKey> r = CandidateKey.parse("P", s);
            assertFalse(r.isPresent(), "doveva essere empty: '" + s + "'");
        }
    }

    @Test
    void returnsEmptyOnNullInsteadOfThrowing() {
        assertFalse(CandidateKey.parse("P", null).isPresent());
    }
}
