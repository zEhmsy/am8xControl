package com.sitecVendor.am8xControl.discovery;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Folder che raggruppa i BAm8xDiscoveryCandidate di una singola centrale.
 * Vive come figlio di BAm8xDiscoveryReport con slot name = CandidateKey.toPanelSlotName().
 *
 * Espone le impostazioni Modbus della centrale (IP, porta, slave address):
 * vengono lette da BAm8xImportService.doAddSelected() per creare un
 * BModbusTcpDevice dedicato per ciascuna centrale.
 */
@NiagaraType
@NoSlotomatic
public final class BAm8xPanelFolder extends BComponent {

    ////////////////////////////////////////////////////////////////
    // Configurazione Modbus per-centrale (editabile)
    ////////////////////////////////////////////////////////////////

    /** IP address del Modbus TCP server per questa centrale. */
    public static final Property ipAddress =
            newProperty(Flags.SUMMARY, "", null);
    public String getIpAddress() { return getString(ipAddress); }
    public void setIpAddress(String v) { setString(ipAddress, v, null); }

    /** Porta TCP (default 502). */
    public static final Property port =
            newProperty(Flags.SUMMARY, 502, null);
    public int getPort() { return getInt(port); }
    public void setPort(int v) { setInt(port, v, null); }

    /**
     * Modbus slave/device address (unit ID). Default 101 per la prima centrale,
     * 102 per la seconda, ecc. — assegnato automaticamente da doDiscover()
     * solo quando il panel folder viene creato la prima volta.
     */
    public static final Property deviceAddress =
            newProperty(Flags.SUMMARY, 0, null);
    public int getDeviceAddress() { return getInt(deviceAddress); }
    public void setDeviceAddress(int v) { setInt(deviceAddress, v, null); }

    ////////////////////////////////////////////////////////////////
    // Helpers
    ////////////////////////////////////////////////////////////////

    /** Lista dei candidate diretti di questo panel folder. */
    public List<BAm8xDiscoveryCandidate> getCandidates() {
        List<BAm8xDiscoveryCandidate> result = new ArrayList<>();
        for (Slot s : getSlotsArray()) {
            if (!(s instanceof Property)) continue;
            try {
                BValue v = get((Property) s);
                if (v instanceof BAm8xDiscoveryCandidate) {
                    result.add((BAm8xDiscoveryCandidate) v);
                }
            } catch (Exception ignore) {}
        }
        return result;
    }

    /** Rimuove tutti i candidate ma preserva la configurazione del panel. */
    public void clearCandidates() {
        List<Property> toRemove = new ArrayList<>();
        for (Property p : getPropertiesArray()) {
            try {
                BValue v = get(p);
                if (v instanceof BAm8xDiscoveryCandidate) toRemove.add(p);
            } catch (Exception ignore) {}
        }
        for (Property p : toRemove) {
            try { remove(p); } catch (Exception ignore) {}
        }
    }

    ////////////////////////////////////////////////////////////////
    // Boilerplate
    ////////////////////////////////////////////////////////////////

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xPanelFolder.class);

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON =
            BIcon.make("module://am8xControl/img/am8xCentral.png");
}
