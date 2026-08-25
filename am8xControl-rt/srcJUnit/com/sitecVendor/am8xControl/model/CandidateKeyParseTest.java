package com.sitecVendor.am8xControl.model;

import com.sitecVendor.am8xControl.model.CandidateKey.Kind;
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

    @Test
    void returnsEmptyOnMalformedNamesInsteadOfThrowing() {
        String[] bad = { "", "L01", "LxxS002", "S002", "L01X002", "L01S002_", "L01M003_x",
                         "Loop01Sensor2", "CENTRALE", "L01S002 " };
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
