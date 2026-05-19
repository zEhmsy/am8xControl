package com.sitecVendor.am8xControl;

import com.tridium.ndriver.BNDevice;
import com.tridium.modbusCore.client.point.BModbusClientBooleanProxyExt;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BAddressFormatEnum;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BStringPoint;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;
import java.util.logging.Logger;

/**
 * Device Niagara che rappresenta un singolo sensore / modulo sulla centrale AM-8x00.
 * Creato dalla Discovery View quando l'utente aggiunge un entry al database.
 *
 * All'import, crea automaticamente 3 point figli con BModbusClientBooleanProxyExt:
 *   alarm       — BBooleanPoint, registro allarme
 *   fault       — BBooleanPoint, registro guasto
 *   statusLabel — BStringPoint,  etichetta stato (senza proxyExt, gestita manualmente)
 */
@NiagaraType
@NoSlotomatic
public class BAm8xDevice extends BNDevice {

    private static final Logger LOG = Logger.getLogger(BAm8xDevice.class.getName());

    public static final Property panelLabel =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getPanelLabel() { return getString(panelLabel); }
    public void setPanelLabel(String v) { setString(panelLabel, v, null); }

    public static final Property loopNumber =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getLoopNumber() { return getInt(loopNumber); }
    public void setLoopNumber(int v) { setInt(loopNumber, v, null); }

    public static final Property positionOnLoop =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getPositionOnLoop() { return getInt(positionOnLoop); }
    public void setPositionOnLoop(int v) { setInt(positionOnLoop, v, null); }

    public static final Property deviceType =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getDeviceType() { return getString(deviceType); }
    public void setDeviceType(String v) { setString(deviceType, v, null); }

    public static final Property label =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getLabel() { return getString(label); }
    public void setLabel(String v) { setString(label, v, null); }

    public static final Property zoneAddress =
            newProperty(Flags.SUMMARY | Flags.READONLY, 0, null);
    public int getZoneAddress() { return getInt(zoneAddress); }
    public void setZoneAddress(int v) { setInt(zoneAddress, v, null); }

    public static final Property zoneLabel =
            newProperty(Flags.SUMMARY | Flags.READONLY, "", null);
    public String getZoneLabel() { return getString(zoneLabel); }
    public void setZoneLabel(String v) { setString(zoneLabel, v, null); }

    // parentModulePos: position of parent M720 on loop; -1 = direct sensor.
    // Needed by module sub-address formula: loop*5000 + parentModulePos*10 + pos.
    public static final Property parentModulePos =
            newProperty(Flags.HIDDEN | Flags.READONLY, -1, null);
    public int getParentModulePos() { return getInt(parentModulePos); }
    public void setParentModulePos(int v) { setInt(parentModulePos, v, null); }

    ////////////////////////////////////////////////////////////////
    // Descriptor apply
    ////////////////////////////////////////////////////////////////

    public void applyDescriptor(Am8xDeviceDescriptor d) {
        if (d == null) return;
        setPanelLabel(d.getPanelLabel());
        setLoopNumber(d.getLoopNumber());
        setPositionOnLoop(d.getPositionOnLoop());
        setDeviceType(d.getDeviceType());
        setLabel(d.getLabel());
        setZoneAddress(d.getZoneAddress());
        setZoneLabel(d.getZoneLabel());
    }

    ////////////////////////////////////////////////////////////////
    // Point auto-creation
    ////////////////////////////////////////////////////////////////

    private void ensurePoints() {
        if (getSlot("alarm") != null) return; // already created

        int loop = getLoopNumber();
        int pos  = getPositionOnLoop();
        int pmp  = getParentModulePos();

        int alarmAddr, faultAddr;
        if (pmp >= 0) {
            // sub-module inside M720 (MON3, IN3, etc.)
            alarmAddr = Am8xModbusAddressing.moduleAlarm(loop, pmp, pos);
            faultAddr = Am8xModbusAddressing.moduleFault(loop, pmp, pos);
        } else if (isModuleDeviceType(getDeviceType())) {
            // direct module on loop (M701, M720 leaf) — deviceType starts with 'M'
            // n_modulo = positionOnLoop, channel = 0 for single-output modules
            alarmAddr = Am8xModbusAddressing.moduleAlarm(loop, pos, 0);
            faultAddr = Am8xModbusAddressing.moduleFault(loop, pos, 0);
        } else {
            // direct loop sensor (NFX...)
            alarmAddr = Am8xModbusAddressing.sensorAlarm(loop, pos);
            faultAddr = Am8xModbusAddressing.sensorFault(loop, pos);
        }

        addBooleanPoint("alarm", alarmAddr);
        addBooleanPoint("fault", faultAddr);
        addStringPoint("statusLabel");

        LOG.info("[am8x] ensurePoints: alarm=" + alarmAddr + " fault=" + faultAddr
                + " on " + getLabel() + " [" + getDeviceType() + "]");
    }

    private static boolean isModuleDeviceType(String type) {
        return type != null && type.startsWith("M");
    }

    private void addBooleanPoint(String name, int addr) {
        try {
            BBooleanPoint pt = new BBooleanPoint();
            BModbusClientBooleanProxyExt proxyExt = new BModbusClientBooleanProxyExt();
            pt.setProxyExt(proxyExt);
            add(name, pt);
            configureAddress(proxyExt, addr);
        } catch (Exception e) {
            LOG.warning("[am8x] addBooleanPoint '" + name + "' failed: " + e.getMessage());
        }
    }

    private void addStringPoint(String name) {
        try {
            BStringPoint pt = new BStringPoint();
            add(name, pt);
        } catch (Exception e) {
            LOG.warning("[am8x] addStringPoint '" + name + "' failed: " + e.getMessage());
        }
    }

    private static void configureAddress(BModbusClientBooleanProxyExt mcpe, int addr) {
        try {
            Object fa = mcpe.get("dataAddress");
            if (!(fa instanceof BFlexAddress)) return;
            BFlexAddress flexAddr = (BFlexAddress) fa;
            flexAddr.setAddressFormat(BAddressFormatEnum.make(1)); // 1 = decimal
            flexAddr.setAddress(String.valueOf(addr));
        } catch (Exception e) {
            LOG.warning("[am8x] configureAddress failed: " + e.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////
    // BNDevice abstract
    ////////////////////////////////////////////////////////////////

    @Override
    public Type getNetworkType() { return BAm8xNetwork.TYPE; }

    @Override
    public void started() throws Exception {
        super.started();
        ensurePoints();
    }

    @Override
    public void doPing() throws Exception {
        // stub — no live TCP connection in this version
    }

    ////////////////////////////////////////////////////////////////
    // Boilerplate
    ////////////////////////////////////////////////////////////////

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDevice.class);

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON = BIcon.make("module://am8xControl/img/am8xDevice.png");
}
