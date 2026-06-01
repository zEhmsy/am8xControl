package com.sitecVendor.am8xControl.discovery;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;
import javax.baja.sys.Slot;
import java.util.ArrayList;
import java.util.List;

/**
 * Contenitore persistente dei candidate di discovery.
 * Vive come figlio di BAm8xImportService con slot name "discovery".
 *
 * Struttura gerarchica:
 *   discovery (BAm8xDiscoveryReport)
 *     └── CENTRALE_1  (BComponent panel folder)
 *           ├── L01S001  (BAm8xDiscoveryCandidate)
 *           └── ...
 *     └── CENTRALE_2  (BComponent panel folder)
 *           ├── L01S001  (BAm8xDiscoveryCandidate)
 *           └── ...
 *
 * I folder panel sono BComponent semplici, i candidate sono figli di quei folder.
 */
@NiagaraType
@NoSlotomatic
public final class BAm8xDiscoveryReport extends BComponent {

    ////////////////////////////////////////////////////////////////
    // Properties
    ////////////////////////////////////////////////////////////////

    public static final Property lastRunTimestamp =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getLastRunTimestamp() { return getString(lastRunTimestamp); }
    public void setLastRunTimestamp(String v) { setString(lastRunTimestamp, v, null); }

    public static final Property totalCandidates =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getTotalCandidates() { return getInt(totalCandidates); }
    public void setTotalCandidates(int v) { setInt(totalCandidates, v, null); }

    public static final Property selectedCount =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getSelectedCount() { return getInt(selectedCount); }
    public void setSelectedCount(int v) { setInt(selectedCount, v, null); }

    ////////////////////////////////////////////////////////////////
    // Actions
    ////////////////////////////////////////////////////////////////

    public static final Action clearAll = newAction(Flags.OPERATOR, null);
    public void clearAll() { invoke(clearAll, null, null); }
    public void doClearAll() { removePanelFolders(); }

    ////////////////////////////////////////////////////////////////
    // Helpers usati da BAm8xImportService
    ////////////////////////////////////////////////////////////////

    /**
     * Trova o crea un BComponent panel-folder figlio diretto.
     * @param panelSlot slot name sanitizzato (es. "CEENTRALE_1")
     */
    public BAm8xPanelFolder ensurePanelFolder(String panelSlot) {
        try {
            BValue v = get(panelSlot);
            if (v instanceof BAm8xPanelFolder) return (BAm8xPanelFolder) v;
        } catch (Exception ignore) {}
        BAm8xPanelFolder folder = new BAm8xPanelFolder();
        try { add(panelSlot, folder, Flags.SUMMARY); } catch (Exception e) { /* già esiste */ }
        // Dopo add() il riferimento locale non è montato — recupera l'istanza montata
        try {
            BValue v = get(panelSlot);
            if (v instanceof BAm8xPanelFolder) return (BAm8xPanelFolder) v;
        } catch (Exception ignore) {}
        return folder;
    }

    /** Trova un panel folder per nome slot, o null se non esiste. */
    public BAm8xPanelFolder getPanelFolder(String panelSlot) {
        try {
            BValue v = get(panelSlot);
            return (v instanceof BAm8xPanelFolder) ? (BAm8xPanelFolder) v : null;
        } catch (Exception ignore) { return null; }
    }

    /** Lista di tutti i panel folder diretti. */
    public List<BAm8xPanelFolder> getPanelFolders() {
        List<BAm8xPanelFolder> result = new ArrayList<>();
        for (Slot s : getSlotsArray()) {
            if (!(s instanceof Property)) continue;
            try {
                BValue v = get((Property) s);
                if (v instanceof BAm8xPanelFolder) result.add((BAm8xPanelFolder) v);
            } catch (Exception ignore) {}
        }
        return result;
    }

    /** Rimuove solo i candidate da ogni panel folder, preservando la config del panel. */
    public void clearAllCandidates() {
        for (BAm8xPanelFolder panel : getPanelFolders()) {
            panel.clearCandidates();
        }
        setTotalCandidates(0);
        setSelectedCount(0);
    }

    /** Rimuove tutti i panel folder (e i candidate dentro). */
    public void removePanelFolders() {
        List<Property> toRemove = new ArrayList<>();
        for (Property p : getPropertiesArray()) {
            try {
                BValue v = get(p);
                if (v instanceof BAm8xPanelFolder) toRemove.add(p);
            } catch (Exception ignore) {}
        }
        for (Property p : toRemove) {
            try { remove(p); } catch (Exception ignore) {}
        }
        setTotalCandidates(0);
        setSelectedCount(0);
    }

    /**
     * Restituisce tutti i BAm8xDiscoveryCandidate da tutti i panel folder.
     */
    public List<BAm8xDiscoveryCandidate> getCandidates() {
        List<BAm8xDiscoveryCandidate> result = new ArrayList<>();
        // Itera panel folder
        for (Slot s : getSlotsArray()) {
            if (!(s instanceof Property)) continue;
            try {
                BValue panelVal = get((Property) s);
                if (!(panelVal instanceof BAm8xPanelFolder)) continue;
                BAm8xPanelFolder panelFolder = (BAm8xPanelFolder) panelVal;
                // Itera candidate dentro il panel folder
                for (Slot cs : panelFolder.getSlotsArray()) {
                    if (!(cs instanceof Property)) continue;
                    try {
                        BValue cv = panelFolder.get((Property) cs);
                        if (cv instanceof BAm8xDiscoveryCandidate) {
                            result.add((BAm8xDiscoveryCandidate) cv);
                        }
                    } catch (Exception ignore) {}
                }
            } catch (Exception ignore) {}
        }
        return result;
    }

    /** Ricalcola selectedCount dai candidate correnti. */
    public void refreshSelectedCount() {
        int count = 0;
        for (BAm8xDiscoveryCandidate c : getCandidates()) {
            if (c.getSelected()) count++;
        }
        setSelectedCount(count);
    }

    ////////////////////////////////////////////////////////////////
    // Boilerplate
    ////////////////////////////////////////////////////////////////

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDiscoveryReport.class);

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON =
            BIcon.make("module://am8xControl/img/am8xFolder.png");
}
