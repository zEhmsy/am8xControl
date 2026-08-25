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
}
