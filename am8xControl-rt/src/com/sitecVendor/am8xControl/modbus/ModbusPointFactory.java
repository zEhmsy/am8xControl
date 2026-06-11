package com.sitecVendor.am8xControl.modbus;

import com.tridium.modbusCore.client.point.BModbusClientNumericProxyExt;
import com.tridium.modbusCore.client.point.BModbusClientEnumBitsProxyExt;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BAddressFormatEnum;
import com.tridium.modbusCore.enums.BDataTypeEnum;

import javax.baja.control.BNumericPoint;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BNumericWritable;
import javax.baja.naming.BOrd;
import javax.baja.sys.*;
import com.tridium.modbusCore.client.point.BModbusClientBooleanProxyExt;
import java.util.logging.Logger;

/**
 * Creates Modbus TCP points for the AM-8x00 scheme.
 *
 * Each sensor in a Loop folder produces:
 *   {slotName}         — BAm8xStatePoint (enum read + actions)
 *   {slotName}_Analog  — BNumericPoint (misura fisica)
 *   Link: Analog.out → StatePoint.valoreCamera
 */
public final class ModbusPointFactory {

    private static final Logger LOG = Logger.getLogger(ModbusPointFactory.class.getName());

    /** Tag Enum per lo stato AM-8x (ordinal 0-6). */
    static final String[] STATE_TAGS = {
        "Normale",
        "Non$20Programmato",
        "Escluso",
        "Test",
        "Allarme",
        "Guasto",
        "PreAllarme"
    };

    private ModbusPointFactory() {}

    // ================================================================
    // 1. BAm8xStatePoint (enum read-only + azioni + metadata)
    // ================================================================

    /**
     * Creates a BAm8xStatePoint with:
     *   - Modbus EnumBits proxy for reading the state register
     *   - 4 metadata slots (deviceType, deviceLabel, zoneAddress, zoneLabel)
     *   - ValoreCamera slot for analog link
     *   - Actions: esclusione / inclusione (writes via internal PresetRegisters)
     *
     * @return the created BAm8xStatePoint, or null if skipped/failed
     */
    public static BAm8xStatePoint createStatePoint(
            BComponent parent, String slotName, int address,
            String deviceType, String deviceLabel, int zoneAddress, String zoneLabel) {

        try {
            BValue existing = parent.get(slotName);
            if (existing instanceof BAm8xModuleStatePoint) {
                return replaceStatePoint(parent, slotName, (BAm8xStatePoint) existing, address,
                        deviceType, deviceLabel, zoneAddress, zoneLabel);
            }
            if (existing instanceof BAm8xStatePoint) {
                BAm8xStatePoint existingPoint = (BAm8xStatePoint) existing;
                existingPoint.ensureCommandConfigSlots();
                return existingPoint;
            }
            if (existing != null) return null;
        } catch (Exception ignore) {}

        try {
            BAm8xStatePoint pt = new BAm8xStatePoint();

            // Configura il range di enumerazione
            BEnumRange range = BEnumRange.make(STATE_TAGS);
            BFacets facets = BFacets.makeEnum(range);
            pt.setFacets(facets);

            // Proxy Modbus: EnumBits per leggere un registro intero come ordinale enum
            BModbusClientEnumBitsProxyExt ext = new BModbusClientEnumBitsProxyExt();
            ext.setBeginningBit(0);
            ext.setNumberOfBits(16);
            pt.setProxyExt(ext);

            // Aggiunge il punto al parent
            parent.add(slotName, pt, Flags.SUMMARY);

            // Configura l'indirizzo Modbus
            configureFlexAddress(ext, address);

            // Popola i metadata
            pt.setDeviceType(deviceType != null ? deviceType : "");
            pt.setDeviceLabel(deviceLabel != null ? deviceLabel : "");
            pt.setZoneAddress(String.valueOf(zoneAddress));
            pt.setZoneLabel(zoneLabel != null ? zoneLabel : "");
            pt.ensureCommandConfigSlots();

            LOG.info("[ModbusPointFactory] created state point '" + slotName + "' addr=" + address);
            return pt;
        } catch (Exception e) {
            LOG.warning("[ModbusPointFactory] createStatePoint '" + slotName
                    + "' addr=" + address + " failed: " + e.getMessage());
            return null;
        }
    }

    public static BAm8xStatePoint migrateStatePointType(BComponent parent, BAm8xStatePoint oldPoint) {
        if (parent == null || oldPoint == null) return null;

        String slotName = oldPoint.getName();
        int address = readProxyAddress(oldPoint);
        int zoneAddress = parseInt(oldPoint.getZoneAddress(), 0);
        return replaceStatePoint(parent, slotName, oldPoint, address,
                oldPoint.getDeviceType(), oldPoint.getDeviceLabel(), zoneAddress, oldPoint.getZoneLabel());
    }

    private static BAm8xStatePoint replaceStatePoint(
            BComponent parent, String slotName, BAm8xStatePoint oldPoint, int address,
            String deviceType, String deviceLabel, int zoneAddress, String zoneLabel) {

        boolean inhibit = readBooleanSlot(oldPoint, "inhibitCMD", false);
        try {
            parent.remove(slotName);
        } catch (Exception e) {
            LOG.warning("[ModbusPointFactory] cannot replace state point '" + slotName
                    + "': remove failed: " + e.getMessage());
            oldPoint.ensureCommandConfigSlots();
            return oldPoint;
        }

        BAm8xStatePoint newPoint = createStatePoint(parent, slotName, address,
                deviceType, deviceLabel, zoneAddress, zoneLabel);
        if (isModuleStateSlot(slotName) && inhibit) {
            newPoint.setInhibitCMD(true);
        }
        return newPoint;
    }

    public static boolean isModuleStateSlot(String slotName) {
        return slotName != null && slotName.matches("L\\d+M\\d+(_\\d+)?");
    }

    // ================================================================
    // 2. BNumericPoint (Analog)
    // ================================================================

    /**
     * Creates a BNumericPoint + BModbusClientNumericProxyExt.
     * @return the created BNumericPoint, or null if skipped/failed
     */
    public static BNumericPoint createNumericPoint(BComponent parent, String slotName, int address) {
        try {
            BValue existing = parent.get(slotName);
            if (existing != null) return existing instanceof BNumericPoint ? (BNumericPoint) existing : null;
        } catch (Exception ignore) {}

        try {
            BNumericPoint pt = new BNumericPoint();
            BModbusClientNumericProxyExt ext = new BModbusClientNumericProxyExt();
            ext.setDataType(BDataTypeEnum.signedInteger);
            pt.setProxyExt(ext);
            parent.add(slotName, pt, Flags.SUMMARY);
            configureFlexAddress(ext, address);
            LOG.fine("[ModbusPointFactory] created numeric '" + slotName + "' addr=" + address);
            return pt;
        } catch (Exception e) {
            LOG.warning("[ModbusPointFactory] createNumericPoint '" + slotName
                    + "' addr=" + address + " failed: " + e.getMessage());
            return null;
        }
    }

    // ================================================================
    // 3. Link: Analog.out → StatePoint.valoreCamera
    // ================================================================

    /**
     * Crea un BLink dall'out di sourcePoint allo slot targetSlotName sul targetPoint.
     */
    public static void createLink(BNumericPoint sourcePoint, BComponent targetPoint, String targetSlotName) {
        try {
            Slot srcSlot = sourcePoint.getSlot("out");
            Slot tgtSlot = targetPoint.getSlot(targetSlotName);
            if (srcSlot == null) {
                LOG.warning("[ModbusPointFactory] link: source slot 'out' not found");
                return;
            }
            if (tgtSlot == null) {
                LOG.warning("[ModbusPointFactory] link: target slot '" + targetSlotName + "' not found");
                return;
            }

            // Verifica se il link esiste già
            BLink[] existingLinks = targetPoint.getLinks();
            if (existingLinks != null) {
                for (BLink el : existingLinks) {
                    if (targetSlotName.equals(el.getTargetSlotName())
                            && "out".equals(el.getSourceSlotName())) {
                        return; // già linkato
                    }
                }
            }

            String sourceSlotPath = sourcePoint.getSlotPath().toString();
            BOrd sourceOrd = BOrd.make("station:|" + sourceSlotPath);
            BLink link = new BLink(sourceOrd, "out", targetSlotName, true);
            link.setEnabled(true);
            targetPoint.add(null, link);

            LOG.info("[ModbusPointFactory] linked " + sourceSlotPath + ".out -> " + targetSlotName);
        } catch (Exception e) {
            LOG.warning("[ModbusPointFactory] createLink failed: " + e.getMessage());
        }
    }

    // ================================================================
    // 4. Punti zona per il device CENTRALE
    // ================================================================

    public static void populateCentralePoints(BComponent centralePoints, java.util.Map<Integer, String> zones) {
        if (centralePoints == null) return;

        // COILS (Letture)
        createBooleanPoint(centralePoints, "Buzzer", 321);
        createBooleanPoint(centralePoints, "Alarm", 322);
        createBooleanPoint(centralePoints, "Fault", 324);
        createBooleanPoint(centralePoints, "Exclusion", 326);
        createBooleanPoint(centralePoints, "Horn_Silenced", 327);

        // HOLDING (Scritture)
        createNumericWritable(centralePoints, "Buzzer_Off", 80);
        createNumericWritable(centralePoints, "Reset", 81);
        createNumericWritable(centralePoints, "Toggle_Mute_Horn", 82);

        if (zones == null || zones.isEmpty()) return;

        for (java.util.Map.Entry<Integer, String> entry : zones.entrySet()) {
            int zoneAddr = entry.getKey();
            String label = entry.getValue();
            String slotName = "Z" + String.format("%03d", zoneAddr); // Z001, Z002
            if (label != null && !label.isEmpty()) {
                slotName = slotName + "_" + label.replaceAll("[^a-zA-Z0-9_]", "_");
            }

            int modbusAddr = 100 + zoneAddr; // 101 per Z001
            createNumericWritable(centralePoints, slotName, modbusAddr);
        }
    }

    public static BBooleanPoint createBooleanPoint(BComponent parent, String slotName, int address) {
        try {
            BValue existing = parent.get(slotName);
            if (existing != null) return existing instanceof BBooleanPoint ? (BBooleanPoint) existing : null;

            BBooleanPoint pt = new BBooleanPoint();
            BModbusClientBooleanProxyExt ext = new BModbusClientBooleanProxyExt();
            pt.setProxyExt(ext);
            parent.add(slotName, pt, Flags.SUMMARY);
            configureFlexAddress(ext, address);
            LOG.fine("[ModbusPointFactory] created boolean '" + slotName + "' addr=" + address);
            return pt;
        } catch (Exception e) {
            LOG.warning("[ModbusPointFactory] createBooleanPoint '" + slotName + "' failed: " + e.getMessage());
            return null;
        }
    }

    public static BNumericWritable createNumericWritable(BComponent parent, String slotName, int address) {
        try {
            BValue existing = parent.get(slotName);
            if (existing != null) return existing instanceof BNumericWritable ? (BNumericWritable) existing : null;

            BNumericWritable pt = new BNumericWritable();
            BModbusClientNumericProxyExt ext = new BModbusClientNumericProxyExt();
            pt.setProxyExt(ext);
            parent.add(slotName, pt, Flags.SUMMARY);
            configureFlexAddress(ext, address);
            LOG.fine("[ModbusPointFactory] created numeric writable '" + slotName + "' addr=" + address);
            return pt;
        } catch (Exception e) {
            LOG.warning("[ModbusPointFactory] createNumericWritable '" + slotName + "' failed: " + e.getMessage());
            return null;
        }
    }

    // ================================================================
    // Private helpers
    // ================================================================

    static void configureFlexAddress(BComponent ext, int addr) {
        try {
            Object fa = null;
            try { fa = ext.get("dataAddress"); } catch (Exception ignore) {}
            if (fa == null) {
                try { fa = ext.get("startingAddress"); } catch (Exception ignore) {}
            }
            if (!(fa instanceof BFlexAddress)) {
                LOG.warning("[ModbusPointFactory] no FlexAddress found on " + ext.getClass().getSimpleName());
                return;
            }
            BFlexAddress flexAddr = (BFlexAddress) fa;
            flexAddr.setAddressFormat(BAddressFormatEnum.make(1)); // 1 = decimal
            flexAddr.setAddress(String.valueOf(addr));
        } catch (Exception e) {
            LOG.warning("[ModbusPointFactory] configureFlexAddress addr=" + addr + " failed: " + e.getMessage());
        }
    }

    private static int readProxyAddress(BAm8xStatePoint point) {
        try {
            BComponent proxyExt = (BComponent) point.getProxyExt();
            if (proxyExt == null) return 0;

            Object fa = null;
            try { fa = proxyExt.get("dataAddress"); } catch (Exception ignore) {}
            if (fa == null) {
                try { fa = proxyExt.get("startingAddress"); } catch (Exception ignore) {}
            }
            if (fa instanceof BFlexAddress) {
                String address = ((BFlexAddress) fa).getAddress();
                return parseInt(address, 0);
            }
        } catch (Exception e) {
            LOG.warning("[ModbusPointFactory] readProxyAddress failed: " + e.getMessage());
        }
        return 0;
    }

    private static boolean readBooleanSlot(BComponent component, String slotName, boolean fallback) {
        try {
            BValue value = component.get(slotName);
            return value instanceof BBoolean ? ((BBoolean) value).getBoolean() : fallback;
        } catch (Exception ignore) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignore) {
            return fallback;
        }
    }
}
