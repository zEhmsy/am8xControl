package com.sitecVendor.am8xControl.semantics;

import com.sitecVendor.am8xControl.model.CandidateKey;
import com.sitecVendor.am8xControl.modbus.BAm8xStatePoint;

import javax.baja.sys.BComponent;

import java.util.Optional;

/**
 * Costruisce Am8xIdentity da un componente dell'albero.
 *
 * ATTENZIONE: questo codice gira dentro addAllImpliedTags, cioè per OGNI entity
 * di OGNI query di tag della station. Due regole:
 *   1. bail-out con un instanceof prima di qualunque parsing;
 *   2. non lanciare mai — un'eccezione qui rompe le query dell'intera station.
 */
public final class Am8xIdentitySource {

    private Am8xIdentitySource() {}

    public static Optional<Am8xIdentity> fromComponent(BComponent c) {
        if (!(c instanceof BAm8xStatePoint)) return Optional.empty();   // percorso di miss
        try {
            BAm8xStatePoint p = (BAm8xStatePoint) c;
            String slotName = p.getName();
            String panel = panelOf(p);

            Optional<CandidateKey> key = CandidateKey.parse(panel, slotName);
            if (!key.isPresent()) return Optional.empty();
            CandidateKey k = key.get();

            return Optional.of(new Am8xIdentity(
                    panel, k.getLoop(), k.getPos(), k.getChannel(), k.getKind(),
                    p.getDeviceType(), p.getDeviceLabel(),
                    p.getZoneAddress(), p.getZoneLabel()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Il panel è il nome dello slot del BModbusClientDevice, cioè il nonno del
     * container "points": device(panelSlot) -> points -> L01(loopFolder) -> L01S002(point).
     *
     * NOTA: il walk del brief originale confondeva loopFolder col panel — quando
     * risaliva fino a trovare un parent chiamato "points" restituiva il nome del
     * NODO CORRENTE (il loop folder, es. "L01"), non il nome del device che sta
     * un livello sopra "points". Verificato leggendo ModbusTreeBuilder.ensureFolder
     * (chiamato con pointsContainer come parent per creare i loop folder L01/L02)
     * e BAm8xImportService (righe ~392-447): il point sta dentro loopFolder,
     * loopFolder sta dentro pointsContainer ("points"), e pointsContainer è
     * il figlio "points" del device il cui slot name È il panel. Quindi il panel
     * si ottiene risalendo di UN livello ulteriore rispetto al nodo il cui parent
     * si chiama "points".
     */
    private static String panelOf(BComponent c) {
        BComponent cur = c;
        for (int i = 0; i < 8 && cur != null; i++) {
            BComponent parent = cur.getParentComponent();
            if (parent == null) break;
            if ("points".equals(parent.getName())) {
                BComponent device = parent.getParentComponent();
                return device != null ? device.getName() : "";
            }
            cur = parent;
        }
        return "";
    }
}
