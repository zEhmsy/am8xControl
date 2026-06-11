package com.sitecVendor.am8xControl.modbus;

import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.point.BModbusClientPointFolder;
import com.tridium.modbusTcp.BModbusTcpDevice;
import com.tridium.modbusTcp.BModbusTcpGateway;
import com.tridium.modbusTcp.BModbusTcpGatewayDevice;
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
     * Finds or creates the Modbus network under the given parent component using
     * IP-aware conflict resolution.
     *
     * <p>The target (ip, port) identifies a logical Modbus endpoint. Networks on
     * <i>different</i> IPs are independent and coexist freely. A conflict arises
     * only when an existing network already serves the target IP but has the
     * wrong topology type:
     * <ul>
     *   <li>existing {@link BModbusTcpNetwork} + requested gateway → conflict;</li>
     *   <li>existing {@link BModbusTcpGateway} + requested plain TCP → conflict.</li>
     * </ul>
     * On conflict the method does NOT cast or overwrite: it logs a warning and
     * throws {@link IllegalStateException}, which {@code doAddSelected} surfaces
     * to the operator as the import status / error.
     *
     * <p>IP location differs by type: a gateway carries its own {@code ipAddress};
     * a plain network has none — its IP lives on each {@link BModbusTcpDevice}
     * child, so a plain network "serves" an IP when any of its devices uses it.
     *
     * @param gateway when true creates/reuses a {@link BModbusTcpGateway}
     *                (devices are {@link BModbusTcpGatewayDevice} sharing the
     *                network IP/port); otherwise a plain {@link BModbusTcpNetwork}.
     * @param ip      target endpoint IP (gateway: applied to the network).
     * @param port    target endpoint TCP port.
     * @throws IllegalStateException on an IP-matched topology conflict.
     */
    public static BModbusTcpNetwork ensureNetwork(BComponent parent, String networkSlot,
                                                  boolean gateway, String ip, int port) {
        // 1. Is there already a network serving this exact IP/port?
        BModbusTcpNetwork ipMatch = findNetworkServingIp(parent, ip, port);
        if (ipMatch != null) {
            boolean existingIsGateway = ipMatch instanceof BModbusTcpGateway;
            if (existingIsGateway == gateway) {
                // Same IP, correct type → reuse.
                if (gateway) applyNetworkSettings((BModbusTcpGateway) ipMatch, ip, port);
                LOG.info("[ModbusTreeBuilder] reusing existing "
                        + (gateway ? "BModbusTcpGateway" : "BModbusTcpNetwork")
                        + " serving IP " + ip + ":" + port);
                return ipMatch;
            }
            // Same IP, wrong type → topology conflict: do not cast/overwrite.
            String existingType  = existingIsGateway ? "ModbusGateway" : "ModbusTcp";
            String requestedType = gateway ? "ModbusGateway" : "ModbusTcp";
            String msg = "Topology Conflict: Network at IP " + ip + ":" + port
                    + " already exists as " + existingType
                    + ", cannot instantiate as " + requestedType;
            LOG.warning("[ModbusTreeBuilder] " + msg);
            throw new IllegalStateException(msg);
        }

        // 2. No network serves this IP → independent endpoint, create a new network.
        BModbusTcpNetwork net = gateway ? new BModbusTcpGateway() : new BModbusTcpNetwork();
        String slot = freeSlot(parent, networkSlot);
        try {
            parent.add(slot, net, Flags.SUMMARY);
            BValue v = parent.get(slot);
            if (v instanceof BModbusTcpNetwork) net = (BModbusTcpNetwork) v;
        } catch (Exception e) {
            LOG.warning("[ModbusTreeBuilder] ensureNetwork add failed: " + e.getMessage());
        }
        if (gateway) applyNetworkSettings((BModbusTcpGateway) net, ip, port);
        return net;
    }

    /**
     * Returns the existing Modbus network (any type) under {@code parent} that
     * serves the given IP/port, or null if none. A gateway is matched on its own
     * {@code ipAddress}; a plain network on the IP of any of its devices.
     */
    private static BModbusTcpNetwork findNetworkServingIp(BComponent parent, String ip, int port) {
        if (ip == null || ip.isEmpty()) return null;
        for (Property p : parent.getPropertiesArray()) {
            BValue v;
            try { v = parent.get(p); } catch (Exception ignore) { continue; }
            if (!(v instanceof BModbusTcpNetwork)) continue;
            if (networkServesIp((BModbusTcpNetwork) v, ip, port)) {
                return (BModbusTcpNetwork) v;
            }
        }
        return null;
    }

    /** True when the network already targets {@code ip}/{@code port}. */
    private static boolean networkServesIp(BModbusTcpNetwork net, String ip, int port) {
        if (net instanceof BModbusTcpGateway) {
            BModbusTcpGateway gw = (BModbusTcpGateway) net;
            try {
                return ip.equals(gw.getIpAddress()) && (port <= 0 || gw.getPort() == port);
            } catch (Exception ignore) { return false; }
        }
        // Plain network: IP lives on the device children.
        for (Property p : net.getPropertiesArray()) {
            try {
                BValue v = net.get(p);
                if (v instanceof BModbusTcpDevice) {
                    BModbusTcpDevice dev = (BModbusTcpDevice) v;
                    if (ip.equals(dev.getIpAddress()) && (port <= 0 || dev.getPort() == port)) {
                        return true;
                    }
                }
            } catch (Exception ignore) {}
        }
        return false;
    }

    /** Returns the desired slot if free, otherwise the first free numbered variant. */
    private static String freeSlot(BComponent parent, String desired) {
        try {
            if (parent.get(desired) == null) return desired;
        } catch (Exception ignore) { return desired; }
        for (int i = 2; i < 100; i++) {
            String s = desired + i;
            try { if (parent.get(s) == null) return s; } catch (Exception ignore) { return s; }
        }
        return desired;
    }

    private static void applyNetworkSettings(BModbusTcpGateway net, String ip, int port) {
        try { if (ip != null && !ip.isEmpty() && !ip.equals(net.getIpAddress())) net.setIpAddress(ip); }
        catch (Exception ignore) {}
        try { if (port > 0 && net.getPort() != port) net.setPort(port); }
        catch (Exception ignore) {}
    }

    /**
     * Finds or creates a BModbusTcpDevice under the given network.
     * Lookup is by slot name only — multiple panels can coexist on the same
     * IP (different slave addresses) provided each has a distinct slot name.
     *
     * @param deviceAddress Modbus slave/unit ID (1–247). Ignored if &lt;= 0.
     * @param gateway       when true creates a {@link BModbusTcpGatewayDevice}
     *                      (IP/port inherited from the gateway network); otherwise
     *                      a {@link BModbusTcpDevice} carrying its own IP/port.
     */
    public static BModbusClientDevice ensureDevice(BModbusTcpNetwork network,
                                                   String deviceSlot, String ip,
                                                   int port, int deviceAddress,
                                                   boolean gateway) {
        try {
            BValue v = network.get(deviceSlot);
            if (v instanceof BModbusClientDevice) {
                BModbusClientDevice dev = (BModbusClientDevice) v;
                applySettings(dev, ip, port, deviceAddress);
                return dev;
            }
        } catch (Exception ignore) {}

        BModbusClientDevice dev = gateway ? new BModbusTcpGatewayDevice() : new BModbusTcpDevice();
        applySettings(dev, ip, port, deviceAddress);
        try {
            network.add(deviceSlot, dev, Flags.SUMMARY);
            BValue v = network.get(deviceSlot);
            if (v instanceof BModbusClientDevice) return (BModbusClientDevice) v;
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
    public static BComponent getPointsContainer(BModbusClientDevice device) {
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

    private static void applySettings(BModbusClientDevice dev, String ip, int port, int deviceAddress) {
        // IP/port live on the device only for plain TCP devices; gateway devices
        // inherit them from the BModbusTcpGateway network.
        if (dev instanceof BModbusTcpDevice) {
            BModbusTcpDevice tcp = (BModbusTcpDevice) dev;
            try { if (ip != null && !ip.equals(tcp.getIpAddress())) tcp.setIpAddress(ip); } catch (Exception ignore) {}
            try { if (port > 0 && tcp.getPort() != port) tcp.setPort(port); } catch (Exception ignore) {}
        }
        // deviceAddress (slave/unit ID, 1–247) lives on the BModbusDevice base — both kinds have it.
        try {
            if (deviceAddress > 0 && dev.getDeviceAddress() != deviceAddress) {
                dev.setDeviceAddress(deviceAddress);
            }
        } catch (Exception e) {
            LOG.warning("[ModbusTreeBuilder] applySettings deviceAddress=" + deviceAddress
                    + " failed: " + e.getMessage());
        }
    }
}
