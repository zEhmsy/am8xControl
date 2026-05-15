package com.sitecVendor.am8xControl.wb;

import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.*;

/**
 * Manager Workbench placeholder per BAm8xNetwork.
 *
 * <p>Fase 1: il Discovery e' eseguito tramite l'action {@code discover}
 * della Property Sheet del BAm8xNetwork. Questo singleton riserva
 * l'aggancio workbench per le future viste personalizzate (Discovery
 * panel in stile Essernet, wizard, ecc.).</p>
 */
@NiagaraType(agent = @AgentOn(types = "am8xControl:Am8xNetwork"))
@NiagaraSingleton
public final class BAm8xNetworkManager extends BSingleton {

    public static final BAm8xNetworkManager INSTANCE = new BAm8xNetworkManager();

    public static final Type TYPE = Sys.loadType(BAm8xNetworkManager.class);

    @Override
    public Type getType() { return TYPE; }

    @Override
    public BIcon getIcon() { return ICON; }
    private static final BIcon ICON = BIcon.make("module://am8xControl/img/am8xNetwork.png");
}
