package com.sitecVendor.am8xControl.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Value object che rappresenta un dispositivo scoperto dal parsing
 * di un file XML di topologia esportato da una centrale AM-8200N.
 */
public final class Am8xDeviceDescriptor {

    private final String panelType;
    private final String panelLabel;
    private final int    loopNumber;
    private final int    positionOnLoop;
    private final String deviceType;
    private final String label;
    private final int    zoneAddress;
    private final String zoneLabel;

    private final List<Am8xSubModuleDescriptor> subModules = new ArrayList<>();

    public Am8xDeviceDescriptor(String panelType,
                                String panelLabel,
                                int    loopNumber,
                                int    positionOnLoop,
                                String deviceType,
                                String label,
                                int    zoneAddress,
                                String zoneLabel) {
        this.panelType      = panelType      == null ? "" : panelType;
        this.panelLabel     = panelLabel     == null ? "" : panelLabel;
        this.loopNumber     = loopNumber;
        this.positionOnLoop = positionOnLoop;
        this.deviceType     = deviceType     == null ? "" : deviceType;
        this.label          = label          == null ? "" : label;
        this.zoneAddress    = zoneAddress;
        this.zoneLabel      = zoneLabel      == null ? "" : zoneLabel;
    }

    public String getPanelType()      { return panelType; }
    public String getPanelLabel()     { return panelLabel; }
    public int    getLoopNumber()     { return loopNumber; }
    public int    getPositionOnLoop() { return positionOnLoop; }
    public String getDeviceType()     { return deviceType; }
    public String getLabel()          { return label; }
    public int    getZoneAddress()    { return zoneAddress; }
    public String getZoneLabel()      { return zoneLabel; }

    public void addSubModule(Am8xSubModuleDescriptor sm) { subModules.add(sm); }
    public List<Am8xSubModuleDescriptor> getSubModules() { return Collections.unmodifiableList(subModules); }
    public boolean hasSubModules() { return !subModules.isEmpty(); }

    public boolean isModuleType() {
        return deviceType != null && deviceType.startsWith("M");
    }

    @Override
    public String toString() {
        return "L" + loopNumber + "P" + positionOnLoop + " " + label + " [" + deviceType + "]";
    }
}
