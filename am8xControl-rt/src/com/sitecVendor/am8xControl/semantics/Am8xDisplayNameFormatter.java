package com.sitecVendor.am8xControl.semantics;

/**
 * Risolve displayNameFormat in una stringa letterale.
 *
 * Il template si risolve QUI e non in un BFormat vivo: niente valutazione a
 * runtime su migliaia di nodi, e un '%' dentro una label resta testo invece di
 * diventare un'espressione.
 */
public final class Am8xDisplayNameFormatter {

    private Am8xDisplayNameFormatter() {}

    public static String render(String format, String canonicalName, Am8xIdentity id) {
        if (format == null || format.isEmpty()) return canonicalName;

        String out = format
                .replace("%name%",        nz(canonicalName))
                .replace("%deviceLabel%", nz(id.getDeviceLabel()))
                .replace("%zone%",        nz(id.getZoneAddress()))
                .replace("%loop%",        String.valueOf(id.getLoop()));

        out = out.replaceAll("\\s{2,}", " ");                 // spazi doppi
        out = out.replaceAll("(^[\\s\\-—/]+)|([\\s\\-—/]+$)", "");  // separatori orfani
        return out.isEmpty() ? canonicalName : out;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
