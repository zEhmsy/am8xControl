package com.sitecVendor.am8xControl.discovery;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;

/**
 * Rappresenta un singolo device (sensore o sub-modulo) scoperto dal parsing XML.
 * Persiste sotto BAm8xDiscoveryReport/candidatesFolder come componente Niagara.
 *
 * L'utente può cambiare solo {@code selected}; tutto il resto è READONLY.
 * Lo slot name di questo componente == CandidateKey.toSlotName() (es. "L01S002").
 */
@NiagaraType
@NoSlotomatic
public final class BAm8xDiscoveryCandidate extends BComponent {

    ////////////////////////////////////////////////////////////////
    // Metadati (readonly)
    ////////////////////////////////////////////////////////////////

    public static final Property panelLabel =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getPanelLabel() { return getString(panelLabel); }
    public void setPanelLabel(String v) { setString(panelLabel, v, null); }

    public static final Property loopNumber =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getLoopNumber() { return getInt(loopNumber); }
    public void setLoopNumber(int v) { setInt(loopNumber, v, null); }

    public static final Property positionOnLoop =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getPositionOnLoop() { return getInt(positionOnLoop); }
    public void setPositionOnLoop(int v) { setInt(positionOnLoop, v, null); }

    public static final Property deviceType =
            newProperty(Flags.SUMMARY, "", null);
    public String getDeviceType() { return getString(deviceType); }
    public void setDeviceType(String v) { setString(deviceType, v, null); }

    public static final Property deviceLabel =
            newProperty(Flags.SUMMARY, "", null);
    public String getDeviceLabel() { return getString(deviceLabel); }
    public void setDeviceLabel(String v) { setString(deviceLabel, v, null); }

    public static final Property zoneAddress =
            newProperty(Flags.SUMMARY, 0, null);
    public int getZoneAddress() { return getInt(zoneAddress); }
    public void setZoneAddress(int v) { setInt(zoneAddress, v, null); }

    public static final Property zoneLabel =
            newProperty(Flags.SUMMARY, "", null);
    public String getZoneLabel() { return getString(zoneLabel); }
    public void setZoneLabel(String v) { setString(zoneLabel, v, null); }

    /** Indirizzo Modbus Qualificatore (Stato: 1-6) — parte di due point _State e _Analog. */
    public static final Property stateAddress =
            newProperty(Flags.SUMMARY, 0, null);
    public int getStateAddress() { return getInt(stateAddress); }
    public void setStateAddress(int v) { setInt(stateAddress, v, null); }

    /** Indirizzo Modbus Analogico (Misura: valore fisico). */
    public static final Property analogAddress =
            newProperty(Flags.SUMMARY, 0, null);
    public int getAnalogAddress() { return getInt(analogAddress); }
    public void setAnalogAddress(int v) { setInt(analogAddress, v, null); }


    /**
     * Slot name usato sia nella folder discovery che nel Modbus device tree.
     * Esempio: "L01S002", "L01M003", "L01M003_1".
     * Impostato da BAm8xImportService.addCandidate() e usato da doAddSelected().
     */
    public static final Property candidateSlotName =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getCandidateSlotName() { return getString(candidateSlotName); }
    public void setCandidateSlotName(String v) { setString(candidateSlotName, v, null); }

    /**
     * Slot name della centrale a cui appartiene questo candidate.
     * Esempio: "CEENTRALE_1", "CENTRALE_2". Usato per creare un
     * BModbusTcpDevice distinto per ciascuna centrale.
     */
    public static final Property panelSlotName =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getPanelSlotName() { return getString(panelSlotName); }
    public void setPanelSlotName(String v) { setString(panelSlotName, v, null); }

    /** True se i point Modbus per questo device esistono già sotto ModbusTcpDevice. */
    public static final Property alreadyImported =
            newProperty(Flags.SUMMARY | Flags.READONLY, false, null);
    public boolean getAlreadyImported() { return getBoolean(alreadyImported); }
    public void setAlreadyImported(boolean v) { setBoolean(alreadyImported, v, null); }

    ////////////////////////////////////////////////////////////////
    // Selezione (editabile dall'utente)
    ////////////////////////////////////////////////////////////////

    /** Se true questo candidate verrà incluso in addSelected(). Default true. */
    public static final Property selected =
            newProperty(Flags.SUMMARY, true, null);
    public boolean getSelected() { return getBoolean(selected); }
    public void setSelected(boolean v) { setBoolean(selected, v, null); }

    ////////////////////////////////////////////////////////////////
    // Action
    ////////////////////////////////////////////////////////////////

    public static final Action toggleSelected = newAction(Flags.OPERATOR, null);
    public void toggleSelected() { invoke(toggleSelected, null, null); }
    public void doToggleSelected() { setSelected(!getSelected()); }

    ////////////////////////////////////////////////////////////////
    // Boilerplate
    ////////////////////////////////////////////////////////////////

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDiscoveryCandidate.class);

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON =
            BIcon.make("module://am8xControl/img/am8xDevice.png");
}
