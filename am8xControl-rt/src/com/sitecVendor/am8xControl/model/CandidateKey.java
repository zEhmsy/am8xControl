package com.sitecVendor.am8xControl.model;

import com.sitecVendor.am8xControl.parser.Am8xDeviceDescriptor;
import com.sitecVendor.am8xControl.parser.Am8xSubModuleDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identificatore stabile di un singolo device (sensore o sub-modulo).
 * Basato sull'indirizzo fisico, non sulla label (che può cambiare).
 *
 * Convenzione slot name (plan.md §2bis):
 *   Loop folder     : L{loop:02d}                    → "L01"
 *   Sensore diretto : L{loop:02d}S{pos:03d}           → "L01S002"
 *   Modulo leaf     : L{loop:02d}M{pos:03d}           → "L01M003"
 *   Sub-canale M720 : L{loop:02d}M{mPos:03d}_{ch}    → "L01M003_1"
 *
 * Nota: si usa '_' invece di '.' come separatore canale perché il
 * framework Niagara non garantisce slot name con '.' in tutte le versioni.
 */
public final class CandidateKey {

    public enum Kind { SENSOR, MODULE, SUB_MODULE }

    private final String panel;
    private final int    loop;
    private final int    pos;
    private final int    parentModulePos; // -1 se KIND != SUB_MODULE
    private final int    channel;         // -1 se KIND != SUB_MODULE
    private final Kind   kind;

    private CandidateKey(String panel, int loop, int pos,
                         int parentModulePos, int channel, Kind kind) {
        this.panel           = panel == null ? "" : panel;
        this.loop            = loop;
        this.pos             = pos;
        this.parentModulePos = parentModulePos;
        this.channel         = channel;
        this.kind            = kind;
    }

    // ----------------------------------------------------------------
    // Factory
    // ----------------------------------------------------------------

    /** Sensore o modulo diretto sul loop; il Kind viene derivato dal descriptor. */
    public static CandidateKey forDevice(Am8xDeviceDescriptor d) {
        Kind k = d.isModuleType() ? Kind.MODULE : Kind.SENSOR;
        return new CandidateKey(d.getPanelLabel(), d.getLoopNumber(),
                d.getPositionOnLoop(), -1, -1, k);
    }

    /** Sub-canale di un modulo M720. */
    public static CandidateKey forSubModule(Am8xDeviceDescriptor parent,
                                             Am8xSubModuleDescriptor sub) {
        return new CandidateKey(parent.getPanelLabel(), parent.getLoopNumber(),
                parent.getPositionOnLoop(), parent.getPositionOnLoop(),
                sub.getNumber(), Kind.SUB_MODULE);
    }

    /**
     * Espande tutti i CandidateKey da un descriptor:
     * uno per il device stesso se non ha sub-moduli, oppure solo i suoi sub-moduli.
     */
    public static List<CandidateKey> expandFrom(Am8xDeviceDescriptor d) {
        List<CandidateKey> keys = new ArrayList<>();
        if (!d.hasSubModules()) keys.add(forDevice(d));
        for (Am8xSubModuleDescriptor sub : d.getSubModules()) {
            keys.add(forSubModule(d, sub));
        }
        return keys;
    }

    // ----------------------------------------------------------------
    // Naming
    // ----------------------------------------------------------------

    /**
     * Slot name del folder panel: es. "CEENTRALE_1", "CENTRALE_2".
     * Sostituisce ogni carattere non alfanumerico con '_', collassa sequenze.
     */
    public String toPanelSlotName() {
        if (panel == null || panel.isEmpty()) return "Panel";
        String s = panel.replaceAll("[^a-zA-Z0-9]", "_")
                        .replaceAll("_+", "_")
                        .replaceAll("^_|_$", "");
        return s.isEmpty() ? "Panel" : s;
    }

    /** Slot name del folder loop padre: es. "L01". */
    public String toLoopSlotName() {
        return String.format("L%02d", loop);
    }

    /**
     * Slot name del device/point folder: es. "L01S002", "L01M003", "L01M003_1".
     * Single source of truth usata da discover(), addSelected(), clearImported().
     */
    public String toSlotName() {
        switch (kind) {
            case SUB_MODULE:
                return String.format("L%02dM%03d_%d", loop, parentModulePos, channel);
            case MODULE:
                return String.format("L%02dM%03d", loop, pos);
            case SENSOR:
            default:
                return String.format("L%02dS%03d", loop, pos);
        }
    }

    /** Inverso di {@link #toSlotName()}. Il panel non è codificato nello slot: si passa. */
    private static final Pattern SLOT = Pattern.compile("L(\\d{2})([SM])(\\d{3})(?:_(\\d+))?");

    public static Optional<CandidateKey> parse(String panel, String slotName) {
        if (slotName == null) return Optional.empty();
        Matcher m = SLOT.matcher(slotName);
        if (!m.matches()) return Optional.empty();

        int loop = Integer.parseInt(m.group(1));
        int pos  = Integer.parseInt(m.group(3));
        boolean module = "M".equals(m.group(2));
        String chGroup = m.group(4);

        if (chGroup != null) {
            if (!module) return Optional.empty();   // un sensore non ha canali
            int ch = Integer.parseInt(chGroup);
            return Optional.of(new CandidateKey(panel, loop, pos, pos, ch, Kind.SUB_MODULE));
        }
        return Optional.of(new CandidateKey(panel, loop, pos, -1, -1,
                module ? Kind.MODULE : Kind.SENSOR));
    }

    // ----------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------

    public String getPanel()           { return panel; }
    public int    getLoop()            { return loop; }
    public int    getPos()             { return pos; }
    public int    getParentModulePos() { return parentModulePos; }
    public int    getChannel()         { return channel; }
    public Kind   getKind()            { return kind; }
    public boolean isSubModule()       { return kind == Kind.SUB_MODULE; }

    // ----------------------------------------------------------------
    // equals / hashCode / toString
    // ----------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandidateKey)) return false;
        CandidateKey k = (CandidateKey) o;
        return loop == k.loop && pos == k.pos
                && parentModulePos == k.parentModulePos
                && channel == k.channel
                && panel.equals(k.panel);
    }

    @Override
    public int hashCode() {
        int h = panel.hashCode();
        h = 31 * h + loop;
        h = 31 * h + pos;
        h = 31 * h + parentModulePos;
        h = 31 * h + channel;
        return h;
    }

    @Override
    public String toString() {
        return panel + "/" + toLoopSlotName() + "/" + toSlotName();
    }
}
