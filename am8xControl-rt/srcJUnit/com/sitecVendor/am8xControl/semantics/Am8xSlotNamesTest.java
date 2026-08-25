package com.sitecVendor.am8xControl.semantics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Am8xSlotNamesTest {

    @Test
    void returnsBaseWhenFree() {
        Set<String> taken = new HashSet<>();
        assertEquals("L01S002", Am8xSlotNames.unique("L01S002", taken::contains));
    }

    @Test
    void appendsCounterWhenBaseTaken() {
        Set<String> taken = new HashSet<>(Arrays.asList("L01S002"));
        assertEquals("L01S0022", Am8xSlotNames.unique("L01S002", taken::contains));
    }

    @Test
    void skipsOverRunOfTakenNames() {
        Set<String> taken = new HashSet<>(Arrays.asList("X", "X2", "X3", "X4"));
        assertEquals("X5", Am8xSlotNames.unique("X", taken::contains));
    }

    @Test
    void fallsBackToBaseWhenExhausted() {
        assertEquals("X", Am8xSlotNames.unique("X", s -> true));
    }
}
