package com.sitecVendor.am8xControl.semantics;

import com.sitecVendor.am8xControl.model.CandidateKey;

/** Identità semantica di un nodo AM-8x. Immutabile, senza dipendenze da baja. */
public final class Am8xIdentity {

    private final String panel;
    private final int loop;
    private final int position;
    private final int channel;          // -1 se non sub-modulo
    private final CandidateKey.Kind kind;
    private final String deviceType;
    private final String deviceLabel;
    private final String zoneAddress;
    private final String zoneLabel;

    public Am8xIdentity(String panel, int loop, int position, int channel,
                        CandidateKey.Kind kind, String deviceType, String deviceLabel,
                        String zoneAddress, String zoneLabel) {
        this.panel       = nz(panel);
        this.loop        = loop;
        this.position    = position;
        this.channel     = channel;
        this.kind        = kind;
        this.deviceType  = nz(deviceType);
        this.deviceLabel = nz(deviceLabel);
        this.zoneAddress = nz(zoneAddress);
        this.zoneLabel   = nz(zoneLabel);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    public String getPanel()       { return panel; }
    public int    getLoop()        { return loop; }
    public int    getPosition()    { return position; }
    public int    getChannel()     { return channel; }
    public CandidateKey.Kind getKind() { return kind; }
    public String getDeviceType()  { return deviceType; }
    public String getDeviceLabel() { return deviceLabel; }
    public String getZoneAddress() { return zoneAddress; }
    public String getZoneLabel()   { return zoneLabel; }

    /** Valore del tag am8x:kind. */
    public String kindTag() {
        switch (kind) {
            case SUB_MODULE: return "subModule";
            case MODULE:     return "module";
            default:         return "sensor";
        }
    }
}
