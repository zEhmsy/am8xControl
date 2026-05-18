package com.sitecVendor.am8xControl;

/**
 * Sub-modulo di un device AM-8200N (es. canali interni di un M720).
 * Riflette il nodo XML {@code <SubModule><Module>}.
 */
public final class Am8xSubModuleDescriptor {

    private final String type;
    private final String label;
    private final int    number;
    private final int    zoneAddress;
    private final String zoneLabel;

    public Am8xSubModuleDescriptor(String type, String label,
                                   int number, int zoneAddress, String zoneLabel) {
        this.type        = type      == null ? "" : type;
        this.label       = label     == null ? "" : label;
        this.number      = number;
        this.zoneAddress = zoneAddress;
        this.zoneLabel   = zoneLabel == null ? "" : zoneLabel;
    }

    public String getType()       { return type; }
    public String getLabel()      { return label; }
    public int    getNumber()     { return number; }
    public int    getZoneAddress(){ return zoneAddress; }
    public String getZoneLabel()  { return zoneLabel; }
}
