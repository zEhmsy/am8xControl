package com.sitecVendor.am8xControl.wb;

import com.tridium.ndriver.ui.device.NDeviceController;
import javax.baja.driver.ui.device.BDeviceManager;

/**
 * Controller della Discovery View per am8xControl.
 * Usa il comportamento default di {@link NDeviceController} —
 * il bottone "Discover" chiama {@code doDiscover()} che submita il job.
 */
public class Am8xDeviceController extends NDeviceController {

    public Am8xDeviceController(BDeviceManager manager) {
        super(manager);
    }
}
