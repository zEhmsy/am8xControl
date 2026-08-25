package com.sitecVendor.am8xControl.semantics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    @Test
    void collisionKeyDiffersAcrossPanelsForTheSameDeviceSlot() {
        // Due panel diversi con lo stesso deviceSlot (es. entrambi "L01S002",
        // caso normale su siti multi-panel cablati in modo identico) NON sono
        // una collisione: le chiavi devono restare distinte.
        String keyPanelA = Am8xSlotNames.collisionKey("CENTRALE_1", "L01S002");
        String keyPanelB = Am8xSlotNames.collisionKey("CENTRALE_2", "L01S002");
        assertNotEquals(keyPanelA, keyPanelB);
    }

    @Test
    void collisionKeyMatchesForDifferentCandidatesUnderSamePanel() {
        // Due candidate diversi che reclamano lo stesso deviceSlot SOTTO LO
        // STESSO panel devono produrre la stessa chiave: è la collisione vera.
        String key1 = Am8xSlotNames.collisionKey("CENTRALE_1", "L01S002");
        String key2 = Am8xSlotNames.collisionKey("CENTRALE_1", "L01S002");
        assertEquals(key1, key2);
    }

    @Test
    void collisionKeyDoesNotAliasPanelDigitsIntoSlotDigits() {
        // Il separatore deve impedire che concatenazioni "ambigue" (panel che
        // finisce con cifre + slot che inizia con cifre) collidano per caso.
        String key1 = Am8xSlotNames.collisionKey("PANEL1", "23L01S002");
        String key2 = Am8xSlotNames.collisionKey("PANEL123", "L01S002");
        assertNotEquals(key1, key2);
    }
}
