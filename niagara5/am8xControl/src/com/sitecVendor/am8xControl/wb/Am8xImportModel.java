package com.sitecVendor.am8xControl.wb;

import com.sitecVendor.am8xControl.discovery.BAm8xDiscoveryCandidate;
import niagara.sys.BBoolean;
import niagara.sys.BComponent;
import niagara.sys.BInteger;
import niagara.sys.BString;
import niagara.sys.BValue;
import niagara.sys.Context;
import niagara.ui.BNullWidget;
import niagara.ui.BWidget;
import niagara.workbench.mgr.MgrColumn;
import niagara.workbench.mgr.MgrEditRow;
import niagara.workbench.mgr.MgrModel;

/**
 * Model del pannello Database — che in questa vista NON viene disegnato
 * (vedi makePane): l'import lavora solo sul pannello Discover.
 *
 * Il model resta comunque completo perché il framework lo pretende: la BMgrTable
 * è agganciata da BAbstractManager.init() e le colonne sono quelle su cui il
 * manager si sincronizza. Scansiona il component tree del service via accept +
 * getSubscribeDepth, quindi le modifiche scrivono direttamente sui BComponent
 * in station.
 */
public class Am8xImportModel extends MgrModel {

    public Am8xImportModel(BAm8xImportManager manager) {
        super(manager);
    }

    /** Filtra per tipo: include solo BAm8xDiscoveryCandidate. */
    @Override
    public niagara.sys.Type[] getIncludeTypes() {
        return new niagara.sys.Type[]{ BAm8xDiscoveryCandidate.TYPE };
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
     * Il pannello Database non viene disegnato: questa vista è solo Discover.
     *
     * super.makePane() va invocato lo stesso perché è lì che nasce la BMgrTable
     * (this.table = makeTable(), più attach e setTransferWidget sul manager), e
     * BAbstractManager.init() fa linkTo(getModel().getTable(), selectionModified,
     * handleDbSelection): con table a null andrebbe in NPE. Si scarta solo il
     * BTitlePane che avvolge la tabella, così spariscono griglia e titolo
     * "Database" senza toccare il resto del cablaggio del framework.
     */
    @Override
    public BWidget makePane() {
        super.makePane();
        return new BNullWidget();
    }
}
