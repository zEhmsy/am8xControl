package com.sitecVendor.am8xControl.semantics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Risoluzione su filesystem degli ORD di file usati dall'import.
 *
 * Vive qui, fuori da BAm8xImportService, per un motivo preciso: quella classe ha
 * uno static init con Sys.loadType() e non si puo' istanziare senza il runtime
 * Niagara, quindi la logica di path li' dentro sarebbe non testabile. Qui e' pura
 * — prende la station home come parametro — e si copre con normali unit test.
 *
 * Perche' merita test: sulla station la risoluzione via ORD di "file:^..."
 * fallisce con UnresolvedException su tutti i candidati (verificato in campo su
 * Niagara 4.15.1.16), quindi questo fallback e' l'unica strada per cui l'import
 * legge davvero il file. Un errore qui non produce un messaggio chiaro: produce
 * "file non trovato" su un file che esiste.
 */
public final class Am8xFilePaths {

    /** ORD di un file relativo alla file space root della station. */
    public static final String STATION_ORD_PREFIX = "file:^";
    public static final String SHARED_DIR_NAME = "shared";

    private static final String LOCAL_ORD_PREFIX = "local:|";
    private static final String STATION_HOST_ORD_PREFIX = "station:|";

    private Am8xFilePaths() {}

    /**
     * Percorsi da provare per un ORD, in ordine di preferenza.
     *
     * ATTENZIONE alla semantica di '^'. Verificato in campo: Sys.getStationHome()
     * restituisce GIA' la file space root della station, cioe' la cartella
     * 'shared' — non la directory della station. Quindi "file:^X" e'
     * &lt;stationHome&gt;/X. Il codice precedente ci appendeva un altro "shared" e
     * cercava in &lt;station&gt;/shared/shared/X, dove non trovava mai nulla.
     *
     * Non si sceglie una semantica sola, si provano entrambe: un impianto gia'
     * installato puo' avere l'ORD "file:^shared/X" persistito e il file gia'
     * scritto nella posizione annidata dalle versioni precedenti, e deve
     * continuare a funzionare senza interventi manuali.
     */
    public static List<File> candidatesFor(File stationHome, String ordStr) {
        List<File> out = new ArrayList<>();
        if (stationHome == null || ordStr == null) return out;

        String clean = stripHostOrdPrefix(ordStr).replace('\\', '/');
        if (!clean.startsWith(STATION_ORD_PREFIX)) return out;

        String rel = safeRelativePath(clean.substring(STATION_ORD_PREFIX.length()));
        if (rel.isEmpty()) return out;

        // 1. semantica corretta: '^' e' gia' la shared root
        add(out, stationHome, rel);

        // 2. ORD legacy con il segmento "shared/" ridondante: prova anche senza
        if (rel.startsWith(SHARED_DIR_NAME + "/")) {
            add(out, stationHome, rel.substring(SHARED_DIR_NAME.length() + 1));
        }

        // 3. file gia' finiti in <stationHome>/shared/ dalle versioni precedenti
        add(out, new File(stationHome, SHARED_DIR_NAME), rel);

        return out;
    }

    /** Normalizza e rifiuta i path che uscirebbero dalla directory di base. */
    public static String safeRelativePath(String relativePath) {
        String safe = relativePath == null ? "" : relativePath.trim().replace('\\', '/');
        while (safe.startsWith("/")) safe = safe.substring(1);
        if (safe.indexOf(':') >= 0 || safe.equals(".") || safe.equals("..")
                || safe.startsWith("../") || safe.contains("/../") || safe.endsWith("/..")) {
            throw new IllegalArgumentException("percorso shared non valido: " + relativePath);
        }
        return safe;
    }

    public static String stripHostOrdPrefix(String ordStr) {
        if (ordStr == null) return "";
        if (ordStr.startsWith(LOCAL_ORD_PREFIX)) return ordStr.substring(LOCAL_ORD_PREFIX.length());
        if (ordStr.startsWith(STATION_HOST_ORD_PREFIX)) return ordStr.substring(STATION_HOST_ORD_PREFIX.length());
        return ordStr;
    }

    private static void add(List<File> out, File base, String rel) {
        if (rel == null || rel.isEmpty()) return;
        File f = new File(base, rel.replace('/', File.separatorChar));
        if (!out.contains(f)) out.add(f);
    }
}
