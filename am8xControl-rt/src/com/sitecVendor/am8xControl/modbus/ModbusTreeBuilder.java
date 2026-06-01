package com.sitecVendor.am8xControl.modbus;

import com.tridium.modbusCore.client.point.BModbusClientPointFolder;
import com.tridium.modbusTcp.BModbusTcpDevice;
import com.tridium.modbusTcp.BModbusTcpNetwork;
import javax.baja.sys.*;
import java.util.logging.Logger;

/**
 * Navigates and creates the Modbus TCP component tree under /Drivers.
 *
 * Ensures components idempotently: find-or-create for each level.
 * All mutations happen in engine-thread context (service action) — add() persists.
 */
public final class ModbusTreeBuilder {

    private static final Logger LOG = Logger.getLogger(ModbusTreeBuilder.class.getName());

    private ModbusTreeBuilder() {}

    /**
     * Finds or creates a BModbusTcpNetwork under the given parent component.
     * First tries the exact slot name; then scans children for any existing network.
     */
    public static BModbusTcpNetwork ensureNetwork(BComponent parent, String networkSlot) {
        try {
            BValue v = parent.get(networkSlot);
            if (v instanceof BModbusTcpNetwork) return (BModbusTcpNetwork) v;
        } catch (Exception ignore) {}

        // Scan for any existing network
        for (Property p : parent.getPropertiesArray()) {
            try {
                BValue v = parent.get(p);
                if (v instanceof BModbusTcpNetwork) {
                    LOG.info("[ModbusTreeBuilder] reusing existing BModbusTcpNetwork at " + p.getName());
                    return (BModbusTcpNetwork) v;
                }
            } catch (Exception ignore) {}
        }

        BModbusTcpNetwork net = new BModbusTcpNetwork();
        try {
            parent.add(networkSlot, net, Flags.SUMMARY);
            BValue v = parent.get(networkSlot);
            if (v instanceof BModbusTcpNetwork) return (BModbusTcpNetwork) v;
        } catch (Exception e) {
            LOG.warning("[ModbusTreeBuilder] ensureNetwork add failed: " + e.getMessage());
        }
        return net;
    }

    /**
     * Finds or creates a BModbusTcpDevice under the given network.
     * Lookup is by slot name only — multiple panels can coexist on the same
     * IP (different slave addresses) provided each has a distinct slot name.
     *
     * @param deviceAddress Modbus slave/unit ID (1–247). Ignored if &lt;= 0.
     */
    public static BModbusTcpDevice ensureDevice(BModbusTcpNetwork network,
                                                 String deviceSlot, String ip,
                                                 int port, int deviceAddress) {
        try {
            BValue v = network.get(deviceSlot);
            if (v instanceof BModbusTcpDevice) {
                BModbusTcpDevice dev = (BModbusTcpDevice) v;
                applySettings(dev, ip, port, deviceAddress);
                return dev;
            }
        } catch (Exception ignore) {}

        BModbusTcpDevice dev = new BModbusTcpDevice();
        applySettings(dev, ip, port, deviceAddress);
        try {
            network.add(deviceSlot, dev, Flags.SUMMARY);
            BValue v = network.get(deviceSlot);
            if (v instanceof BModbusTcpDevice) return (BModbusTcpDevice) v;
        } catch (Exception e) {
            LOG.warning("[ModbusTreeBuilder] ensureDevice add failed: " + e.getMessage());
        }
        return dev;
    }

    /**
     * Returns the "points" device extension under a BModbusTcpDevice.
     * Points and folders MUST live under this slot for the Modbus proxy
     * extensions to be polled — adding them as direct children of the
     * device leaves them outside the poll cycle.
     */
    public static BComponent getPointsContainer(BModbusTcpDevice device) {
        try {
            BValue v = device.get("points");
            if (v instanceof BComponent) return (BComponent) v;
        } catch (Exception e) {
            LOG.warning("[ModbusTreeBuilder] getPointsContainer failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds or creates a BModbusClientPointFolder under the given parent.
     * Used for loop folders (L01, L02) and device folders (L01S002, L01M003).
     *
     * Must be a BModbusClientPointFolder (not plain BComponent) so the Database
     * view of the Modbus device walks into it and shows the contained points.
     */
    public static BComponent ensureFolder(BComponent parent, String slotName) {
        try {
            BValue v = parent.get(slotName);
            if (v instanceof BComponent) return (BComponent) v;
        } catch (Exception ignore) {}
        BModbusClientPointFolder folder = new BModbusClientPointFolder();
        try {
            parent.add(slotName, folder, Flags.SUMMARY);
            BValue v = parent.get(slotName);
            if (v instanceof BComponent) return (BComponent) v;
        } catch (Exception e) {
            LOG.warning("[ModbusTreeBuilder] ensureFolder '" + slotName + "' failed: " + e.getMessage());
        }
        return folder;
    }

    private static void applySettings(BModbusTcpDevice dev, String ip, int port, int deviceAddress) {
        try { if (ip != null && !ip.equals(dev.getIpAddress())) dev.setIpAddress(ip); } catch (Exception ignore) {}
        try { if (port > 0 && dev.getPort() != port) dev.setPort(port); } catch (Exception ignore) {}
        try {
            if (deviceAddress > 0) {
                // BModbusTcpDevice property is "deviceAddress" (slave/unit ID, 1–247)
                BValue current = dev.get("deviceAddress");
                if (current instanceof BInteger) {
                    int cur = ((BInteger) current).getInt();
                    if (cur != deviceAddress) dev.set("deviceAddress", BInteger.make(deviceAddress));
                } else {
                    dev.set("deviceAddress", BInteger.make(deviceAddress));
                }
            }
        } catch (Exception e) {
            LOG.warning("[ModbusTreeBuilder] applySettings deviceAddress=" + deviceAddress
                    + " failed: " + e.getMessage());
        }
    }
}
