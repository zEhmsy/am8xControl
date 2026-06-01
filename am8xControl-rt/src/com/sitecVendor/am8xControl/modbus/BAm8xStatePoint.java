package com.sitecVendor.am8xControl.modbus;

import com.tridium.modbusCore.client.datatypes.BModbusClientPresetRegisters;
import com.tridium.modbusCore.client.datatypes.BModbusClientPresetRegister;
import com.tridium.modbusCore.datatypes.BFlexAddress;

import javax.baja.control.BEnumPoint;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.*;
import java.util.logging.Logger;

/**
 * Punto Stato custom per sensori AM-8x00.
 *
 * Estende BEnumPoint (sola lettura dal Modbus) aggiungendo:
 *   - 4 slot metadata (deviceType, deviceLabel, zoneAddress, zoneLabel)
 *   - Slot ValoreCamera (riceve link dall'analogico)
 *   - Azione "esclusione": scrive comando 1 sul registro Modbus via PresetRegisters
 *   - Azione "inclusione": scrive comando 2 sul registro Modbus via PresetRegisters
 *   - I comandi 3/4 sono visibili solo sulle istanze LxxMxxx
 *
 * Il componente BModbusClientPresetRegisters è un figlio interno, invisibile nel wiresheet
 * principale ma funzionale. Quando si invoca un comando, il preset register
 * scrive il valore sul bus e poi il proxy legge il nuovo stato aggiornato dalla centrale.
 */
@NiagaraType
@NoSlotomatic
public class BAm8xStatePoint extends BEnumPoint {

    private static final Logger LOG = Logger.getLogger(BAm8xStatePoint.class.getName());

    ////////////////////////////////////////////////////////////////
    // Slot: Metadata (BString, summary, editabili)
    ////////////////////////////////////////////////////////////////

    public static final Property deviceType =
            newProperty(Flags.SUMMARY, "", null);
    public String getDeviceType() { return getString(deviceType); }
    public void setDeviceType(String v) { setString(deviceType, v, null); }

    public static final Property deviceLabel =
            newProperty(Flags.SUMMARY, "", null);
    public String getDeviceLabel() { return getString(deviceLabel); }
    public void setDeviceLabel(String v) { setString(deviceLabel, v, null); }

    public static final Property zoneAddress =
            newProperty(Flags.SUMMARY, "", null);
    public String getZoneAddress() { return getString(zoneAddress); }
    public void setZoneAddress(String v) { setString(zoneAddress, v, null); }

    public static final Property zoneLabel =
            newProperty(Flags.SUMMARY, "", null);
    public String getZoneLabel() { return getString(zoneLabel); }
    public void setZoneLabel(String v) { setString(zoneLabel, v, null); }

    ////////////////////////////////////////////////////////////////
    // Slot: ValoreCamera (riceve il link dall'analogico)
    ////////////////////////////////////////////////////////////////

    public static final Property valoreCamera =
            newProperty(Flags.SUMMARY, new BStatusNumeric(), null);
    public BStatusNumeric getValoreCamera() { return (BStatusNumeric) get(valoreCamera); }
    public void setValoreCamera(BStatusNumeric v) { set(valoreCamera, v, null); }

    ////////////////////////////////////////////////////////////////
    // Actions: Esclusione / Inclusione
    ////////////////////////////////////////////////////////////////

    public boolean getInhibitCMD() {
        if (!isModuleStatePoint()) return false;
        try {
            ensureInhibitCmdSlot();
            BValue value = get(INHIBIT_CMD_SLOT);
            if (value instanceof BBoolean) return ((BBoolean) value).getBoolean();
        } catch (Exception e) {
            LOG.warning("[Am8xStatePoint] getInhibitCMD failed: " + e.getMessage());
        }
        return false;
    }

    public void setInhibitCMD(boolean v) {
        if (!isModuleStatePoint()) {
            cleanupInhibitCmdSlot();
            return;
        }
        try {
            ensureInhibitCmdSlot();
            set(INHIBIT_CMD_SLOT, BBoolean.make(v), null);
        } catch (Exception e) {
            LOG.warning("[Am8xStatePoint] setInhibitCMD failed: " + e.getMessage());
        }
    }

    /** Scrive il comando di Esclusione (valore 1) sul registro Modbus. */
    public static final Action esclusione = newAction(Flags.OPERATOR, null);
    public void esclusione() { invoke(esclusione, null, null); }
    public void doEsclusione() {
        writePresetCommand(1);
    }

    /** Scrive il comando di Inclusione (valore 2) sul registro Modbus. */
    public static final Action inclusione = newAction(Flags.OPERATOR, null);
    public void inclusione() { invoke(inclusione, null, null); }
    public void doInclusione() {
        writePresetCommand(2);
    }

    /** Scrive il comando di Attivazione (valore 3), solo per LxxMxxx. */
    public static final Action attivazione = newAction(Flags.OPERATOR, null);
    public void attivazione() { invoke(attivazione, null, null); }
    public void doAttivazione() {
        writeModulePresetCommand(3);
    }

    /** Scrive il comando di Disattivazione (valore 4), solo per LxxMxxx. */
    public static final Action disattivazione = newAction(Flags.OPERATOR, null);
    public void disattivazione() { invoke(disattivazione, null, null); }
    public void doDisattivazione() {
        writeModulePresetCommand(4);
    }

    public void cleanupLegacyCommandSlots() {
        try {
            if (get(LEGACY_DISATTIVABILE_SLOT) != null) remove(LEGACY_DISATTIVABILE_SLOT);
        } catch (Exception e) {
            LOG.warning("[Am8xStatePoint] cleanup legacy command slots failed: " + e.getMessage());
        }
    }

    public void ensureCommandConfigSlots() {
        cleanupLegacyCommandSlots();
        if (isModuleStatePoint()) {
            ensureInhibitCmdSlot();
            setModuleCommandActionsHidden(false);
        } else {
            cleanupInhibitCmdSlot();
            setModuleCommandActionsHidden(true);
        }
    }

    @Override
    public void started() throws Exception {
        super.started();
        ensureCommandConfigSlots();
    }

    ////////////////////////////////////////////////////////////////
    // Internal: PresetRegisters management
    ////////////////////////////////////////////////////////////////

    private static final String PRESET_SLOT = "presetCmd";
    private static final String REG_SLOT    = "commandValue";
    private static final String INHIBIT_CMD_SLOT = "inhibitCMD";
    private static final String LEGACY_DISATTIVABILE_SLOT = "disattivabile";

    protected boolean isModuleStatePoint() {
        String name = getName();
        return name != null && name.matches("L\\d+M\\d+(_\\d+)?");
    }

    private void setModuleCommandActionsHidden(boolean hidden) {
        int flags = hidden ? (Flags.OPERATOR | Flags.HIDDEN) : Flags.OPERATOR;
        try { setFlags(attivazione, flags); } catch (Exception ignore) {}
        try { setFlags(disattivazione, flags); } catch (Exception ignore) {}
    }

    protected void ensureInhibitCmdSlot() {
        try {
            if (get(INHIBIT_CMD_SLOT) == null) {
                add(INHIBIT_CMD_SLOT, BBoolean.FALSE, Flags.SUMMARY);
                return;
            }

            Slot slot = getSlot(INHIBIT_CMD_SLOT);
            if (slot instanceof Property) {
                setFlags((Property) slot, Flags.SUMMARY);
            }
        } catch (Exception e) {
            LOG.warning("[Am8xStatePoint] ensureInhibitCmdSlot failed: " + e.getMessage());
        }
    }

    protected void cleanupInhibitCmdSlot() {
        try {
            if (get(INHIBIT_CMD_SLOT) != null) remove(INHIBIT_CMD_SLOT);
        } catch (Exception e) {
            LOG.warning("[Am8xStatePoint] cleanup Inhibit CMD slot failed: " + e.getMessage());
        }
    }

    protected void writeModulePresetCommand(int commandValue) {
        ensureCommandConfigSlots();
        if (!isModuleStatePoint()) {
            LOG.warning("[Am8xStatePoint] command " + commandValue
                    + " ignored because module command was invoked on non-module point " + getSlotPath());
            return;
        }
        if (getInhibitCMD()) {
            LOG.warning("[Am8xStatePoint] command " + commandValue
                    + " ignored because Inhibit CMD=true on " + getSlotPath());
            return;
        }
        writePresetCommand(commandValue);
    }

    /**
     * Scrive un valore sul registro Modbus tramite il PresetRegisters interno.
     * Se il componente non esiste ancora (non è stato inizializzato), lo crea on-the-fly.
     */
    protected void writePresetCommand(int commandValue) {
        try {
            // Trova o crea il preset component
            BModbusClientPresetRegisters preset = getOrCreatePreset();
            if (preset == null) {
                LOG.warning("[Am8xStatePoint] preset component not available, cannot write command " + commandValue);
                return;
            }

            // Trova il registro figlio
            BValue rv = preset.get(REG_SLOT);
            if (!(rv instanceof BModbusClientPresetRegister)) {
                LOG.warning("[Am8xStatePoint] commandValue register not found in preset");
                return;
            }
            BModbusClientPresetRegister reg = (BModbusClientPresetRegister) rv;

            // Imposta il valore e invoca la scrittura
            reg.setValue(BInteger.make(commandValue));
            preset.doWrite();

            LOG.info("[Am8xStatePoint] wrote command " + commandValue + " on " + getSlotPath());
        } catch (Exception e) {
            LOG.warning("[Am8xStatePoint] writePresetCommand(" + commandValue + ") failed: " + e.getMessage());
        }
    }

    /**
     * Restituisce il BModbusClientPresetRegisters figlio, creandolo se necessario.
     */
    private BModbusClientPresetRegisters getOrCreatePreset() {
        try {
            BValue existing = get(PRESET_SLOT);
            if (existing instanceof BModbusClientPresetRegisters) {
                return (BModbusClientPresetRegisters) existing;
            }
        } catch (Exception ignore) {}

        // Se non esiste, crealo e configuralo con l'indirizzo dal proxy ext
        try {
            BModbusClientPresetRegisters preset = new BModbusClientPresetRegisters();
            add(PRESET_SLOT, preset, Flags.HIDDEN);

            // Configura lo startingAddress con lo stesso indirizzo del punto (dal proxy ext)
            configurePresetAddress(preset);

            // Aggiungi il registro figlio
            BModbusClientPresetRegister reg = new BModbusClientPresetRegister();
            preset.add(REG_SLOT, reg, Flags.SUMMARY);

            LOG.info("[Am8xStatePoint] created internal preset component");
            return preset;
        } catch (Exception e) {
            LOG.warning("[Am8xStatePoint] getOrCreatePreset failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Copia l'indirizzo Modbus dal proxy ext del punto al preset registers.
     */
    private void configurePresetAddress(BModbusClientPresetRegisters preset) {
        try {
            BComponent proxyExt = (BComponent) getProxyExt();
            if (proxyExt == null) return;

            Object srcAddr = proxyExt.get("dataAddress");
            if (!(srcAddr instanceof BFlexAddress)) return;

            BFlexAddress source = (BFlexAddress) srcAddr;
            BFlexAddress target = preset.getStartingAddress();
            target.setAddressFormat(source.getAddressFormat());
            target.setAddress(source.getAddress());
        } catch (Exception e) {
            LOG.warning("[Am8xStatePoint] configurePresetAddress failed: " + e.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////
    // Type
    ////////////////////////////////////////////////////////////////

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xStatePoint.class);
}
