package com.sitecVendor.am8xControl.semantics;

import java.util.function.Predicate;

/** Utility di naming degli slot. Pura: nessuna dipendenza da baja, testabile come POJO. */
public final class Am8xSlotNames {

    private Am8xSlotNames() {}

    /** Il nome base se libero, altrimenti la prima variante numerata libera. */
    public static String unique(String base, Predicate<String> taken) {
        if (!taken.test(base)) return base;
        for (int i = 2; i < 100; i++) {
            String s = base + i;
            if (!taken.test(s)) return s;
        }
        return base;
    }

    /**
     * Chiave da usare in una mappa di rilevamento collisioni "scritto in questo
     * commit": identifica univocamente uno slot ALL'INTERNO del suo namespace
     * (il device/panel che lo contiene), non solo per nome nudo.
     *
     * Due panel diversi possono legittimamente contenere entrambi "L01S002"
     * sotto folder separate — non è una collisione. Solo due CandidateKey
     * diversi che reclamano lo stesso deviceSlot SOTTO LO STESSO panel lo sono.
     * Il separatore NUL evita ambiguità fra un nome panel che finisce con
     * cifre e uno slot che inizia con cifre (concatenazione non ambigua).
     */
    public static String collisionKey(String panelSlot, String deviceSlot) {
        return panelSlot + '\0' + deviceSlot;
    }
}
