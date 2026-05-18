package com.sitecVendor.am8xControl;

import com.tridium.ndriver.discover.BINDiscoveryHost;
import com.tridium.ndriver.discover.BNDiscoveryJob;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.*;

/**
 * Job di discovery per il modulo am8xControl.
 *
 * <p>Il framework {@code BNDiscoveryJob} gestisce interamente il ciclo di vita
 * del job e la comunicazione con la Discovery View del Workbench.
 * Il parsing XML avviene in {@link BAm8xNetwork#getDiscoveryObjects} che viene
 * chiamato da {@code doRun()} del job base.</p>
 */
@NiagaraType
@NoSlotomatic
public class BAm8xLearnDevicesJob extends BNDiscoveryJob {

    public BAm8xLearnDevicesJob() {
        super();
    }

    public BAm8xLearnDevicesJob(BINDiscoveryHost host) {
        super(host);
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xLearnDevicesJob.class);
}
