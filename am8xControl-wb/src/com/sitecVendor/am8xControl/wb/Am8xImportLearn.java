package com.sitecVendor.am8xControl.wb;

import com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryCandidate;
import com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryReport;
import com.sitecVendor.am8xControl.discovery.BAm8xPanelFolder;
import com.sitecVendor.am8xControl.service.BAm8xImportService;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.workbench.mgr.MgrColumn;
import javax.baja.workbench.mgr.MgrEditRow;
import javax.baja.workbench.mgr.MgrLearn;

public class Am8xImportLearn extends MgrLearn {

    public Am8xImportLearn(BAm8xImportManager manager) {
        super(manager);
    }

    @Override
    protected MgrColumn[] makeColumns() {
        return new MgrColumn[]{

            // Colonna 0 — Name (usata dal framework per il group header label)
            new MgrColumn.Name() {
                @Override public Object get(Object item) {
                    if (item instanceof BAm8xPanelFolder)
                        return ((BAm8xPanelFolder) item).getName();
                    if (item instanceof BAm8xDiscoveryCandidate)
                        return ((BAm8xDiscoveryCandidate) item).getCandidateSlotName();
                    return super.get(item);
                }
            },

            // Colonna 1 — Selected (readonly: marcato/non marcato per Add/Cancel a livello centrale)
            new MgrColumn("Added", MgrColumn.READONLY) {
                @Override public Object get(Object item) {
                    if (item instanceof BAm8xDiscoveryCandidate)
                        return BBoolean.make(((BAm8xDiscoveryCandidate) item).getSelected());
                    if (item instanceof BAm8xPanelFolder) {
                        BAm8xPanelFolder pf = (BAm8xPanelFolder) item;
                        long sel = pf.getCandidates().stream()
                                .filter(BAm8xDiscoveryCandidate::getSelected).count();
                        long total = pf.getCandidates().size();
                        if (sel == 0) return BString.make("—");
                        if (sel == total) return BString.make("✓ tutti (" + total + ")");
                        return BString.make(sel + " / " + total);
                    }
                    return BString.make("");
                }
            },

            // Colonna 2 — Slot
            new MgrColumn("Slot") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? ((BAm8xDiscoveryCandidate) item).getCandidateSlotName() : "";
                }
            },

            // Colonna 3 — Type (EDITABLE)
            new MgrColumn("Type", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? ((BAm8xDiscoveryCandidate) item).getDeviceType() : "";
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getDiscovery();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getDeviceType())
                        : BString.make("");
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getDiscovery();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BString)
                        ((BAm8xDiscoveryCandidate) item).setDeviceType(((BString) v).getString());
                }
            },

            // Colonna 4 — Label (EDITABLE)
            new MgrColumn("Label", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? ((BAm8xDiscoveryCandidate) item).getDeviceLabel() : "";
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getDiscovery();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getDeviceLabel())
                        : BString.make("");
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getDiscovery();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BString)
                        ((BAm8xDiscoveryCandidate) item).setDeviceLabel(((BString) v).getString());
                }
            },

            // Colonna 5 — Loop
            new MgrColumn("Loop") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? ((BAm8xDiscoveryCandidate) item).getLoopNumber() : 0;
                }
            },

            // Colonna 6 — Pos
            new MgrColumn("Pos") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? ((BAm8xDiscoveryCandidate) item).getPositionOnLoop() : 0;
                }
            },

            // Colonna 7 — Zone (EDITABLE)
            new MgrColumn("Zone", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? ((BAm8xDiscoveryCandidate) item).getZoneLabel() : "";
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getDiscovery();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getZoneLabel())
                        : BString.make("");
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getDiscovery();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BString)
                        ((BAm8xDiscoveryCandidate) item).setZoneLabel(((BString) v).getString());
                }
            },

            // Colonna 8 — State Addr (EDITABLE)
            new MgrColumn("State Addr", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? ((BAm8xDiscoveryCandidate) item).getStateAddress() : 0;
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getDiscovery();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BInteger.make(((BAm8xDiscoveryCandidate) item).getStateAddress())
                        : BInteger.make(0);
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getDiscovery();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BInteger)
                        ((BAm8xDiscoveryCandidate) item).setStateAddress(((BInteger) v).getInt());
                }
            },

            // Colonna 9 — Analog Addr (EDITABLE)
            new MgrColumn("Analog Addr", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? ((BAm8xDiscoveryCandidate) item).getAnalogAddress() : 0;
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getDiscovery();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BInteger.make(((BAm8xDiscoveryCandidate) item).getAnalogAddress())
                        : BInteger.make(0);
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getDiscovery();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BInteger)
                        ((BAm8xDiscoveryCandidate) item).setAnalogAddress(((BInteger) v).getInt());
                }
            },

            // Colonna 10 — Imported (già in Modbus tree)
            new MgrColumn("Imported", MgrColumn.READONLY) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BBoolean.make(((BAm8xDiscoveryCandidate) item).getAlreadyImported())
                        : null;
                }
            },
        };
    }

    // ────────────────────────────────────────────────────────────────
    // Tree hierarchical: PanelFolder = group, Candidate = leaf
    // (le icone Central/Device vengono già dalle classi BAm8xPanelFolder
    // e BAm8xDiscoveryCandidate via getIcon())
    // ────────────────────────────────────────────────────────────────

    @Override
    public boolean isGroup(Object item) {
        return item instanceof BAm8xPanelFolder;
    }

    @Override
    public boolean hasChildren(Object item) {
        return item instanceof BAm8xPanelFolder
            && !((BAm8xPanelFolder) item).getCandidates().isEmpty();
    }

    @Override
    public Object[] getChildren(Object item) {
        if (item instanceof BAm8xPanelFolder)
            return ((BAm8xPanelFolder) item).getCandidates().toArray();
        return new Object[0];
    }

    @Override
    public boolean isDepthExpandable(int depth) {
        return depth == 0;
    }

    @Override
    public void toRow(Object item, MgrEditRow row) {
        MgrColumn[] cols = getColumns();
        for (MgrColumn col : cols) {
            Object val = col.get(item);
            if (val instanceof BValue) row.setCell(col, (BValue) val);
            else if (val instanceof Integer) row.setCell(col, BInteger.make((Integer) val));
            else if (val != null) row.setCell(col, BString.make(val.toString()));
        }
    }

    @Override
    public javax.baja.workbench.mgr.MgrTypeInfo[] toTypes(Object item) throws Exception {
        return new javax.baja.workbench.mgr.MgrTypeInfo[0];
    }

    @Override
    public boolean isExisting(Object item, javax.baja.sys.BComponent parent) {
        return item instanceof BAm8xDiscoveryCandidate
            && ((BAm8xDiscoveryCandidate) item).getAlreadyImported();
    }

    // ────────────────────────────────────────────────────────────────
    // Aggiornamento dati dal report di discovery
    // ────────────────────────────────────────────────────────────────

    public void updateDiscoveryData() {
        BAm8xImportService service =
                (BAm8xImportService) ((BAm8xImportManager) getManager()).getCurrentValue();
        if (service == null) { updateRoots(new Object[0]); return; }
        BValue reportVal = service.get("discovery");
        if (reportVal instanceof BAm8xDiscoveryReport) {
            updateRoots(((BAm8xDiscoveryReport) reportVal).getPanelFolders().toArray());
        } else {
            updateRoots(new Object[0]);
        }
    }
}
