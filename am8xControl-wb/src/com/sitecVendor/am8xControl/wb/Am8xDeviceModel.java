package com.sitecVendor.am8xControl.wb;

import com.sitecVendor.am8xControl.BAm8xDevice;
import com.tridium.ndriver.ui.device.BNDeviceManager;
import com.tridium.ndriver.ui.device.NDeviceModel;
import javax.baja.workbench.mgr.MgrColumn;

public class Am8xDeviceModel extends NDeviceModel {

    public Am8xDeviceModel(BNDeviceManager manager) {
        super(manager);
    }

    @Override
    protected MgrColumn[] makeColumns() {
        return new MgrColumn[]{
            new MgrColumn.Name(),
            new MgrColumn("Type") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDevice
                        ? ((BAm8xDevice) item).getDeviceType() : "";
                }
            },
            new MgrColumn("Loop") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDevice
                        ? ((BAm8xDevice) item).getLoopNumber() : 0;
                }
            },
            new MgrColumn("Pos") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDevice
                        ? ((BAm8xDevice) item).getPositionOnLoop() : 0;
                }
            },
            new MgrColumn("Zone") {
                @Override public Object get(Object item) {
                    return item instanceof BAm8xDevice
                        ? ((BAm8xDevice) item).getZoneLabel() : "";
                }
            },
        };
    }
}
