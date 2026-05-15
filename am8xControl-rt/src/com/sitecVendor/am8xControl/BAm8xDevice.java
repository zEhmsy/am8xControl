package com.sitecVendor.am8xControl;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;

/**
 * Componente Niagara che rappresenta un singolo dispositivo (sensore / modulo)
 * scoperto sul loop Essernet di una centrale AM-8200N.
 *
 * <p>I valori sono valorizzati dal BAm8xNetwork al termine del Discovery XML;
 * sono visibili (READONLY) nel Property Sheet del Workbench.</p>
 */
@NiagaraType
@NoSlotomatic
public class BAm8xDevice extends BComponent {

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
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getDeviceType() { return getString(deviceType); }
    public void setDeviceType(String v) { setString(deviceType, v, null); }

    public static final Property label =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getLabel() { return getString(label); }
    public void setLabel(String v) { setString(label, v, null); }

    public static final Property zoneAddress =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getZoneAddress() { return getInt(zoneAddress); }
    public void setZoneAddress(int v) { setInt(zoneAddress, v, null); }

    public static final Property zoneLabel =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getZoneLabel() { return getString(zoneLabel); }
    public void setZoneLabel(String v) { setString(zoneLabel, v, null); }

    /**
     * Popola il device dai dati di un descriptor.
     */
    public void applyDescriptor(Am8xDeviceDescriptor d) {
        if (d == null) return;
        setPanelLabel(d.getPanelLabel());
        setLoopNumber(d.getLoopNumber());
        setPositionOnLoop(d.getPositionOnLoop());
        setDeviceType(d.getDeviceType());
        setLabel(d.getLabel());
        setZoneAddress(d.getZoneAddress());
        setZoneLabel(d.getZoneLabel());
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDevice.class);

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON = BIcon.make("module://am8xControl/img/am8xDevice.png");
}
