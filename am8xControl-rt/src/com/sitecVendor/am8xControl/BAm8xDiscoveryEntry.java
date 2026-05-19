package com.sitecVendor.am8xControl;

import com.tridium.ndriver.discover.BINDiscoveryIcon;
import com.tridium.ndriver.discover.BNDiscoveryLeaf;
import com.tridium.ndriver.util.SfUtil;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.*;

/**
 * Entry della Discovery View: rappresenta un singolo dispositivo scoperto
 * dal file XML della centrale AM-8200N.
 *
 * Deve essere @NiagaraType per serializzarsi correttamente attraverso il proxy
 * verso il Workbench. Le proprieta' con SfUtil.incl() compaiono come colonne
 * nel pannello Discovery del manager.
 */
@NiagaraType
@NoSlotomatic
public class BAm8xDiscoveryEntry extends BNDiscoveryLeaf implements BINDiscoveryIcon {

    // ASCII Unit Separator / Record Separator — safe inside BString property values
    public static final String FIELD_SEP  = "";
    public static final String RECORD_SEP = "";

    ////////////////////////////////////////////////////////////////
    // Properties visible as discovery pane columns
    ////////////////////////////////////////////////////////////////

    public static final Property panelLabel =
        newProperty(Flags.SUMMARY | Flags.READONLY, "", SfUtil.incl());
    public String getPanelLabel() { return getString(panelLabel); }
    public void setPanelLabel(String v) { setString(panelLabel, v, null); }

    public static final Property loopNumber =
        newProperty(Flags.SUMMARY | Flags.READONLY, 0, SfUtil.incl());
    public int getLoopNumber() { return getInt(loopNumber); }
    public void setLoopNumber(int v) { setInt(loopNumber, v, null); }

    public static final Property positionOnLoop =
        newProperty(Flags.SUMMARY | Flags.READONLY, 0, SfUtil.incl());
    public int getPositionOnLoop() { return getInt(positionOnLoop); }
    public void setPositionOnLoop(int v) { setInt(positionOnLoop, v, null); }

    public static final Property deviceType =
        newProperty(Flags.SUMMARY | Flags.READONLY, "", SfUtil.incl());
    public String getDeviceType() { return getString(deviceType); }
    public void setDeviceType(String v) { setString(deviceType, v, null); }

    public static final Property deviceLabel =
        newProperty(Flags.SUMMARY | Flags.READONLY, "", SfUtil.incl());
    public String getDeviceLabel() { return getString(deviceLabel); }
    public void setDeviceLabel(String v) { setString(deviceLabel, v, null); }

    public static final Property zoneLabel =
        newProperty(Flags.SUMMARY | Flags.READONLY, "", SfUtil.incl());
    public String getZoneLabel() { return getString(zoneLabel); }
    public void setZoneLabel(String v) { setString(zoneLabel, v, null); }

    // Hidden: needed by updateTarget but not shown as columns
    public static final Property panelType =
        newProperty(Flags.HIDDEN | Flags.READONLY, "", null);
    public String getPanelType() { return getString(panelType); }
    public void setPanelType(String v) { setString(panelType, v, null); }

    public static final Property zoneAddress =
        newProperty(Flags.HIDDEN | Flags.READONLY, 0, null);
    public int getZoneAddress() { return getInt(zoneAddress); }
    public void setZoneAddress(int v) { setInt(zoneAddress, v, null); }

    // Parent M720 positionOnLoop; -1 = this entry is a direct loop sensor.
    public static final Property parentModulePos =
        newProperty(Flags.HIDDEN | Flags.READONLY, -1, null);
    public int getParentModulePos() { return getInt(parentModulePos); }
    public void setParentModulePos(int v) { setInt(parentModulePos, v, null); }

    // Hidden: encodes sub-modules so they survive the RT->WB proxy.
    // Format: RECORD_SEP separates modules; FIELD_SEP separates fields within each:
    //   type  label  number  zoneAddress  zoneLabel
    public static final Property childrenData =
        newProperty(Flags.HIDDEN | Flags.READONLY, "", null);
    public String getChildrenData() { return getString(childrenData); }
    public void setChildrenData(String v) { setString(childrenData, v, null); }

    ////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////

    /** Required by Niagara type system for deserialization. */
    public BAm8xDiscoveryEntry() {}

    /** Used by getDiscoveryObjects() on the RT side. */
    public BAm8xDiscoveryEntry(Am8xDeviceDescriptor d) {
        setPanelType(d.getPanelType());
        setPanelLabel(d.getPanelLabel());
        setLoopNumber(d.getLoopNumber());
        setPositionOnLoop(d.getPositionOnLoop());
        setDeviceType(d.getDeviceType());
        setDeviceLabel(d.getLabel());
        setZoneAddress(d.getZoneAddress());
        setZoneLabel(d.getZoneLabel());
        if (d.hasSubModules()) {
            StringBuilder sb = new StringBuilder();
            for (Am8xSubModuleDescriptor sm : d.getSubModules()) {
                if (sb.length() > 0) sb.append(RECORD_SEP);
                sb.append(sm.getType())        .append(FIELD_SEP)
                  .append(sm.getLabel())       .append(FIELD_SEP)
                  .append(sm.getNumber())      .append(FIELD_SEP)
                  .append(sm.getZoneAddress()) .append(FIELD_SEP)
                  .append(sm.getZoneLabel())   .append(FIELD_SEP)
                  .append(d.getPositionOnLoop()); // parent M720 pos
            }
            setChildrenData(sb.toString());
        }
    }

    ////////////////////////////////////////////////////////////////
    // BINDiscoveryLeaf
    ////////////////////////////////////////////////////////////////

    @Override
    public String getDiscoveryName() {
        String lbl = getDeviceLabel();
        String name = lbl.isEmpty()
            ? "L" + getLoopNumber() + "_P" + getPositionOnLoop()
            : lbl;
        // Niagara slot names: only letters, digits, underscore
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    @Override
    public void updateTarget(BComponent target) {
        if (!(target instanceof BAm8xDevice)) return;
        BAm8xDevice dev = (BAm8xDevice) target;
        dev.setParentModulePos(getParentModulePos());
        Am8xDeviceDescriptor d = new Am8xDeviceDescriptor(
            getPanelType(), getPanelLabel(),
            getLoopNumber(), getPositionOnLoop(),
            getDeviceType(), getDeviceLabel(),
            getZoneAddress(), getZoneLabel()
        );
        dev.applyDescriptor(d);
    }

    @Override
    public boolean isExisting(BComponent component) {
        if (!(component instanceof BAm8xDevice)) return false;
        BAm8xDevice dev = (BAm8xDevice) component;
        if (dev.getLoopNumber()     != getLoopNumber())     return false;
        if (dev.getPositionOnLoop() != getPositionOnLoop()) return false;
        // If device has empty panelLabel (added with legacy code before Phase 3 fix),
        // match on loop+pos alone. Otherwise require exact panel match so same-position
        // devices from different panels are kept distinct.
        String devPanel = dev.getPanelLabel();
        return devPanel.isEmpty() || devPanel.equals(getPanelLabel());
    }

    @Override
    public TypeInfo[] getValidDatabaseTypes() {
        return new TypeInfo[]{ BAm8xDevice.TYPE.getTypeInfo() };
    }

    @Override
    public BIcon getDiscoveryIcon() {
        return BIcon.make("module://am8xControl/img/am8xDevice.png");
    }

    ////////////////////////////////////////////////////////////////
    // Boilerplate
    ////////////////////////////////////////////////////////////////

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDiscoveryEntry.class);
}
