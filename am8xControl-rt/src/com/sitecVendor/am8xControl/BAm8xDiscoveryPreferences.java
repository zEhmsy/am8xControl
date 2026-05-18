package com.sitecVendor.am8xControl;

import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.NoSlotomatic;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

/**
 * Discovery preferences for AM-8200N. Tells the NDriver framework which leaf type
 * to expect so it can properly populate and serialize the discovery pane.
 */
@NiagaraType
@NoSlotomatic
public final class BAm8xDiscoveryPreferences extends BNDiscoveryPreferences {

    @Override
    public Type getDiscoveryLeafType() {
        return BAm8xDiscoveryEntry.TYPE;
    }

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xDiscoveryPreferences.class);
}
