package com.sitecVendor.am8xControl.wb;

import com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryCandidate;
import com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryReport;
import com.sitecVendor.am8xControl.service.BAm8xImportService;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.workbench.mgr.MgrColumn;
import javax.baja.workbench.mgr.MgrEditRow;
import javax.baja.workbench.mgr.MgrModel;

/**
 * Database pane: mostra tutti i BAm8xDiscoveryCandidate come tabella piatta editabile.
 * MgrModel scansiona il component tree del service (via accept + getSubscribeDepth),
 * quindi le modifiche scrivono direttamente sui BComponent in station.
 */
public class Am8xImportModel extends MgrModel {

    public Am8xImportModel(BAm8xImportManager manager) {
        super(manager);
    }

    /** Filtra per tipo: include solo BAm8xDiscoveryCandidate. */
    @Override
    public javax.baja.sys.Type[] getIncludeTypes() {
        return new javax.baja.sys.Type[]{ BAm8xDiscoveryCandidate.TYPE };
    }

    @Override
    public boolean accept(BComponent c) {
        return c instanceof BAm8xDiscoveryCandidate;
    }

    /** service(0) → discoveryReport(1) → panelFolder(2) → candidate(3). */
    @Override
    public int getSubscribeDepth() { return 3; }

    @Override
    protected MgrColumn[] makeColumns() {
        return new MgrColumn[]{

            new MgrColumn("Slot", MgrColumn.READONLY) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getCandidateSlotName())
                        : BString.make("");
                }
            },

            new MgrColumn("Panel", MgrColumn.READONLY) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getPanelLabel())
                        : BString.make("");
                }
            },

            new MgrColumn("Type", MgrColumn.READONLY) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getDeviceType())
                        : BString.make("");
                }
            },

            new MgrColumn("Label", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getDeviceLabel())
                        : BString.make("");
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getTarget();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getDeviceLabel())
                        : BString.make("");
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getTarget();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BString)
                        ((BAm8xDiscoveryCandidate) item).setDeviceLabel(((BString) v).getString());
                }
            },

            new MgrColumn("Zone", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getZoneLabel())
                        : BString.make("");
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getTarget();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BString.make(((BAm8xDiscoveryCandidate) item).getZoneLabel())
                        : BString.make("");
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getTarget();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BString)
                        ((BAm8xDiscoveryCandidate) item).setZoneLabel(((BString) v).getString());
                }
            },

            new MgrColumn("State Addr", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BInteger.make(((BAm8xDiscoveryCandidate) item).getStateAddress())
                        : BInteger.make(0);
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getTarget();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BInteger.make(((BAm8xDiscoveryCandidate) item).getStateAddress())
                        : BInteger.make(0);
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getTarget();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BInteger)
                        ((BAm8xDiscoveryCandidate) item).setStateAddress(((BInteger) v).getInt());
                }
            },

            new MgrColumn("Analog Addr", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BInteger.make(((BAm8xDiscoveryCandidate) item).getAnalogAddress())
                        : BInteger.make(0);
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getTarget();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BInteger.make(((BAm8xDiscoveryCandidate) item).getAnalogAddress())
                        : BInteger.make(0);
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getTarget();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BInteger)
                        ((BAm8xDiscoveryCandidate) item).setAnalogAddress(((BInteger) v).getInt());
                }
            },

            new MgrColumn("Selected", MgrColumn.EDITABLE) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BBoolean.make(((BAm8xDiscoveryCandidate) item).getSelected())
                        : BBoolean.FALSE;
                }
                @Override public BValue load(MgrEditRow row) throws Exception {
                    Object item = row.getTarget();
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BBoolean.make(((BAm8xDiscoveryCandidate) item).getSelected())
                        : BBoolean.FALSE;
                }
                @Override public void save(MgrEditRow row, BValue v, Context cx) throws Exception {
                    Object item = row.getTarget();
                    if (item instanceof BAm8xDiscoveryCandidate && v instanceof BBoolean)
                        ((BAm8xDiscoveryCandidate) item).setSelected(((BBoolean) v).getBoolean());
                }
            },

            new MgrColumn("Imported", MgrColumn.READONLY) {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDiscoveryCandidate
                        ? BBoolean.make(((BAm8xDiscoveryCandidate) item).getAlreadyImported())
                        : BBoolean.FALSE;
                }
            },
        };
    }

    /**
     * Popola il DB pane scansionando dalla BAm8xDiscoveryReport (depth 2).
     * Più affidabile che reload() perché imposta esplicitamente il container.
     */
    public void updateDiscoveryData() {
        try {
            BAm8xImportService svc =
                    (BAm8xImportService) getManager().getCurrentValue();
            if (svc == null) { getTable().reload(); return; }
            javax.baja.sys.BValue rv = svc.get("discovery");
            if (rv instanceof BAm8xDiscoveryReport) {
                // Carica direttamente dalla report: panelFolder(1) → candidate(2)
                javax.baja.workbench.component.table.BComponentTable tbl =
                        (javax.baja.workbench.component.table.BComponentTable) getTable();
                tbl.load(
                    (BAm8xDiscoveryReport) rv,
                    new Class[]{ BAm8xDiscoveryCandidate.class },
                    2,
                    null);
            } else {
                getTable().reload();
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(Am8xImportModel.class.getName())
                .info("[Am8xImportModel] updateDiscoveryData failed: " + e);
        }
    }
}
