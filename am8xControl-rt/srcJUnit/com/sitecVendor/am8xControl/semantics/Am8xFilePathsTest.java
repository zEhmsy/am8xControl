package com.sitecVendor.am8xControl.semantics;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Copre la costruzione dei percorsi su filesystem per un ORD di file.
 *
 * Perche' conta: sulla station la risoluzione via ORD di "file:^..." fallisce
 * (UnresolvedException su tutti i candidati), quindi questo fallback e' l'unica
 * strada per cui l'import legge davvero l'XML. Un errore qui non da' un
 * messaggio chiaro: da' "file non trovato" su un file che esiste.
 */
class Am8xFilePathsTest {

    private static final File HOME = new File("/stations/SUP_TEST/shared");

    private static String[] paths(List<File> files) {
        String[] out = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            out[i] = files.get(i).getPath().replace(File.separatorChar, '/');
        }
        return out;
    }

    @Test
    void resolvesPlainStationOrdAgainstTheStationHome() {
        List<File> c = Am8xFilePaths.candidatesFor(HOME, "file:^TEST.xml");
        assertEquals("/stations/SUP_TEST/shared/TEST.xml", paths(c)[0],
                "'^' e' gia' la shared root: il primo candidato non deve aggiungere 'shared'");
    }

    @Test
    void redundantSharedSegmentStillResolvesToTheRealFile() {
        // E' la forma che le versioni precedenti scrivevano negli ORD persistiti.
        List<File> c = Am8xFilePaths.candidatesFor(HOME, "file:^shared/TEST.xml");
        assertTrue(java.util.Arrays.asList(paths(c)).contains("/stations/SUP_TEST/shared/TEST.xml"),
                "un ORD legacy con 'shared/' deve comunque trovare il file nella shared root, "
                        + "invece: " + java.util.Arrays.toString(paths(c)));
    }

    @Test
    void keepsTheNestedLegacyLocationAsACandidate() {
        // File gia' scritti da doUploadXml delle versioni precedenti: non vanno persi.
        List<File> c = Am8xFilePaths.candidatesFor(HOME, "file:^shared/TEST.xml");
        assertTrue(java.util.Arrays.asList(paths(c)).contains("/stations/SUP_TEST/shared/shared/TEST.xml"),
                "la posizione annidata legacy deve restare fra i candidati, invece: "
                        + java.util.Arrays.toString(paths(c)));
    }

    @Test
    void readsALegacyOrdLiterallyBeforeStrippingIt() {
        // Ordine voluto, e non e' quello che verrebbe istintivo.
        // Un ORD "file:^shared/X" esiste solo negli impianti configurati dalle
        // versioni precedenti, ed e' proprio li' — <sharedRoot>/shared/X — che il
        // vecchio doUploadXml scriveva il file. Quindi la lettura LETTERALE va
        // provata per prima: e' quella che trova i file degli impianti esistenti.
        // La forma senza "shared/" resta come fallback per chi ha configurato
        // l'ORD a mano seguendo la convenzione Niagara.
        List<File> c = Am8xFilePaths.candidatesFor(HOME, "file:^shared/TEST.xml");
        String[] p = paths(c);
        int literal = java.util.Arrays.asList(p).indexOf("/stations/SUP_TEST/shared/shared/TEST.xml");
        int stripped = java.util.Arrays.asList(p).indexOf("/stations/SUP_TEST/shared/TEST.xml");
        assertTrue(literal >= 0 && stripped >= 0, java.util.Arrays.toString(p));
        assertTrue(literal < stripped,
                "un ORD legacy va letto prima letteralmente, invece: "
                        + java.util.Arrays.toString(p));
    }

    @Test
    void aPlainOrdPrefersTheSharedRootItself() {
        // Il caso nuovo: dopo il fix, doUploadXml scrive in <sharedRoot> e genera
        // "file:^X", quindi il primo candidato dev'essere <sharedRoot>/X.
        String[] p = paths(Am8xFilePaths.candidatesFor(HOME, "file:^TEST.xml"));
        assertEquals("/stations/SUP_TEST/shared/TEST.xml", p[0], java.util.Arrays.toString(p));
    }

    @Test
    void producesNoDuplicates() {
        List<File> c = Am8xFilePaths.candidatesFor(HOME, "file:^TEST.xml");
        assertEquals(c.size(), new java.util.HashSet<>(c).size(), "candidati duplicati: " + c);
    }

    @Test
    void ignoresOrdsThatAreNotStationFileOrds() {
        assertTrue(Am8xFilePaths.candidatesFor(HOME, "station:|slot:/Services").isEmpty());
        assertTrue(Am8xFilePaths.candidatesFor(HOME, "").isEmpty());
    }

    @Test
    void neverReturnsNullOnNullInput() {
        assertTrue(Am8xFilePaths.candidatesFor(HOME, null).isEmpty());
        assertTrue(Am8xFilePaths.candidatesFor(null, "file:^TEST.xml").isEmpty());
    }

    @Test
    void rejectsTraversalOutsideTheStationHome() {
        assertThrows(IllegalArgumentException.class,
                () -> Am8xFilePaths.candidatesFor(HOME, "file:^../../config.bog"));
    }
}
