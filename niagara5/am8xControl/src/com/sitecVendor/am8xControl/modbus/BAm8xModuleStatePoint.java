package com.sitecVendor.am8xControl.modbus;

import niagara.nre.annotations.NiagaraType;
import niagara.nre.annotations.NoSlotomatic;
import niagara.sys.Sys;
import niagara.sys.Type;

/**
 * Punto stato AM-8x riservato ai moduli/sottomoduli LxxMxxx.
 *
 * Usa le azioni modulo ereditate da BAm8xStatePoint, visibili solo su LxxMxxx.
 */
@NiagaraType
@NoSlotomatic
public class BAm8xModuleStatePoint extends BAm8xStatePoint {

    @Override
    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BAm8xModuleStatePoint.class);
}
