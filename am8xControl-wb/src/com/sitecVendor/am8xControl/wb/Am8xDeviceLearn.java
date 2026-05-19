package com.sitecVendor.am8xControl.wb;

import com.sitecVendor.am8xControl.BAm8xDevice;
import com.sitecVendor.am8xControl.BAm8xDiscoveryEntry;
import com.tridium.ndriver.ui.NMgrLearn;
import javax.baja.workbench.mgr.BAbstractManager;
import javax.baja.workbench.mgr.MgrColumn;
import javax.baja.workbench.mgr.MgrEditRow;
import javax.baja.workbench.mgr.MgrTypeInfo;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Slot;
import java.util.ArrayList;
import java.util.List;

public class Am8xDeviceLearn extends NMgrLearn {

    public Am8xDeviceLearn(BAbstractManager manager) {
        super(manager);
    }

    ////////////////////////////////////////////////////////////////
    // MgrLearn
    ////////////////////////////////////////////////////////////////

    @Override
    protected MgrColumn[] makeColumns() {
        return new MgrColumn[]{
            // Override get() so the Name column shows getDiscoveryName()
            // instead of casting to BComponent (which BAm8xDiscoveryEntry is not).
            new MgrColumn.Name() {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryEntry
                        ? ((BAm8xDiscoveryEntry) item).getDiscoveryName()
                        : super.get(item);
                }
            },
            new MgrColumn("Panel") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryEntry
                        ? ((BAm8xDiscoveryEntry) item).getPanelLabel() : "";
                }
            },
            new MgrColumn("Loop") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryEntry
                        ? ((BAm8xDiscoveryEntry) item).getLoopNumber() : 0;
                }
            },
            new MgrColumn("Pos") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryEntry
                        ? ((BAm8xDiscoveryEntry) item).getPositionOnLoop() : 0;
                }
            },
            new MgrColumn("Type") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryEntry
                        ? ((BAm8xDiscoveryEntry) item).getDeviceType() : "";
                }
            },
            new MgrColumn("Zone") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryEntry
                        ? ((BAm8xDiscoveryEntry) item).getZoneLabel() : "";
                }
            },
        };
    }

    @Override
    public void toRow(Object item, MgrEditRow row) {
        if (!(item instanceof BAm8xDiscoveryEntry)) return;
        BAm8xDiscoveryEntry entry = (BAm8xDiscoveryEntry) item;

        // CRITICAL: framework NMgrLearn.toRow() normally calls entry.updateTarget(target)
        // to populate the device-to-be-added. Our override bypasses super, so we must
        // invoke it explicitly — otherwise the added device stays at default values.
        BComponent target = row.getTarget();
        if (target != null) {
            try {
                entry.updateTarget(target);
            } catch (Exception ignore) {}
        }

        MgrColumn[] cols = row.getColumns();
        for (int i = 0; i < cols.length; i++) {
            if (cols[i] instanceof MgrColumn.Name) continue; // name via setDefaultName
            Object val = cols[i].get(item);
            if (val instanceof Integer)
                row.setCell(i, BInteger.make((Integer) val));
            else if (val != null)
                row.setCell(i, BString.make(val.toString()));
        }
        row.setDefaultName(entry.getDiscoveryName());
    }

    @Override
    public MgrTypeInfo[] toTypes(Object item) {
        if (!(item instanceof BAm8xDiscoveryEntry) || hasChildren(item))
            return new MgrTypeInfo[0];
        // Suppress Add for items that already exist in the DB.
        // isMatchable() enables the Match button separately for those items.
        if (getExisting(item) != null) return new MgrTypeInfo[0];
        return MgrTypeInfo.makeArray(BAm8xDevice.TYPE);
    }

    @Override
    public boolean isMatchable(Object item, BComponent component) {
        // Match button enabled when the discovered item corresponds to this component.
        if (!(item instanceof BAm8xDiscoveryEntry)) return false;
        return matchesInTree((BAm8xDiscoveryEntry) item, component);
    }

    @Override
    public boolean isExisting(Object item, BComponent component) {
        if (!(item instanceof BAm8xDiscoveryEntry)) return false;
        return matchesInTree((BAm8xDiscoveryEntry) item, component);
    }

    // Recurse into child components so devices inside BNDeviceFolder are found.
    private static boolean matchesInTree(BAm8xDiscoveryEntry entry, BComponent comp) {
        if (entry.isExisting(comp)) return true;
        try {
            for (Slot s : comp.getSlotsArray()) {
                BValue child = comp.get(s.getName());
                if (child instanceof BComponent) {
                    if (matchesInTree(entry, (BComponent) child)) return true;
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    ////////////////////////////////////////////////////////////////
    // Group / children — M720 modules appear as expandable groups
    ////////////////////////////////////////////////////////////////

    @Override
    public boolean isGroup(Object item) {
        return hasChildren(item);
    }

    @Override
    public boolean hasChildren(Object item) {
        return item instanceof BAm8xDiscoveryEntry
            && !((BAm8xDiscoveryEntry) item).getChildrenData().isEmpty();
    }

    @Override
    public Object[] getChildren(Object item) {
        if (!(item instanceof BAm8xDiscoveryEntry)) return new Object[0];
        BAm8xDiscoveryEntry parent = (BAm8xDiscoveryEntry) item;
        String data = parent.getChildrenData();
        if (data == null || data.isEmpty()) return new Object[0];

        String[] records = data.split(BAm8xDiscoveryEntry.RECORD_SEP, -1);
        List<BAm8xDiscoveryEntry> children = new ArrayList<>();
        for (String record : records) {
            String[] f = record.split(BAm8xDiscoveryEntry.FIELD_SEP, -1);
            if (f.length < 5) continue;
            int number    = safeInt(f[2]);
            int zoneAddr  = safeInt(f[3]);
            // field[5] = parent M720 positionOnLoop (added in v2)
            int parentPos = f.length >= 6 ? safeInt(f[5]) : -1;
            BAm8xDiscoveryEntry child = new BAm8xDiscoveryEntry();
            child.setPanelType(parent.getPanelType());
            child.setPanelLabel(parent.getPanelLabel());
            child.setLoopNumber(parent.getLoopNumber());
            child.setPositionOnLoop(number);
            child.setDeviceType(f[0]);
            child.setDeviceLabel(f[1]);
            child.setZoneAddress(zoneAddr);
            child.setZoneLabel(f[4]);
            child.setParentModulePos(parentPos);
            children.add(child);
        }
        return children.toArray();
    }

    @Override
    public boolean isDepthExpandable(int depth) {
        return depth == 0; // only first level is expandable
    }

    ////////////////////////////////////////////////////////////////
    // Helpers
    ////////////////////////////////////////////////////////////////

    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
