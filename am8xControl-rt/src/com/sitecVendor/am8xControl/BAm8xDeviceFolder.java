package com.sitecVendor.am8xControl;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;

/**
 * Folder che raggruppa i {@link BAm8xDevice} di una singola centrale AM-8200N.
 * Ogni centrale presente nel file XML ottiene il proprio folder.
 */
@NiagaraType
@NoSlotomatic
public class BAm8xDeviceFolder extends BComponent {

    public static final Property panelLabel =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getPanelLabel() { return getString(panelLabel); }
    public void setPanelLabel(String v) { setString(panelLabel, v, null); }

    public static final Property panelType =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getPanelType() { return getString(panelType); }
    public void setPanelType(String v) { setString(panelType, v, null); }

    public void setPanelInfo(String label, String type) {
        setPanelLabel(label == null ? "" : label);
        setPanelType(type == null ? "" : type);
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDeviceFolder.class);

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON = BIcon.make("module://am8xControl/img/am8xFolder.png");
}
