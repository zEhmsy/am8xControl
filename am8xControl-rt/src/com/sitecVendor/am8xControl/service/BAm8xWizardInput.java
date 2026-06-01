package com.sitecVendor.am8xControl.service;

import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.*;
import javax.baja.naming.BOrd;

@NiagaraType
public class BAm8xWizardInput extends BStruct {

    public static final Property xmlFilePath = newProperty(Flags.SUMMARY, BOrd.DEFAULT, null);
    public BOrd getXmlFilePath() { return (BOrd) get(xmlFilePath); }
    public void setXmlFilePath(BOrd v) { set(xmlFilePath, v, null); }

    public static final Property modbusNetworkSlot = newProperty(Flags.SUMMARY, "ModbusTcpNetwork", null);
    public String getModbusNetworkSlot() { return getString(modbusNetworkSlot); }
    public void setModbusNetworkSlot(String v) { setString(modbusNetworkSlot, v, null); }

    public static final Property defaultIpAddress = newProperty(Flags.SUMMARY, "192.168.1.10", null);
    public String getDefaultIpAddress() { return getString(defaultIpAddress); }
    public void setDefaultIpAddress(String v) { setString(defaultIpAddress, v, null); }

    public static final Property defaultPort = newProperty(Flags.SUMMARY, 502, null);
    public int getDefaultPort() { return getInt(defaultPort); }
    public void setDefaultPort(int v) { setInt(defaultPort, v, null); }

    public static final Property deviceAddressStart = newProperty(Flags.SUMMARY, 101, null);
    public int getDeviceAddressStart() { return getInt(deviceAddressStart); }
    public void setDeviceAddressStart(int v) { setInt(deviceAddressStart, v, null); }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xWizardInput.class);
}
