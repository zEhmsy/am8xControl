package com.sitecVendor.am8xControl.semantics;

import com.sitecVendor.am8xControl.model.CandidateKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Am8xDisplayNameFormatterTest {

    private static Am8xIdentity id(String label, String zone) {
        return new Am8xIdentity("CENTRALE_1", 1, 2, -1,
                CandidateKey.Kind.SENSOR, "NFXI-OPT", label, zone, "Corridoio");
    }

    private static final String FMT = "%name% — %deviceLabel%";

    @Test
    void substitutesPlaceholders() {
        assertEquals("L01S002 — Ottico ingresso",
                Am8xDisplayNameFormatter.render(FMT, "L01S002", id("Ottico ingresso", "12")));
    }

    @Test
    void dropsOrphanSeparatorWhenLabelEmpty() {
        assertEquals("L01S002",
                Am8xDisplayNameFormatter.render(FMT, "L01S002", id("", "12")));
    }

    @Test
    void neverReturnsEmptyStringFallsBackToCanonicalName() {
        assertEquals("L01S002",
                Am8xDisplayNameFormatter.render("%deviceLabel%", "L01S002", id("", "")));
    }

    @Test
    void treatsPercentInLabelAsLiteralText() {
        assertEquals("L01S002 — Umidita 50%",
                Am8xDisplayNameFormatter.render(FMT, "L01S002", id("Umidita 50%", "12")));
    }

    @Test
    void collapsesDoubleSpaces() {
        assertEquals("L01S002 12",
                Am8xDisplayNameFormatter.render("%name%  %zone%  %deviceLabel%",
                        "L01S002", id("", "12")));
    }

    @Test
    void supportsLoopAndZonePlaceholders() {
        assertEquals("1/12",
                Am8xDisplayNameFormatter.render("%loop%/%zone%", "L01S002", id("x", "12")));
    }
}
