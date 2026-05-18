package com.sitecVendor.am8xControl.wb;

import com.tridium.ndriver.ui.device.BNDeviceManager;
import javax.baja.workbench.mgr.MgrController;
import javax.baja.workbench.mgr.MgrLearn;
import javax.baja.workbench.mgr.MgrModel;
import javax.baja.workbench.mgr.MgrState;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.*;

/**
 * Manager Workbench per {@code BAm8xNetwork}.
 *
 * <p>Registra come agente sul tipo {@code Am8xNetwork} e fornisce al framework
 * ndriver le factory per Model, Learn, Controller e State — le quattro
 * componenti della Discovery View nativa.</p>
 */
@NiagaraType(agent = @AgentOn(types = "am8xControl:Am8xNetwork"))
public class BAm8xDeviceManager extends BNDeviceManager {

    @Override
    protected MgrModel makeModel() {
        return new Am8xDeviceModel(this);
    }

    @Override
    protected MgrLearn makeLearn() {
        return new Am8xDeviceLearn(this);
    }

    @Override
    protected MgrController makeController() {
        return new Am8xDeviceController(this);
    }

    @Override
    protected MgrState makeState() {
        return super.makeState();
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDeviceManager.class);

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON = BIcon.make("module://am8xControl/img/am8xNetwork.png");
}
